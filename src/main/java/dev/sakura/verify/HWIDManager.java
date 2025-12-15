package dev.sakura.verify;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//@AutoNative
public class HWIDManager {
    //@NativeVirtualization(VirtualMachine.SHARK_BLACK)
    private static String getHWID() {
        return System.getenv("PROCESSOR_IDENTIFIER") +
                System.getenv("COMPUTERNAME") +
                System.getenv("PROCESSOR_ARCHITECTURE") +
                System.getenv("TEMP") +
                System.getenv("COMPUTERNAME") +
                System.getProperty("user.name");
    }

    private static byte[] encryptBySHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //@NativeVirtualization(VirtualMachine.TIGER_BLACK)
    public static String getEncryptedHWID() {
        byte[] hash = encryptBySHA256(getHWID());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
