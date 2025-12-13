package dev.sakura.verify;

public class VerificationManager {
    private static VerificationManager INSTANCE;

    private boolean verified = false;
    private String username = "";
    private String failReason = "";
    private byte[] receivedJarBytes = null;

    public static VerificationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VerificationManager();
        }
        return INSTANCE;
    }

    public boolean verify(String username, String password) {
        VerificationConnection connection = new VerificationConnection();

        if (!connection.connect()) {
            failReason = "无法连接到服务器！";
            return false;
        }

        try {
            String response = connection.login(username, password);

            if (response.startsWith("[PASS]")) {
                this.verified = true;
                this.username = username;
                return true;
            } else if (response.startsWith("[FAIL]")) {
                failReason = response.substring(6);
                return false;
            } else if (response.startsWith("[ERROR]")) {
                failReason = response.substring(7);
                return false;
            } else {
                failReason = "Unknown response: " + response;
                return false;
            }
        } finally {
            connection.close();
        }
    }

    public RegisterResult register(String username, String password) {
        VerificationConnection connection = new VerificationConnection();

        if (!connection.connect()) {
            return new RegisterResult(false, "无法连接到服务器！");
        }

        try {
            String response = connection.register(username, password);

            if (response.startsWith("[REGISTERED]")) {
                return new RegisterResult(true, "注册请求已提交！请去QQ群艾特管理员。");
            } else if (response.startsWith("[FAIL]")) {
                return new RegisterResult(false, response.substring(6));
            } else if (response.startsWith("[ERROR]")) {
                return new RegisterResult(false, response.substring(7));
            } else {
                return new RegisterResult(false, "未知响应: " + response);
            }
        } finally {
            connection.close();
        }
    }

    public boolean isVerified() {
        return verified;
    }

    public String getUsername() {
        return username;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setReceivedJarBytes(byte[] bytes) {
        this.receivedJarBytes = bytes;
    }
    
    /**
     * 检查是否有有效的 JAR 数据
     * 这是验证成功的唯一可靠标志
     */
    public boolean hasValidJarData() {
        // 必须有数据
        if (receivedJarBytes == null || receivedJarBytes.length < 100) {
            return false;
        }
        
        // 检查 JAR/ZIP 魔数 (PK\x03\x04)
        if (receivedJarBytes[0] != 0x50 || receivedJarBytes[1] != 0x4B ||
            receivedJarBytes[2] != 0x03 || receivedJarBytes[3] != 0x04) {
            return false;
        }
        
        return true;
    }

    public void loadClasses() {
        if (receivedJarBytes == null || receivedJarBytes.length == 0) {
            System.err.println("[NekoLoader] 神秘问题并不想让你进入客户端");
            return;
        }

        NekoLoader.getInstance().loadFromJar(receivedJarBytes);
    }

    public record RegisterResult(boolean success, String message) {
    }
}
