package dev.sakura.verify;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 * 安全守卫：这个类经过我跟Claude的精密打造最终成功产出
 */
public class SecurityGuard {
    private static volatile boolean securityCheckPassed = false;
    private static volatile long lastCheckTime = 0;
    private static final long CHECK_INTERVAL = 30000; // 30秒检查一次
    
    // 混淆的魔数，用于完整性校验
    private static final int[] MAGIC = {0x53, 0x41, 0x4B, 0x55, 0x52, 0x41};
    
    /**
     * 执行全面安全检查
     */
    public static boolean performSecurityCheck() {
        try {
            // 检测调试器
            if (isDebuggerAttached()) {
                triggerSecurityViolation("DBG");
                return false;
            }

            // 检测可疑的 Java Agent
            if (hasSuspiciousAgent()) {
                triggerSecurityViolation("AGT");
                return false;
            }
            
            // 检测类加载器篡改
            if (isClassLoaderCompromised()) {
                triggerSecurityViolation("CLD");
                return false;
            }
            
            // 检测时间篡改
            if (isTimeManipulated()) {
                triggerSecurityViolation("TIM");
                return false;
            }
            
            securityCheckPassed = true;
            lastCheckTime = System.nanoTime();
            return true;
        } catch (Throwable t) {
            // 任何异常都视为安全威胁
            triggerSecurityViolation("EXC");
            return false;
        }
    }
    
    /**
     * 快速安全检查（定期执行）
     */
    public static boolean quickCheck() {
        long now = System.nanoTime();
        if (now - lastCheckTime > CHECK_INTERVAL * 1_000_000L) {
            return performSecurityCheck();
        }
        return securityCheckPassed && !isDebuggerAttached();
    }
    
    /**
     * 检测调试器
     */
    private static boolean isDebuggerAttached() {
        // 方法1：检查 JVM 参数
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtimeMXBean.getInputArguments();
        for (String arg : args) {
            String lower = arg.toLowerCase();
            if (lower.contains("-agentlib:jdwp") || 
                lower.contains("-xdebug") ||
                lower.contains("-xrunjdwp") ||
                lower.contains("suspend=")) {
                return true;
            }
        }
        
        // 方法2：检查调试端口
        try {
            String jdwpAgent = System.getProperty("java.compiler");
            if (jdwpAgent != null && jdwpAgent.contains("jdwp")) {
                return true;
            }
        } catch (Throwable ignored) {}
        
        // 方法3：检查线程名（调试时通常有特殊线程）
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            String name = t.getName().toLowerCase();
            if (name.contains("jdwp") || name.contains("debugger")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检测可疑的 Java Agent
     */
    private static boolean hasSuspiciousAgent() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> args = runtimeMXBean.getInputArguments();
        
        // ByteBuddy Agent 是我们自己用的，所以要排除
        int suspiciousCount = 0;
        for (String arg : args) {
            String lower = arg.toLowerCase();
            // 检测可疑的 agent
            if (lower.contains("-javaagent:") && !lower.contains("byte-buddy")) {
                // 可能是恶意 agent
                suspiciousCount++;
            }
        }
        
        // 如果有超过1个非我们的 agent，可能有问题
        return suspiciousCount > 1;
    }
    
    /**
     * 检测类加载器是否被篡改
     */
    private static boolean isClassLoaderCompromised() {
        try {
            // 检查我们的类是否被正确的类加载器加载
            ClassLoader cl = SecurityGuard.class.getClassLoader();
            if (cl == null) {
                return true; // Bootstrap loader不应该加载我们的类
            }
            
            String clName = cl.getClass().getName();
            if (clName.contains("Hook") || clName.contains("Inject") || clName.contains("Transform")) {
                return true;
            }

            return false;
        } catch (Throwable t) {
            return true;
        }
    }
    
    /**
     * 检测时间是否被篡改（用于检测调试时的时间冻结）
     */
    private static long lastNanoTime = 0;
    private static long lastMilliTime = 0;
    
    private static boolean isTimeManipulated() {
        long nanoNow = System.nanoTime();
        long milliNow = System.currentTimeMillis();
        
        if (lastNanoTime != 0) {
            long nanoDiff = nanoNow - lastNanoTime;
            long milliDiff = milliNow - lastMilliTime;
            
            // 如果nano时间和milli时间差异太大，可能被篡改
            if (nanoDiff > 0 && milliDiff > 0) {
                double ratio = (double) nanoDiff / (milliDiff * 1_000_000);
                if (ratio < 0.1 || ratio > 10) {
                    return true;
                }
            }
        }
        
        lastNanoTime = nanoNow;
        lastMilliTime = milliNow;
        return false;
    }
    
    /**
     * 计算挑战响应（用于服务器验证）
     */
    public static String computeChallengeResponse(byte[] challenge, String hwid, byte[] sharedSecret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(challenge);
            md.update(hwid.getBytes());
            md.update(sharedSecret);
            // 添加环境指纹
            md.update(getEnvironmentFingerprint().getBytes());
            byte[] hash = md.digest();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取环境指纹（用于绑定到特定环境）
     */
    public static String getEnvironmentFingerprint() {
        StringBuilder sb = new StringBuilder();
        sb.append(System.getProperty("os.name", ""));
        sb.append(System.getProperty("os.arch", ""));
        sb.append(System.getProperty("java.version", ""));
        sb.append(Runtime.getRuntime().availableProcessors());
        return sb.toString();
    }
    
    /**
     * 触发安全违规（混淆处理，不直接退出）
     */
    private static void triggerSecurityViolation(String code) {
        securityCheckPassed = false;
        // 不直接退出，而是让后续的解密失败
        System.setProperty("sakura.sv." + code, String.valueOf(System.nanoTime()));
    }
    
    /**
     * 获取安全状态值（用于密钥派生）
     * 如果安全检查未通过，返回的值会导致解密失败
     */
    public static byte[] getSecuritySalt() {
        if (!securityCheckPassed) {
            // 返回错误的盐值，导致解密失败
            return new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int m : MAGIC) {
                md.update((byte) m);
            }
            md.update(getEnvironmentFingerprint().getBytes());
            return md.digest();
        } catch (Exception e) {
            return new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        }
    }
}
