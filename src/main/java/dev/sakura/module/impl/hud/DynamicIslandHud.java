package dev.sakura.module.impl.hud;

import dev.sakura.Sakura;
import dev.sakura.manager.Managers;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.module.impl.client.HudEditor;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.utils.animations.Easing;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static dev.sakura.module.impl.hud.DynamicIslandHud.Size.INVENTORY_BG_COLOR;

public class DynamicIslandHud extends HudModule {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    static final class Size {
        static final float BASE_W = 65, BASE_H = 19;
        static final float EXPANDED_W = 90, EXPANDED_H = 25;
        static final float RADIUS = 6;
        static final float ELEMENT_SPACING = 20;
        static final float ELEMENT_WIDTH = 50;
        static final float LOGO_FONT_SIZE = 12;
        static final float INFO_FONT_SIZE = 10;
        static final float GLOW_RADIUS = 3.0f;
        static final Color INVENTORY_BG_COLOR = new Color(18, 18, 18, 70);
    }

    private static final class Timing {
        static final long EXPAND = 300L;
        static final long DISPLAY = 1500L;
        static final long COLLAPSE_1 = 300L;
        static final long COLLAPSE_2 = 400L;
        static final long TOTAL = EXPAND + DISPLAY + COLLAPSE_1 + COLLAPSE_2;

    }

    private enum Phase {IDLE, EXPANDING, DISPLAY, COLLAPSE_1, COLLAPSE_2}

    private final BoolValue blur = new BoolValue("Blur", true);
    private final NumberValue<Double> blurStrength = new NumberValue<>("BlurStrength", 10.0, 1.0, 20.0, 0.5, blur::get);

    private static ToggleInfo currentToggle;
    private static ToggleInfo pendingToggle;
    private long toggleStartTime = -1L;
    private float targetExpandedWidth = Size.EXPANDED_W;

    private Phase phase = Phase.IDLE;
    private float progress;
    private float blurOpacity = 1f;
    private float animX, animY, animW, animH;

    public DynamicIslandHud() {
        super("DynamicIsland", 0, 3);
    }

    public static void onModuleToggle(Module module, boolean enabled) {
        pendingToggle = new ToggleInfo(module.getName(), enabled);
    }

    @Override
    public void renderInGame(DrawContext context) {
        if (isHudEditorOpen()) return;
        this.currentContext = context;
        update();
        renderBlur(context);
        renderSideBlurs(context, phase == Phase.IDLE ? 0f :
                       phase == Phase.EXPANDING ? progress :
                       phase == Phase.DISPLAY ? 1f :
                       phase == Phase.COLLAPSE_1 ? 1f :
                       1f - progress);
        NanoVGRenderer.INSTANCE.draw(vg -> renderContent());
    }

    @Override
    public void renderInEditor(DrawContext context, float mouseX, float mouseY) {
        handleDrag(mouseX, mouseY);
        this.currentContext = context;
        update();
        renderBlur(context);
        renderSideBlurs(context, phase == Phase.IDLE ? 0f :
                       phase == Phase.EXPANDING ? progress :
                       phase == Phase.DISPLAY ? 1f :
                       phase == Phase.COLLAPSE_1 ? 1f :
                       1f - progress);
        NanoVGRenderer.INSTANCE.draw(vg -> {
            renderContent();
            NanoVGHelper.drawRect(x, y, width, height,
                    dragging ? withAlpha(ClickGui.color(0), 80) : withAlpha(ClickGui.backgroundColor.get(), 50));
        });
    }

    @Override
    public void onRenderContent() {
    }

    private boolean isHudEditorOpen() {
        HudEditor editor = Managers.MODULE.getModule(HudEditor.class);
        return editor != null && editor.isEnabled();
    }

    private void handleDrag(float mouseX, float mouseY) {
        if (!dragging) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        x = clamp(mouseX - dragX, 0, sw - width);
        y = clamp(mouseY - dragY, 0, sh - height);
        relativeX = x / sw;
        relativeY = y / sh;
    }

    private void update() {
        processToggle();
        calculateState();
    }

    private void processToggle() {
        if (pendingToggle != null) {
            currentToggle = pendingToggle;
            pendingToggle = null;
            toggleStartTime = System.currentTimeMillis();
            targetExpandedWidth = calculateExpandedWidth();
        } else if (currentToggle != null && elapsed() >= Timing.TOTAL) {
            currentToggle = null;
            toggleStartTime = -1L;
        }
    }

    private void calculateState() {
        int screenWidth = mc.getWindow().getScaledWidth();
        long dt = elapsed();
        if (currentToggle == null && toggleStartTime == -1L) {
            setPhase(Phase.IDLE, 0f, Size.BASE_W, Size.BASE_H, 1f);
        } else if (dt < Timing.EXPAND) {
            float p = easeOut(dt / (float) Timing.EXPAND);
            setPhase(Phase.EXPANDING, p,
                    lerp(Size.BASE_W, targetExpandedWidth, p),
                    lerp(Size.BASE_H, Size.EXPANDED_H, p),
                    lerp(1f, 1f, p));
        } else if (dt < Timing.EXPAND + Timing.DISPLAY) {
            float p = (dt - Timing.EXPAND) / (float) Timing.DISPLAY;
            setPhase(Phase.DISPLAY, p, targetExpandedWidth, Size.EXPANDED_H, 1f);
        } else if (dt < Timing.EXPAND + Timing.DISPLAY + Timing.COLLAPSE_1) {
            float p = easeOut((dt - Timing.EXPAND - Timing.DISPLAY) / (float) Timing.COLLAPSE_1);
            setPhase(Phase.COLLAPSE_1, p, targetExpandedWidth, Size.EXPANDED_H, lerp(1f, 0f, p));
        } else {
            float p = easeOut((dt - Timing.EXPAND - Timing.DISPLAY - Timing.COLLAPSE_1) / (float) Timing.COLLAPSE_2);
            setPhase(Phase.COLLAPSE_2, p,
                    lerp(targetExpandedWidth, Size.BASE_W, p),
                    lerp(Size.EXPANDED_H, Size.BASE_H, p),
                    lerp(0f, 0f, p));
        }

        animX = (screenWidth - animW) / 2f;
        animY = y;
        this.width = animW;
        this.height = animH;
        this.x = animX;
    }

    private void setPhase(Phase p, float prog, float w, float h, float blur) {
        this.phase = p;
        this.progress = prog;
        this.animW = w;
        this.animH = h;
        this.blurOpacity = interpolateBlurOpacity(blur);
    }
    private float interpolateBlurOpacity(float targetBlur) {
        float delta = targetBlur - this.blurOpacity;
        float interpolationFactor = 0.15f;
        return this.blurOpacity + delta * interpolationFactor;
    }

    private void renderBlur(DrawContext context) {
        if (!blur.get()) return;
        float clampedBlurOpacity = Math.max(0f, Math.min(1f, blurOpacity));
        Shader2DUtils.drawRoundedBlur(
                context.getMatrices(), animX, animY, animW, animH, Size.RADIUS,
                new Color(0, 0, 0, 0), blurStrength.get().floatValue(), clampedBlurOpacity
        );
    }

    private void renderSideBlurs(DrawContext context, float expandProgress) {
        if (!blur.get()) return;

        float clampedBlurOpacity = Math.max(0f, Math.min(1f, blurOpacity));
        float timeBgX = animX - Size.ELEMENT_SPACING - Size.ELEMENT_WIDTH;
        Shader2DUtils.drawRoundedBlur(
                context.getMatrices(), timeBgX, animY, Size.ELEMENT_WIDTH, animH, Size.RADIUS,
                new Color(0, 0, 0, 0), blurStrength.get().floatValue(), clampedBlurOpacity
        );
        float nameBgX = animX + animW + Size.ELEMENT_SPACING;
        Shader2DUtils.drawRoundedBlur(
                context.getMatrices(), nameBgX, animY, Size.ELEMENT_WIDTH, animH, Size.RADIUS,
                new Color(0, 0, 0, 0), blurStrength.get().floatValue(), clampedBlurOpacity
        );
    }

    private void renderContent() {
        switch (phase) {
            case IDLE -> renderIdle();
            case EXPANDING -> renderExpanding();
            case DISPLAY -> renderDisplay();
            case COLLAPSE_1 -> renderCollapse1();
            case COLLAPSE_2 -> renderCollapse2();
        }
    }

    private void renderIdle() {
        drawBackground(INVENTORY_BG_COLOR);
        drawSideInfo(0f);
        drawCenteredTitle();
    }

    private void renderExpanding() {
        drawBackground(INVENTORY_BG_COLOR);
        drawSideInfo(progress);
        if (currentToggle != null) {
            drawToggleInfo(alphaFromProgress(progress), progress);
        }
    }

    private void renderDisplay() {
        drawBackground(INVENTORY_BG_COLOR);
        drawSideInfo(1f);
        if (currentToggle != null) {
            drawToggleInfo(255, progress);
        }
    }

    private void renderCollapse1() {
        drawBackground(INVENTORY_BG_COLOR);
        drawColorBar(1f - progress);
        drawSideInfo(1f);
        if (currentToggle != null) {
            drawToggleInfo(alphaFromProgress(1f - progress), 1f - progress);
        }
    }

    private void renderCollapse2() {
        drawBackground(INVENTORY_BG_COLOR);
        drawSideInfo(0f);
    }

    private void drawBackground(Color color) {
        NanoVGHelper.drawRoundRectBloom(animX, animY, animW, animH, Size.RADIUS, color);
    }

    private void drawColorBar(float progress) {
        float padding = 8, barH = 1.5f;
        float barY = animY + animH - barH - 3;
        float maxW = animW - padding * 2;
        float currentW = maxW * progress;
        NanoVGHelper.drawRoundRect(animX + padding, barY, maxW, barH, barH / 2,
                withAlpha(ClickGui.color(0), 50));
        if (currentW > 0) {
            NanoVGHelper.drawRoundRect(animX + padding, barY, currentW, barH, barH / 2,
                    withAlpha(ClickGui.color(0), 220));
        }
    }

    private void drawCenteredTitle() {
        int font = FontLoader.bold((int)Size.LOGO_FONT_SIZE);
        String name = Sakura.MOD_NAME;
        float textW = NanoVGHelper.getTextWidth(name, font, Size.LOGO_FONT_SIZE);
        NanoVGHelper.drawGlowingString(name, animX + (animW - textW) / 2f, animY + animH / 2f + 4, font, Size.LOGO_FONT_SIZE, ClickGui.color(0), Size.GLOW_RADIUS);
    }

    private void drawSideInfo(float expandProgress) {
        int font = FontLoader.medium(9);
        Color color = new Color(255, 255, 255,255);
        float centerY = animY + animH / 2f + 3;
        Color bgColor = INVENTORY_BG_COLOR;
        String time = LocalTime.now().format(TIME_FORMAT);
        float timeW = NanoVGHelper.getTextWidth(time, font, Size.INFO_FONT_SIZE);
        float timeBgX = animX - Size.ELEMENT_SPACING - Size.ELEMENT_WIDTH;
        NanoVGHelper.drawRoundRectBloom(timeBgX, animY, Size.ELEMENT_WIDTH, animH, Size.RADIUS, bgColor);
        NanoVGHelper.drawString(time, timeBgX + (Size.ELEMENT_WIDTH - timeW) / 2, centerY, font, Size.INFO_FONT_SIZE, color);
        String username ="FPS:"+ mc.getCurrentFps();
        float nameW = NanoVGHelper.getTextWidth(username, font, Size.INFO_FONT_SIZE);
        float nameBgX = animX + animW + Size.ELEMENT_SPACING;
        NanoVGHelper.drawRoundRectBloom(nameBgX, animY, Size.ELEMENT_WIDTH, animH, Size.RADIUS, bgColor);
        NanoVGHelper.drawString(username, nameBgX + (Size.ELEMENT_WIDTH - nameW) / 2, centerY, font, Size.INFO_FONT_SIZE, color);
    }

    private void drawToggleInfo(int alpha, float timeProgress) {
        if (currentToggle == null) return;
        float padding = 6, iconSize = 16;
        float centerY = animY + (animH - 3) / 2f;
        int iconFont = FontLoader.icons(iconSize);
        String icon = currentToggle.enabled ? "U" : "T";
        Color iconColor = currentToggle.enabled ? ClickGui.color(0) : ClickGui.color2(0);
        float iconW = NanoVGHelper.getTextWidth(icon, iconFont, iconSize);
        NanoVGHelper.drawString(icon, animX + padding + 6, centerY + iconSize * 0.35f, iconFont, iconSize, iconColor);
        int textFont = FontLoader.medium((int)Size.LOGO_FONT_SIZE);
        String status = currentToggle.name + (currentToggle.enabled ? " 已开启" : " 已关闭");
        NanoVGHelper.drawString(status, animX + padding + iconW + 14, centerY + Size.LOGO_FONT_SIZE * 0.35f, textFont, Size.LOGO_FONT_SIZE - 2f, Color.WHITE);

        drawProgressBar(alpha, timeProgress);
    }
    private void drawProgressBar(int alpha, float timeProgress) {
        float padding = 8, barH = 1.5f;
        float barY = animY + animH - barH - 3;
        float maxW = animW - padding * 2;
        float currentW = maxW * (1f - timeProgress);
        NanoVGHelper.drawRoundRect(animX + padding, barY, maxW, barH, barH / 2,
                withAlpha(ClickGui.color(0), 50));
        if (currentW > 0) {
            NanoVGHelper.drawRoundRect(animX + padding, barY, currentW, barH, barH / 2,
                    withAlpha(ClickGui.color(0), 220));
        }
    }

    private float calculateExpandedWidth() {
        if (currentToggle == null) return Size.EXPANDED_W;

        float padding = 6, iconSize = 16, textSize = Size.LOGO_FONT_SIZE;
        int iconFont = FontLoader.icons(iconSize);
        int textFont = FontLoader.medium(textSize);

        String icon = currentToggle.enabled ? "U" : "T";
        String status = currentToggle.name + (currentToggle.enabled ? " 已开启" : " 已关闭");

        float iconW = NanoVGHelper.getTextWidth(icon, iconFont, iconSize);
        float textW = NanoVGHelper.getTextWidth(status, textFont, textSize);
        float needed = padding * 2 + iconW + 4 + textW;

        return Math.max(Size.EXPANDED_W, Math.max(needed, 41));
    }

    private long elapsed() {
        return toggleStartTime == -1L ? 0 : System.currentTimeMillis() - toggleStartTime;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float easeOut(float t) {
        return (float) Easing.CUBIC_OUT.ease(t);
    }

    private static int alphaFromProgress(float p) {
        return (int) (255 * p);
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private record ToggleInfo(String name, boolean enabled) {
    }
}