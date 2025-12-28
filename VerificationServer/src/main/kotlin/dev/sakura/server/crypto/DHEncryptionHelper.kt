package dev.sakura.server.crypto

import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DHEncryptionHelper {
    private val encryptionAlgorithm = "AES"

    private val publicKey: PublicKey
    private val keyAgreement: KeyAgreement = KeyAgreement.getInstance("ECDH")
    private var sharedKey: ByteArray? = null

    init {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        val kp = kpg.generateKeyPair()
        publicKey = kp.public
        keyAgreement.init(kp.private)
    }

    fun setReceiverPublicKey(publicKey: PublicKey) {
        keyAgreement.doPhase(publicKey, true)
        sharedKey = keyAgreement.generateSecret()
    }

    fun encrypt(bytes: ByteArray): ByteArray {
        val key = generateKey()
        val cipher = Cipher.getInstance(encryptionAlgorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(bytes)
    }

    fun decrypt(encryptedData: ByteArray): ByteArray {
        val key = generateKey()
        val cipher = Cipher.getInstance(encryptionAlgorithm)
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(encryptedData)
    }

    fun encryptWithSession(data: ByteArray, sessionToken: ByteArray, timestamp: Long): ByteArray {
        val sessionKey = deriveSessionKey(sessionToken, timestamp)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sessionKey, "AES"), spec)

        val encrypted = cipher.doFinal(data)
        val hmac = computeHmac(sessionKey, iv + encrypted)

        return iv + encrypted + hmac
    }

    private fun deriveSessionKey(sessionToken: ByteArray, timestamp: Long): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sharedKey ?: throw IllegalStateException("Handshake not complete"))
        md.update(sessionToken)
        md.update(timestamp.toString().toByteArray())
        return md.digest().copyOf(16) // AES-128
    }

    private fun computeHmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun generateSessionToken(): ByteArray {
        return ByteArray(32).also { SecureRandom().nextBytes(it) }
    }

    fun generateChallenge(): ByteArray {
        return ByteArray(64).also { SecureRandom().nextBytes(it) }
    }

    fun verifyChallengeResponse(challenge: ByteArray, hwid: String, envFingerprint: String, response: String): Boolean {
        val expected = computeChallengeResponse(challenge, hwid, envFingerprint)
        return MessageDigest.isEqual(expected.toByteArray(), response.toByteArray())
    }

    fun computeChallengeResponse(challenge: ByteArray, hwid: String, envFingerprint: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(challenge)
        md.update(hwid.toByteArray())
        md.update(sharedKey ?: throw IllegalStateException("Handshake not complete"))
        md.update(envFingerprint.toByteArray())
        val hash = md.digest()
        return Base64.getEncoder().encodeToString(hash)
    }

    fun encryptWithChallenge(
        data: ByteArray,
        sessionToken: ByteArray,
        timestamp: Long,
        challengeResponse: String
    ): ByteArray {
        // SHA256(sessionKey + challengeResponse)
        val sessionKey = deriveSessionKey(sessionToken, timestamp)
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sessionKey)
        md.update(challengeResponse.toByteArray())
        val finalKey = md.digest().copyOf(16)

        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(finalKey, "AES"), spec)

        val encrypted = cipher.doFinal(data)
        val hmac = computeHmac(finalKey, iv + encrypted)

        return iv + encrypted + hmac
    }

    fun getPublicKey(): PublicKey = publicKey

    private fun generateKey(): Key {
        return SecretKeySpec(sharedKey, encryptionAlgorithm)
    }

    fun isHandshakeComplete(): Boolean = sharedKey != null
}
