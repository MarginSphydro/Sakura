package dev.sakura.module.impl.hud;

import dev.sakura.Sakura;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.utils.animations.Animation;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.DecelerateAnimation;
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
    private final Animation expandAnim = new DecelerateAnimation(300, 1, Direction.BACKWARDS);
    private long toggleStartTime;

    private static final float BASE_WIDTH = 130;
    private static final float BASE_HEIGHT = 38;
    private static final float EXPANDED_WIDTH = 180;
    private static final float EXPANDED_HEIGHT = 50;
    private static final float RADIUS = 20;
    private static final long DISPLAY_DURATION = 1000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DynamicIslandHud() {
        super("DynamicIsland", 0, 6);
    }

    public static void onModuleToggle(Module module, boolean enabled) {
        pendingToggle = new ToggleInfo(module.getName(), enabled);
    }

    @Override
    public void onRenderContent() {
        processToggle();
        float progress = expandAnim.getOutput().floatValue();

        int sw = mc.getWindow().getScaledWidth();
        float s = getScale();

        float w = lerp(BASE_WIDTH, EXPANDED_WIDTH, progress) * s;
        float h = lerp(BASE_HEIGHT, EXPANDED_HEIGHT, progress) * s;
        float ix = (sw - w) / 2f;
        float iy = y;

        this.width = w;
        this.height = h;
        this.x = ix;

        if (blur.get()) {
            withRawCoords(() -> Shader2DUtils.drawRoundedBlur(
                    getMatrix(), ix, iy, w, h, RADIUS * s,
                    new Color(0, 0, 0, 0), blurStrength.get().floatValue(), 1.0f
            ));
        }

        NanoVGHelper.drawRoundRect(ix, iy, w, h, RADIUS * s, new Color(0, 0, 0, 90));

        float sideOffset = (w - BASE_WIDTH * s) / 2f + 14 * s;
        String time = LocalTime.now().format(TIME_FMT);
        String player = mc.getSession().getUsername();
        int sideFont = FontLoader.greycliffMedium(16 * s);
        float sideFontSize = 16 * s;
        Color sideColor = new Color(200, 200, 200, (int) (255 * (1 - progress * 0.3f)));

        float timeW = NanoVGHelper.getTextWidth(time, sideFont, sideFontSize);
        NanoVGHelper.drawString(time, ix - sideOffset - timeW, iy + h / 2f + 6 * s, sideFont, sideFontSize, sideColor);
        NanoVGHelper.drawString(player, ix + w + sideOffset, iy + h / 2f + 6 * s, sideFont, sideFontSize, sideColor);

        if (progress < 0.05f) {
            renderCollapsed(ix, iy, w, h, s);
        } else {
            renderExpanded(ix, iy, w, h, s, progress);
        }
    }

    private void renderCollapsed(float ix, float iy, float w, float h, float s) {
        int font = FontLoader.greycliffBold(22 * s);
        float fontSize = 22 * s;
        float tw = NanoVGHelper.getTextWidth(Sakura.MOD_NAME, font, fontSize);
        NanoVGHelper.drawString(Sakura.MOD_NAME, ix + (w - tw) / 2f, iy + h / 2f + 8 * s, font, fontSize, Color.WHITE);
    }

    private void renderExpanded(float ix, float iy, float w, float h, float s, float progress) {
        if (currentToggle == null) return;

        float padding = 12 * s;
        float iconSize = 24 * s;
        float contentY = iy + (h - 6 * s) / 2f;

        int iconFont = FontLoader.icon(iconSize);
        String icon = currentToggle.enabled ? "\uf00c" : "\uf00d";
        Color iconColor = currentToggle.enabled ? ClickGui.color(0) : new Color(255, 80, 80);
        float iconW = NanoVGHelper.getTextWidth(icon, iconFont, iconSize);
        NanoVGHelper.drawString(icon, ix + padding, contentY + iconSize * 0.35f, iconFont, iconSize,
                new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), (int) (255 * progress)));

        int textFont = FontLoader.greycliffMedium(14 * s);
        float textSize = 14 * s;
        String status = currentToggle.name + (currentToggle.enabled ? " 已开启" : " 已关闭");
        float textX = ix + padding + iconW + 8 * s;
        NanoVGHelper.drawString(status, textX, contentY + textSize * 0.35f, textFont, textSize,
                new Color(255, 255, 255, (int) (255 * progress)));

        float barPadding = 16 * s;
        float barHeight = 3 * s;
        float barY = iy + h - barHeight - 6 * s;
        float barMaxWidth = w - barPadding * 2;

        long elapsed = System.currentTimeMillis() - toggleStartTime;
        float timeProgress = Math.min(1.0f, elapsed / (float) DISPLAY_DURATION);

        float barWidth = barMaxWidth * (1.0f - timeProgress);

        NanoVGHelper.drawRoundRect(ix + barPadding, barY, barMaxWidth, barHeight, barHeight / 2, new Color(255, 255, 255, (int) (50 * progress)));
        if (barWidth > 0) {
            NanoVGHelper.drawRoundRect(ix + barPadding, barY, barWidth, barHeight, barHeight / 2, new Color(255, 255, 255, (int) (220 * progress)));
        }
    }

    private void processToggle() {
        if (pendingToggle != null) {
            currentToggle = pendingToggle;
            pendingToggle = null;
            toggleStartTime = System.currentTimeMillis();
            expandAnim.setDirection(Direction.FORWARDS);
            return;
        }

        if (currentToggle != null && System.currentTimeMillis() - toggleStartTime > DISPLAY_DURATION) {
            expandAnim.setDirection(Direction.BACKWARDS);
            if (expandAnim.finished(Direction.BACKWARDS)) {
                currentToggle = null;
            }
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record ToggleInfo(String name, boolean enabled) {
    }
}