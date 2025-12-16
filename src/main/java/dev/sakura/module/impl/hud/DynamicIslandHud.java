package dev.sakura.module.impl.hud;

import dev.sakura.Sakura;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.utils.animations.Easing;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DynamicIslandHud extends HudModule {
    private final BoolValue blur = new BoolValue("Blur", true);
    private final NumberValue<Double> blurStrength = new NumberValue<>("BlurStrength", 10.0, 1.0, 20.0, 0.5, blur::get);

    private static ToggleInfo currentToggle;
    private static ToggleInfo pendingToggle;
    private long toggleStartTime = -1L;

    private static final float BASE_WIDTH = 130;
    private static final float BASE_HEIGHT = 38;
    private static final float MIN_EXPANDED_WIDTH = 180;
    private static final float EXPANDED_HEIGHT = 50;
    private static final float RADIUS = 20;
    private static final long EXPAND_DURATION = 200L;
    private static final long DISPLAY_DURATION = 1000L;
    private static final long COLLAPSE_STAGE1_DURATION = 200L;
    private static final long COLLAPSE_STAGE2_DURATION = 300L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private float targetExpandedWidth = MIN_EXPANDED_WIDTH;

    public DynamicIslandHud() {
        super("DynamicIsland", 0, 6);
    }

    public static void onModuleToggle(Module module, boolean enabled) {
        pendingToggle = new ToggleInfo(module.getName(), enabled);
    }

    @Override
    public void onRenderContent() {
        processToggle();

        if (currentToggle == null && toggleStartTime == -1L) {
            renderIdle();
            return;
        }

        long delta = System.currentTimeMillis() - toggleStartTime;
        long collapseStart = EXPAND_DURATION + DISPLAY_DURATION;
        long totalDuration = collapseStart + COLLAPSE_STAGE1_DURATION + COLLAPSE_STAGE2_DURATION;

        if (delta < EXPAND_DURATION) {
            float progress = (float) Easing.CUBIC_OUT.ease(delta / (float) EXPAND_DURATION);
            renderExpanding(progress);
        } else if (delta < collapseStart) {
            float timeProgress = (delta - EXPAND_DURATION) / (float) DISPLAY_DURATION;
            renderDisplay(timeProgress);
        } else if (delta < collapseStart + COLLAPSE_STAGE1_DURATION) {
            float progress = (float) Easing.CUBIC_OUT.ease((delta - collapseStart) / (float) COLLAPSE_STAGE1_DURATION);
            renderCollapseStage1(progress);
        } else if (delta < totalDuration) {
            float progress = (float) Easing.CUBIC_OUT.ease((delta - collapseStart - COLLAPSE_STAGE1_DURATION) / (float) COLLAPSE_STAGE2_DURATION);
            renderCollapseStage2(progress);
        } else {
            renderIdle();
        }
    }

    private void renderIdle() {
        int sw = mc.getWindow().getScaledWidth();

        float w = BASE_WIDTH;
        float h = BASE_HEIGHT;
        float ix = (sw - w) / 2f;
        float iy = y;

        this.width = w;
        this.height = h;
        this.x = ix;

        if (blur.get()) {
            withPixelCoords(ix, iy, w, h, (px, py, pw, ph) ->
                    Shader2DUtils.drawRoundedBlur(getMatrix(), px, py, pw, ph, RADIUS * getScaleFactor(),
                            new Color(0, 0, 0, 0), blurStrength.get().floatValue(), 1.0f));
        }

        NanoVGHelper.drawRoundRect(ix, iy, w, h, RADIUS, new Color(0, 0, 0, 90));

        renderSideInfo(ix, iy, w, h, 0f);

        int font = FontLoader.greycliffBold(22);
        float fontSize = 22;
        float tw = NanoVGHelper.getTextWidth(Sakura.MOD_NAME, font, fontSize);
        NanoVGHelper.drawString(Sakura.MOD_NAME, ix + (w - tw) / 2f, iy + h / 2f + 8, font, fontSize, Color.WHITE);
    }

    private void renderExpanding(float progress) {
        int sw = mc.getWindow().getScaledWidth();

        float w = lerp(BASE_WIDTH, targetExpandedWidth, progress);
        float h = lerp(BASE_HEIGHT, EXPANDED_HEIGHT, progress);
        float ix = (sw - w) / 2f;
        float iy = y;

        this.width = w;
        this.height = h;
        this.x = ix;

        if (blur.get()) {
            withPixelCoords(ix, iy, w, h, (px, py, pw, ph) ->
                    Shader2DUtils.drawRoundedBlur(getMatrix(), px, py, pw, ph, RADIUS * getScaleFactor(),
                            new Color(0, 0, 0, 0), blurStrength.get().floatValue(), progress));
        }

        NanoVGHelper.drawRoundRect(ix, iy, w, h, RADIUS, new Color(0, 0, 0, 90));

        renderSideInfo(ix, iy, w, h, progress);

        if (currentToggle != null) {
            int textAlpha = (int) (255 * progress);
            renderContent(ix, iy, w, h, textAlpha, 0f);
        }
    }

    private void renderDisplay(float timeProgress) {
        int sw = mc.getWindow().getScaledWidth();

        float w = targetExpandedWidth;
        float h = EXPANDED_HEIGHT;
        float ix = (sw - w) / 2f;
        float iy = y;

        this.width = w;
        this.height = h;
        this.x = ix;

        if (blur.get()) {
            withPixelCoords(ix, iy, w, h, (px, py, pw, ph) ->
                    Shader2DUtils.drawRoundedBlur(getMatrix(), px, py, pw, ph, RADIUS * getScaleFactor(),
                            new Color(0, 0, 0, 0), blurStrength.get().floatValue(), 1.0f));
        }

        NanoVGHelper.drawRoundRect(ix, iy, w, h, RADIUS, new Color(0, 0, 0, 90));

        renderSideInfo(ix, iy, w, h, 1f);

        if (currentToggle != null) {
            renderContent(ix, iy, w, h, 255, timeProgress);
        }
    }

    private void renderCollapseStage1(float progress) {
        int sw = mc.getWindow().getScaledWidth();

        float w = targetExpandedWidth;
        float h = EXPANDED_HEIGHT;
        float ix = (sw - w) / 2f;
        float iy = y;

        this.width = w;
        this.height = h;
        this.x = ix;

        if (blur.get()) {
            withPixelCoords(ix, iy, w, h, (px, py, pw, ph) ->
                    Shader2DUtils.drawRoundedBlur(getMatrix(), px, py, pw, ph, RADIUS * getScaleFactor(),
                            new Color(0, 0, 0, 0), blurStrength.get().floatValue(), 1f - progress));
        }

        NanoVGHelper.drawRoundRect(ix, iy, w, h, RADIUS, new Color(0, 0, 0, 90));

        Color themeColor = ClickGui.color(0);
        float barWidth = w * progress;
        if (barWidth > 0) {
            NanoVGHelper.drawRoundRect(ix, iy, barWidth, h, RADIUS, themeColor);
        }

        renderSideInfo(ix, iy, w, h, 1f);

        if (currentToggle != null) {
            int textAlpha = (int) (255 * (1f - progress));
            renderContent(ix, iy, w, h, textAlpha, 1f);
        }
    }

    private void renderCollapseStage2(float progress) {
        int sw = mc.getWindow().getScaledWidth();

        float w = lerp(targetExpandedWidth, BASE_WIDTH, progress);
        float h = lerp(EXPANDED_HEIGHT, BASE_HEIGHT, progress);
        float ix = (sw - w) / 2f;
        float iy = y;

        this.width = w;
        this.height = h;
        this.x = ix;

        Color themeColor = ClickGui.color(0);
        NanoVGHelper.drawRoundRect(ix, iy, w, h, RADIUS, themeColor);

        renderSideInfo(ix, iy, w, h, 1f - progress);
    }

    private void renderSideInfo(float ix, float iy, float w, float h, float expandProgress) {
        float sideOffset = (w - BASE_WIDTH) / 2f + 14;
        String time = LocalTime.now().format(TIME_FMT);
        String player = mc.getSession().getUsername();
        int sideFont = FontLoader.greycliffMedium(16);
        float sideFontSize = 16;
        Color sideColor = new Color(200, 200, 200, (int) (255 * (1 - expandProgress * 0.3f)));

        float timeW = NanoVGHelper.getTextWidth(time, sideFont, sideFontSize);
        NanoVGHelper.drawString(time, ix - sideOffset - timeW, iy + h / 2f + 6, sideFont, sideFontSize, sideColor);
        NanoVGHelper.drawString(player, ix + w + sideOffset, iy + h / 2f + 6, sideFont, sideFontSize, sideColor);
    }

    private void renderContent(float ix, float iy, float w, float h, int alpha, float timeProgress) {
        if (currentToggle == null) return;

        float padding = 12;
        float iconSize = 24;
        float contentY = iy + (h - 6) / 2f;

        int iconFont = FontLoader.icon(iconSize);
        String icon = currentToggle.enabled ? "\uf00c" : "\uf00d";
        Color iconColor = currentToggle.enabled ? ClickGui.color(0) : new Color(255, 80, 80);
        float iconW = NanoVGHelper.getTextWidth(icon, iconFont, iconSize);
        NanoVGHelper.drawString(icon, ix + padding, contentY + iconSize * 0.35f, iconFont, iconSize,
                new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), alpha));

        int textFont = FontLoader.greycliffMedium(14);
        float textSize = 14;
        String status = currentToggle.name + (currentToggle.enabled ? " 已开启" : " 已关闭");
        float textX = ix + padding + iconW + 8;
        NanoVGHelper.drawString(status, textX, contentY + textSize * 0.35f, textFont, textSize,
                new Color(255, 255, 255, alpha));

        float barPadding = 16;
        float barHeight = 3;
        float barY = iy + h - barHeight - 6;
        float barMaxWidth = w - barPadding * 2;
        float barWidth = barMaxWidth * (1.0f - timeProgress);

        NanoVGHelper.drawRoundRect(ix + barPadding, barY, barMaxWidth, barHeight, barHeight / 2, new Color(255, 255, 255, (int) (50 * alpha / 255f)));
        if (barWidth > 0) {
            NanoVGHelper.drawRoundRect(ix + barPadding, barY, barWidth, barHeight, barHeight / 2, new Color(255, 255, 255, (int) (220 * alpha / 255f)));
        }
    }

    private void processToggle() {
        if (pendingToggle != null) {
            currentToggle = pendingToggle;
            pendingToggle = null;
            toggleStartTime = System.currentTimeMillis();
            calculateExpandedWidth();
            return;
        }

        if (currentToggle != null && toggleStartTime != -1L) {
            long delta = System.currentTimeMillis() - toggleStartTime;
            long totalDuration = EXPAND_DURATION + DISPLAY_DURATION + COLLAPSE_STAGE1_DURATION + COLLAPSE_STAGE2_DURATION;

            if (delta >= totalDuration) {
                currentToggle = null;
                toggleStartTime = -1L;
            }
        }
    }

    private void calculateExpandedWidth() {
        if (currentToggle == null) {
            targetExpandedWidth = MIN_EXPANDED_WIDTH;
            return;
        }

        float padding = 12;
        float iconSize = 24;
        float textSize = 14;
        float barPadding = 16;

        int iconFont = FontLoader.icon(iconSize);
        String icon = currentToggle.enabled ? "\uf00c" : "\uf00d";
        float iconW = NanoVGHelper.getTextWidth(icon, iconFont, iconSize);

        int textFont = FontLoader.greycliffMedium(textSize);
        String status = currentToggle.name + (currentToggle.enabled ? " 已开启" : " 已关闭");
        float textW = NanoVGHelper.getTextWidth(status, textFont, textSize);

        float neededWidth = padding + iconW + 8 + textW + padding;
        neededWidth = Math.max(neededWidth, barPadding * 2 + 50);

        targetExpandedWidth = Math.max(MIN_EXPANDED_WIDTH, neededWidth);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record ToggleInfo(String name, boolean enabled) {
    }
}
