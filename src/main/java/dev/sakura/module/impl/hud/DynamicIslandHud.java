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
import java.util.LinkedList;
import java.util.Queue;

public class DynamicIslandHud extends HudModule {
    private final BoolValue blur = new BoolValue("Blur", true);
    private final NumberValue<Double> blurStrength = new NumberValue<>("BlurStrength", 10.0, 1.0, 20.0, 0.5, blur::get);

    private static final Queue<ToggleInfo> toggleQueue = new LinkedList<>();
    private ToggleInfo currentToggle;
    private final Animation expandAnim = new DecelerateAnimation(300, 1, Direction.BACKWARDS);
    private long toggleStartTime;

    private static final float BASE_WIDTH = 130;
    private static final float BASE_HEIGHT = 38;
    private static final float EXPANDED_WIDTH = 200;
    private static final float EXPANDED_HEIGHT = 80;
    private static final float RADIUS = 20;
    private static final long DISPLAY_DURATION = 1800;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DynamicIslandHud() {
        super("DynamicIsland", 0, 6);
    }

    public static void onModuleToggle(Module module, boolean enabled) {
        toggleQueue.offer(new ToggleInfo(module.getName(), enabled));
    }

    @Override
    public void onRenderContent() {
        processQueue();
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

        int iconFont = FontLoader.icon(36 * s);
        String icon = currentToggle.enabled ? "\uf00c" : "\uf00d";
        Color iconColor = currentToggle.enabled ? ClickGui.color(0) : new Color(255, 80, 80);
        float iconW = NanoVGHelper.getTextWidth(icon, iconFont, 36 * s);
        NanoVGHelper.drawString(icon, ix + (w - iconW) / 2f, iy + h * 0.45f, iconFont, 36 * s,
                new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), (int) (255 * progress)));

        int textFont = FontLoader.greycliffMedium(14 * s);
        float textSize = 14 * s;
        String status = currentToggle.name + (currentToggle.enabled ? " 已开启" : " 已关闭");
        float textW = NanoVGHelper.getTextWidth(status, textFont, textSize);
        NanoVGHelper.drawString(status, ix + (w - textW) / 2f, iy + h * 0.85f, textFont, textSize,
                new Color(200, 200, 200, (int) (255 * progress)));
    }

    private void processQueue() {
        if (currentToggle != null && System.currentTimeMillis() - toggleStartTime > DISPLAY_DURATION) {
            expandAnim.setDirection(Direction.BACKWARDS);
            if (expandAnim.finished(Direction.BACKWARDS)) {
                currentToggle = null;
            }
        }

        if (currentToggle == null && !toggleQueue.isEmpty()) {
            currentToggle = toggleQueue.poll();
            toggleStartTime = System.currentTimeMillis();
            expandAnim.setDirection(Direction.FORWARDS);
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private record ToggleInfo(String name, boolean enabled) {}
}