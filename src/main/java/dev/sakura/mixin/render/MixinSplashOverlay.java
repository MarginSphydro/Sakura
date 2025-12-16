package dev.sakura.mixin.render;

import dev.sakura.shaders.SplashShader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {
    @Shadow
    @Final
    private ResourceReload reload;

    @Shadow
    private float progress;

    @Shadow
    private long reloadCompleteTime;

    @Shadow
    private long reloadStartTime;

    @Shadow
    @Final
    private boolean reloading;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private Consumer<Optional<Throwable>> exceptionHandler;

    @Unique
    private boolean sakura$shaderInitialized = false;

    @Unique
    private float sakura$displayProgress = 0f;

    @Unique
    private long sakura$startTime = -1L;

    @Unique
    private static final float PROGRESS_SMOOTH_SPEED = 0.3f; // 进度平滑速度

    @Unique
    private static final long MIN_DISPLAY_TIME = 5700L; // 最小显示时间

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        long currentTime = Util.getMeasuringTimeMs();

        if (!sakura$shaderInitialized) {
            SplashShader.getInstance().init();
            sakura$shaderInitialized = true;
            sakura$startTime = currentTime;
        }

        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = currentTime;
        }

        float fadeOutProgress = this.reloadCompleteTime > -1L ? (float) (currentTime - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float fadeInProgress = this.reloadStartTime > -1L ? (float) (currentTime - this.reloadStartTime) / 500.0F : -1.0F;

        float loadProgress = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + loadProgress * 0.05F, 0.0F, 1.0F);

        long elapsedTime = currentTime - sakura$startTime;
        float timeBasedMaxProgress = MathHelper.clamp((float) elapsedTime / MIN_DISPLAY_TIME, 0f, 1f);

        timeBasedMaxProgress = 1f - (1f - timeBasedMaxProgress) * (1f - timeBasedMaxProgress);

        float targetProgress = Math.min(loadProgress, timeBasedMaxProgress);

        sakura$displayProgress += (targetProgress - sakura$displayProgress) * PROGRESS_SMOOTH_SPEED * delta;
        sakura$displayProgress = MathHelper.clamp(sakura$displayProgress, 0f, 1f);

        if (loadProgress >= 1.0f && elapsedTime >= MIN_DISPLAY_TIME) {
            sakura$displayProgress += (1f - sakura$displayProgress) * 0.1f;
        }

        float zoom = 1.0f;
        float fadeOut = 0f;

        if (SplashShader.getInstance().isTransitionStarted()) {
            float t = SplashShader.getInstance().getTransitionProgress();
            zoom = 1.0f + t * t * t * 20.0f;

            if (t > 0.6f) {
                fadeOut = (t - 0.6f) / 0.4f;
            }
        }

        if (fadeOut < 0.99f) {
            SplashShader.getInstance().render(width, height, sakura$displayProgress, fadeOut, zoom);
        }

        if (SplashShader.getInstance().isTransitionStarted() && this.client.currentScreen != null) {
            this.client.currentScreen.render(context, 0, 0, delta);
        }

        if (fadeOutProgress >= 2.0F || SplashShader.getInstance().isTransitionComplete()) {
            this.client.setOverlay(null);
            SplashShader.getInstance().cleanup();
            sakura$shaderInitialized = false;
        }

        long elapsedTotal = currentTime - sakura$startTime;
        boolean minTimeReached = elapsedTotal >= MIN_DISPLAY_TIME;
        if (this.reloadCompleteTime == -1L && this.reload.isComplete() && sakura$displayProgress >= 0.95f && minTimeReached && (!this.reloading || fadeInProgress >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }

            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            SplashShader.getInstance().startTransition();

            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, width, height);
            }
        }

        ci.cancel();
    }
}
