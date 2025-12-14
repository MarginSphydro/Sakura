package dev.sakura.verify;

import dev.undefinedteam.obfuscator.annotations.AutoNative;
import dev.undefinedteam.obfuscator.annotations.NativeVirtualization;
import dev.undefinedteam.obfuscator.annotations.VirtualMachine;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户组管理 - 根据用户等级限制功能
 * 等级: 1=普通, 2=高级, 3=VIP, 4=管理员
 */

@AutoNative
public class UserGroup {
    private static int currentGroup = 1;
    private static int groupMirror = 1;           // 镜像值
    private static int groupChecksum = 0x5A4B3C;  // 校验值 (group ^ MAGIC)
    private static final int MAGIC = 0x5A4B3D;    // 魔数

    // 服务端签名
    private static String serverSignature = "";
    private static final String HMAC_KEY = "SakuraVerify2024!@#";  // 这个是与服务端共享的密钥

    // 服务端发送的限制类列表
    private static final Set<String> RESTRICTED_CLASSES = new HashSet<>();

    @NativeVirtualization(VirtualMachine.SHARK_BLACK)
    public static void setGroup(int group, String signature) {
        if (!verifySignature(group, signature)) {
            triggerSecurityViolation();
            return;
        }
        currentGroup = group;
        groupMirror = group;
        groupChecksum = group ^ MAGIC;
        serverSignature = signature;
    }

    /**
     * 获取用户组
     */
    @NativeVirtualization(VirtualMachine.TIGER_BLACK)
    public static int getGroup() {
        if (!checkIntegrity()) {
            triggerSecurityViolation();
            return 1; // 返回最低权限
        }
        return currentGroup;
    }

    /**
     * 验证服务端签名
     */
    @NativeVirtualization(VirtualMachine.SHARK_BLACK)
    private static boolean verifySignature(int group, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            String data = "group:" + group;
            String expected = computeHMAC(data);
            return signature.equals(expected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算HMAC签名
     */
    private static String computeHMAC(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }

    @NativeVirtualization(VirtualMachine.TIGER_RED)
    private static boolean checkIntegrity() {
        if (currentGroup != groupMirror) {
            return false;
        }
        if ((currentGroup ^ MAGIC) != groupChecksum) {
            return false;
        }
        return true;
    }

    /**
     * 触发安全违规
     */
    private static void triggerSecurityViolation() {
        // 重置为最低权限
        currentGroup = 1;
        groupMirror = 1;
        groupChecksum = 1 ^ MAGIC;
        RESTRICTED_CLASSES.clear();
        // 设置安全标记
        System.setProperty("sakura.security.violation", String.valueOf(System.nanoTime()));
    }

    public static String getGroupName(int group) {
        return switch (group) {
            case 1 -> "普通用户";
            case 2 -> "高级用户";
            case 3 -> "VIP";
            case 4 -> "管理员";
            default -> "你他妈是破解的吧";
        };
    }

    public static String getCurrentGroupName() {
        return getGroupName(currentGroup);
    }

    @NativeVirtualization(VirtualMachine.SHARK_BLACK)
    public static void setRestrictedClasses(String classNames, String signature, int group) {
        if (!verifyRestrictionsSignature(group, classNames, signature)) {
            triggerSecurityViolation();
            return;
        }
        RESTRICTED_CLASSES.clear();
        if (classNames == null || classNames.isEmpty()) {
            return;
        }
        for (String className : classNames.split(",")) {
            String trimmed = className.trim();
            if (!trimmed.isEmpty()) {
                RESTRICTED_CLASSES.add(trimmed);
            }
        }
    }

    /**
     * 验证限制类列表签名
     */
    private static boolean verifyRestrictionsSignature(int group, String restrictions, String signature) {
        try {
            String data = "group:" + group + "|restrictions:" + restrictions;
            String expected = computeHMAC(data);
            return signature.equals(expected);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查当前用户组是否可以加载指定类
     *
     * @param className 类名
     * @return true=可以加载, false=权限不足
     */
    @NativeVirtualization(VirtualMachine.SHARK_BLACK)
    public static boolean canLoadClass(String className) {
        return !RESTRICTED_CLASSES.contains(className);
    }

    /**
     * 获取当前用户组无法使用的所有类
     */
    public static Set<String> getRestrictedClasses() {
        return Collections.unmodifiableSet(RESTRICTED_CLASSES);
    }

    /**
     * 检查用户是否为为大手子
     */
    public static boolean isAdvanced() {
        return currentGroup >= 2;
    }

    public static boolean isVIP() {
        return currentGroup >= 3;
    }

    public static boolean isAdmin() {
        return currentGroup >= 4;
    }
}
