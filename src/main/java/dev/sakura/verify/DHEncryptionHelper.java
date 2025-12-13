package dev.sakura.verify;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

public class DHEncryptionHelper {
    private static final String ENCRYPTION_ALGORITHM = "AES";

    private final PublicKey publicKey;
    private final KeyAgreement keyAgreement;
    private byte[] sharedKey;

    public DHEncryptionHelper() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            KeyPair kp = kpg.generateKeyPair();
            this.publicKey = kp.getPublic();
            this.keyAgreement = KeyAgreement.getInstance("ECDH");
            this.keyAgreement.init(kp.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DHEncryptionHelper", e);
        }
    }

    public void setReceiverPublicKey(PublicKey publicKey) {
        try {
            keyAgreement.doPhase(publicKey, true);
            sharedKey = keyAgreement.generateSecret();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set receiver public key", e);
        }
    }

    public byte[] encrypt(byte[] bytes) {
        try {
            Key key = generateKey();
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return cipher.doFinal(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {
        try {
            Key key = generateKey();
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public boolean isHandshakeComplete() {
        return sharedKey != null;
    }

    private Key generateKey() {
        return new SecretKeySpec(sharedKey, ENCRYPTION_ALGORITHM);
    }

    /**
     * 使用会话令牌解密数据 (AES-GCM)
     * 输入格式: IV(12) + 加密数据 + HMAC(32)
     */
    public byte[] decryptWithSession(byte[] encryptedData, byte[] sessionToken, long timestamp) {
        try {
            if (encryptedData.length < 12 + 32) {
                throw new SecurityException("数据格式无效");
            }

            byte[] sessionKey = deriveSessionKey(sessionToken, timestamp);
            
            // 提取 IV, 加密数据, HMAC
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(encryptedData, 12, encryptedData.length - 32);
            byte[] receivedHmac = Arrays.copyOfRange(encryptedData, encryptedData.length - 32, encryptedData.length);
            
            // 验证 HMAC
            byte[] expectedHmac = computeHmac(sessionKey, Arrays.copyOfRange(encryptedData, 0, encryptedData.length - 32));
            if (!MessageDigest.isEqual(receivedHmac, expectedHmac)) {
                throw new SecurityException("HMAC 验证失败，数据可能被篡改");
            }
            
            // 解密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sessionKey, "AES"), spec);
            
            return cipher.doFinal(encrypted);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("会话解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成会话密钥: SHA256(sharedKey + sessionToken + timestamp)
     */
    private byte[] deriveSessionKey(byte[] sessionToken, long timestamp) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(sharedKey);
        md.update(sessionToken);
        md.update(String.valueOf(timestamp).getBytes());
        byte[] hash = md.digest();
        return Arrays.copyOf(hash, 16); // AES-128
    }

    /**
     * 计算 HMAC-SHA256
     */
    private byte[] computeHmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }
    
    /**
     * 计算挑战响应
     */
    public String computeChallengeResponse(byte[] challenge, String hwid, String envFingerprint) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(challenge);
            md.update(hwid.getBytes());
            md.update(sharedKey);
            md.update(envFingerprint.getBytes());
            byte[] hash = md.digest();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute challenge response", e);
        }
    }
    
    /**
     * 使用三重密钥解密（DH密钥 + 会话令牌 + 挑战响应）
     */
    public byte[] decryptWithChallenge(byte[] encryptedData, byte[] sessionToken, long timestamp, String challengeResponse) {
        try {
            if (encryptedData.length < 12 + 32) {
                throw new SecurityException("数据格式无效");
            }
            
            // 派生会话密钥
            byte[] sessionKey = deriveSessionKey(sessionToken, timestamp);
            
            // 派生最终密钥：SHA256(sessionKey + challengeResponse)
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sessionKey);
            md.update(challengeResponse.getBytes());
            byte[] finalKey = Arrays.copyOf(md.digest(), 16);
            
            // 提取 IV, 加密数据, HMAC
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(encryptedData, 12, encryptedData.length - 32);
            byte[] receivedHmac = Arrays.copyOfRange(encryptedData, encryptedData.length - 32, encryptedData.length);
            
            // 验证 HMAC
            byte[] expectedHmac = computeHmac(finalKey, Arrays.copyOfRange(encryptedData, 0, encryptedData.length - 32));
            if (!MessageDigest.isEqual(receivedHmac, expectedHmac)) {
                throw new SecurityException("HMAC 验证失败");
            }
            
            // 解密
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(finalKey, "AES"), spec);
            
            return cipher.doFinal(encrypted);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("三重解密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取共享密钥（用于安全检查）
     */
    public byte[] getSharedKey() {
        return sharedKey;
    }
}
