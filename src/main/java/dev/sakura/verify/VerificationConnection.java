package dev.sakura.verify;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

//@AutoNative
public class VerificationConnection {
    private static final String SERVER_HOST = "127.0.0.1"/*"116.204.133.141"*/;
    private static final int SERVER_PORT = 54188;

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private DHEncryptionHelper dhHelper;
    private boolean handshakeComplete = false;

    public VerificationConnection() {
        this.dhHelper = new DHEncryptionHelper();
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            performHandshake();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void performHandshake() throws Exception {
        sendRaw(dhHelper.getPublicKey().getEncoded());

        byte[] serverPublicKeyBytes = receiveRaw();
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(serverPublicKeyBytes);
        PublicKey serverPublicKey = KeyFactory.getInstance("EC").generatePublic(keySpec);
        dhHelper.setReceiverPublicKey(serverPublicKey);

        handshakeComplete = true;
    }

    // 存储挑战响应，用于后续解密
    private String challengeResponse = null;

    //@NativeVirtualization(VirtualMachine.SHARK_BLACK)
    public String login(String username, String password) {
        try {
            // 先执行安全检查
            if (!SecurityGuard.performSecurityCheck()) {
                return "[ERROR] Security check failed";
            }

            String hwid = HWIDManager.getEncryptedHWID();
            String message = "[LOGIN]" + username + "@" + password + "@" + hwid;
            sendMessage(message);

            String response = receiveMessage();

            // 处理挑战-响应流程
            if (response.startsWith("[CHALLENGE]")) {
                String challengeBase64 = response.substring(11);
                byte[] challenge = Base64.getDecoder().decode(challengeBase64);

                // 计算挑战响应
                String envFingerprint = SecurityGuard.getEnvironmentFingerprint();
                challengeResponse = dhHelper.computeChallengeResponse(challenge, hwid, envFingerprint);

                // 发送响应
                sendMessage("[RESPONSE]" + challengeResponse + "|" + envFingerprint);

                // 等待服务器验证
                response = receiveMessage();
            }

            // 处理登录结果
            if (response.startsWith("[PASS]")) {
                String tokenData = response.substring(6);
                String[] parts = tokenData.split("\\|");
                if (parts.length < 2) {
                    return "[ERROR] 你太牛逼了(TO)";
                }

                byte[] sessionToken = Base64.getDecoder().decode(parts[0]);
                long timestamp = Long.parseLong(parts[1]);

                // 解析用户组和签名
                if (parts.length < 6) {
                    return "[ERROR] 你太牛逼了(SG)";
                }
                int userGroup = Integer.parseInt(parts[2]);
                String groupSignature = parts[3];
                String restrictedClasses = parts[4];
                String restrictionsSignature = parts[5];

                UserGroup.setGroup(userGroup, groupSignature);
                UserGroup.setRestrictedClasses(restrictedClasses, restrictionsSignature, userGroup);

                // 验证时间戳（5分钟内有效，防止重放攻击）
                long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - timestamp) > 5 * 60 * 1000) {
                    return "[ERROR] 你太牛逼了(TI)";
                }

                // 再次安全检查
                if (!SecurityGuard.quickCheck()) {
                    return "[ERROR] 你太牛逼了(SC)";
                }

                System.out.println("[Verify] Challenge verified, receiving encrypted JAR...");
                byte[] encryptedJar = receiveJarBytes();

                if (encryptedJar != null && encryptedJar.length > 0) {
                    // 使用三重密钥解密 JAR（需要正确的挑战响应）
                    try {
                        if (challengeResponse == null) {
                            return "[ERROR] 你太牛逼了(CR)";
                        }
                        byte[] jarBytes = dhHelper.decryptWithChallenge(encryptedJar, sessionToken, timestamp, challengeResponse);
                        VerificationManager.getInstance().setReceivedJarBytes(jarBytes);
                    } catch (SecurityException e) {
                        return "[ERROR] 你太牛逼了(DE)";
                    }
                }

                return "[PASS]";
            }

            return response;
        } catch (Exception e) {
            return "[ERROR]" + e.getMessage();
        }
    }

    private byte[] receiveJarBytes() {
        try {
            byte[] encrypted = receiveRaw();
            return handshakeComplete ? dhHelper.decrypt(encrypted) : encrypted;
        } catch (Exception e) {
            return null;
        }
    }

    public String register(String username, String password) {
        try {
            String hwid = HWIDManager.getEncryptedHWID();
            String message = "[REGISTER]" + username + "@" + password + "@" + hwid;
            sendMessage(message);
            return receiveMessage();
        } catch (Exception e) {
            return "[ERROR]" + e.getMessage();
        }
    }

    private void sendMessage(String message) throws IOException {
        byte[] bytes = message.getBytes();
        byte[] encrypted = handshakeComplete ? dhHelper.encrypt(bytes) : bytes;
        sendRaw(encrypted);
    }

    private String receiveMessage() throws IOException {
        byte[] encrypted = receiveRaw();
        byte[] decrypted = handshakeComplete ? dhHelper.decrypt(encrypted) : encrypted;
        return new String(decrypted);
    }

    private void sendRaw(byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private byte[] receiveRaw() throws IOException {
        int size = input.readInt();
        byte[] bytes = new byte[size];
        input.readFully(bytes);
        return bytes;
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
