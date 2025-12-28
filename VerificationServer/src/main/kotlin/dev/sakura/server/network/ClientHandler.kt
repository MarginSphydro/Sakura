package dev.sakura.server.network

import dev.sakura.server.crypto.DHEncryptionHelper
import dev.sakura.server.data.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.*

class ClientHandler(private val socket: Socket) {
    private val input = DataInputStream(socket.getInputStream())
    private val output = DataOutputStream(socket.getOutputStream())
    private val dhHelper = DHEncryptionHelper()
    private var handshakeComplete = false

    private val clientAddress = socket.inetAddress.hostAddress

    suspend fun handle() = withContext(Dispatchers.IO) {
        runCatching {
            println("[Client] Connection from $clientAddress")

            performHandshake()

            while (!socket.isClosed) {
                val message = receiveMessage()
                if (message.isEmpty()) break

                println("[Client] $clientAddress: $message")
                processMessage(message)
                break
            }
        }.onFailure {
            println("[Client] Error handling $clientAddress: ${it.message}")
        }

        runCatching { socket.close() }
        println("[Client] Disconnected: $clientAddress")
    }

    private suspend fun performHandshake() = withContext(Dispatchers.IO) {
        val clientPublicKeyBytes = receiveRaw()
        val keySpec = X509EncodedKeySpec(clientPublicKeyBytes)
        val clientPublicKey = KeyFactory.getInstance("EC").generatePublic(keySpec)
        dhHelper.setReceiverPublicKey(clientPublicKey)

        sendRaw(dhHelper.getPublicKey().encoded)

        handshakeComplete = true
        println("[Client] Handshake complete with $clientAddress")
    }

    private suspend fun processMessage(message: String) {
        when {
            message.startsWith("[LOGIN]") -> handleLogin(message)
            message.startsWith("[REGISTER]") -> handleRegister(message)
            else -> sendMessage("[ERROR]Unknown command")
        }
    }

    private suspend fun handleLogin(message: String) {
        val data = message.removePrefix("[LOGIN]")
        val parts = data.split("@")

        if (parts.size != 3) {
            sendMessage("[ERROR]Invalid login format")
            return
        }

        val username = parts[0]
        val password = parts[1]
        val hwid = parts[2]

        val result = UserDatabase.verifyUser(username, password, hwid)

        when (result) {
            UserDatabase.VerifyResult.SUCCESS -> {
                println("[Login] SUCCESS: $username from $clientAddress")

                // 第一阶段：发送挑战
                val challenge = dhHelper.generateChallenge()
                val challengeBase64 = Base64.getEncoder().encodeToString(challenge)
                sendMessage("[CHALLENGE]$challengeBase64")

                // 接收客户端的挑战响应和环境指纹
                val responseMsg = receiveMessage()
                if (!responseMsg.startsWith("[RESPONSE]")) {
                    println("[Login] FAILED: Invalid challenge response format from $username")
                    sendMessage("[FAIL]Invalid response")
                    return
                }

                val responseParts = responseMsg.removePrefix("[RESPONSE]").split("|")
                if (responseParts.size != 2) {
                    println("[Login] FAILED: Malformed response from $username")
                    sendMessage("[FAIL]Malformed response")
                    return
                }

                val clientResponse = responseParts[0]
                val envFingerprint = responseParts[1]

                // 验证挑战响应
                if (!dhHelper.verifyChallengeResponse(challenge, hwid, envFingerprint, clientResponse)) {
                    println("[Login] FAILED: Challenge verification failed for $username")
                    sendMessage("[FAIL]Challenge failed")
                    return
                }

                println("[Login] Challenge verified for $username")

                // 第二阶段：发送会话令牌
                val sessionToken = dhHelper.generateSessionToken()
                val timestamp = System.currentTimeMillis()
                val tokenBase64 = Base64.getEncoder().encodeToString(sessionToken)
                val userGroup = UserDatabase.getUserGroup(username)
                val restrictedClasses = UserDatabase.getRestrictedClassesForGroup(userGroup)

                // 生成签名（防篡改）
                val groupSignature = UserDatabase.signGroup(userGroup)
                val restrictionsSignature = UserDatabase.signRestrictions(userGroup, restrictedClasses)

                // 发送响应: [PASS]<token>|<timestamp>|<group>|<groupSig>|<restricted_classes>|<restrictionsSig>
                sendMessage("[PASS]$tokenBase64|$timestamp|$userGroup|$groupSignature|$restrictedClasses|$restrictionsSignature")

                // 使用三重密钥加密 JAR（DH密钥 + 会话令牌 + 挑战响应）
                val clientJar = File("Client.jar")
                if (clientJar.exists()) {
                    val jarBytes = clientJar.readBytes()
                    val tripleEncrypted =
                        dhHelper.encryptWithChallenge(jarBytes, sessionToken, timestamp, clientResponse)
                    println("[Login] Sending ${tripleEncrypted.size} bytes (triple-encrypted) to $username")
                    sendBytes(tripleEncrypted)
                } else {
                    println("[Login] WARNING: Client.jar not found!")
                    sendBytes(ByteArray(0))
                }
            }

            UserDatabase.VerifyResult.USER_NOT_FOUND -> {
                println("[Login] FAILED: User not found - $username")
                sendMessage("[FAIL]User not found")
            }

            UserDatabase.VerifyResult.WRONG_PASSWORD -> {
                println("[Login] FAILED: Wrong password - $username")
                sendMessage("[FAIL]Wrong password")
            }

            UserDatabase.VerifyResult.HWID_MISMATCH -> {
                println("[Login] FAILED: HWID mismatch - $username (HWID: $hwid)")
                sendMessage("[FAIL]HWID mismatch")
            }
        }
    }

    private suspend fun sendBytes(bytes: ByteArray) = withContext(Dispatchers.IO) {
        val encrypted = if (handshakeComplete) dhHelper.encrypt(bytes) else bytes
        sendRaw(encrypted)
    }

    private suspend fun handleRegister(message: String) {
        val data = message.removePrefix("[REGISTER]")
        val parts = data.split("@")

        if (parts.size != 3) {
            sendMessage("[ERROR]Invalid register format")
            return
        }

        val username = parts[0]
        val password = parts[1]
        val hwid = parts[2]

        val result = UserDatabase.registerUser(username, password, hwid)

        when (result) {
            UserDatabase.RegisterResult.SUCCESS -> {
                println("[Register] SUCCESS: $username (HWID: $hwid)")
                sendMessage("[REGISTERED]")
            }

            UserDatabase.RegisterResult.USERNAME_EXISTS -> {
                println("[Register] FAILED: Username exists - $username")
                sendMessage("[FAIL]Username already exists")
            }

            UserDatabase.RegisterResult.PENDING_EXISTS -> {
                println("[Register] FAILED: Pending exists - $username")
                sendMessage("[FAIL]Registration pending approval")
            }
        }
    }

    private suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        val bytes = message.toByteArray()
        val encrypted = if (handshakeComplete) dhHelper.encrypt(bytes) else bytes
        sendRaw(encrypted)
    }

    private suspend fun receiveMessage(): String = withContext(Dispatchers.IO) {
        val encrypted = receiveRaw()
        val decrypted = if (handshakeComplete) dhHelper.decrypt(encrypted) else encrypted
        String(decrypted)
    }

    private suspend fun sendRaw(bytes: ByteArray) = withContext(Dispatchers.IO) {
        output.writeInt(bytes.size)
        output.write(bytes)
        output.flush()
    }

    private suspend fun receiveRaw(): ByteArray = withContext(Dispatchers.IO) {
        val size = input.readInt()
        val bytes = ByteArray(size)
        input.readFully(bytes)
        bytes
    }
}
