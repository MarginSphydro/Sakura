package dev.sakura.mixin.render;

import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.utils.animations.ChatAnimationManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.awt.*;

@Mixin(ChatHud.class)
public class MixinChatHud {
    private static final int CHAT_MARGIN_LEFT = 4;
    private static int minX = Integer.MAX_VALUE;
    private static int minY = Integer.MAX_VALUE;
    private static int maxX = Integer.MIN_VALUE;
    private static int maxY = Integer.MIN_VALUE;
    private static boolean shouldRender = false;
    private static DrawContext cachedContext = null;
    private static float targetX = 0;
    private static float targetY = 0;
    private static float currentX = 0;
    private static float currentY = 0;
    private static boolean initialized = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(CallbackInfo ci) {
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        shouldRender = false;
        if (!initialized) {
            currentX = 6F;
            currentY = 0;
            targetX = 6F;
            targetY = 0;
            initialized = true;
        }
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void redirectChatBackground(DrawContext context, int x1, int y1, int x2, int y2, int color)
    {
        minX = Math.min(minX, x1 + CHAT_MARGIN_LEFT);
        minY = Math.min(minY, y1);
        maxX = Math.max(maxX, x2 + CHAT_MARGIN_LEFT);
        maxY = Math.max(maxY, y2);
        shouldRender = true;
        cachedContext = context;
        targetX = minX - 4f + 6F;
        targetY = minY - 4f;
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderEnd(CallbackInfo ci) {
        if (!shouldRender || cachedContext == null) return;
        ChatAnimationManager animationManager = ChatAnimationManager.getInstance();
        currentX = (float) animationManager.getChatHudAnimation("x", targetX, 300);
        currentY = (float) animationManager.getChatHudAnimation("y", targetY, 300);
        float radius = 3f;
        float blurStrength = 8f;
        float blurOpacity = 0.85f;
        int width = maxX - minX;
        int height = maxY - minY;
        float padding = 4f;
        float finalWidth = width + padding * 2;
        float finalHeight = height + padding * 2;
        Shader2DUtils.drawRoundedBlur(cachedContext.getMatrices(), currentX, currentY, finalWidth, finalHeight, radius, new Color(0, 0, 0, 0), blurStrength, blurOpacity);
        NanoVGRenderer.INSTANCE.draw(vg -> {
            Color backgroundColor = new Color(20, 20, 20, 140);
            NanoVGHelper.drawRoundRect(currentX, currentY, finalWidth, finalHeight, radius, backgroundColor);
        });
    }
}