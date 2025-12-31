package dev.sakura.loader;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

//@AutoNative
public class Loader implements IMixinConfigPlugin {
    // 使用混淆的状态值代替简单的 boolean
    private static volatile int loadState = 0;
    private static final int STATE_INIT = 0x5A;
    private static final int STATE_VERIFIED = 0xA5;
    private static final int STATE_LOADED = 0x55;

    @Override
    public void onLoad(String mixinPackage) {
        if (loadState == 0) {
            loadState = STATE_INIT;
            System.out.println("[Sakura] Starting verification...");

            if (!SecurityGuard.performSecurityCheck()) {
                triggerFailure("SEC");
                return;
            }

            LoginWindow loginWindow = new LoginWindow();

            // 不使用简单的 boolean 返回值
            // LoginWindow.show() 内部会设置 VerificationManager 的状态
            loginWindow.show();

            // 检查是否收到了有效的JAR数据（这是唯一可靠的成功标志）
            // 如果破解者修改了show()的返回值，他们仍然没有 JAR 数据
            if (!VerificationManager.getInstance().hasValidJarData()) {
                triggerFailure("JAR");
                return;
            }

            if (!SecurityGuard.quickCheck()) {
                triggerFailure("CHK");
                return;
            }

            loadState = STATE_VERIFIED;

            // 加载类（需要正确的解密密钥）
            try {
                VerificationManager.getInstance().loadClasses();
                loadState = STATE_LOADED;
            } catch (Throwable t) {
                triggerFailure("LOAD");
            }
        }
    }

    //@NativeVirtualization(VirtualMachine.TIGER_BLACK)
    private void triggerFailure(String code) {
        System.err.println("[Sakura] Verification failed: " + code);
        new Thread(() -> {
            try {
                Thread.sleep(1000 + (long) (Math.random() * 2000));
            } catch (InterruptedException ignored) {
            }
            System.exit(1);
        }).start();

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
