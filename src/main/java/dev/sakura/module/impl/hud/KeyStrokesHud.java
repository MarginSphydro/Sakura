package dev.sakura.module.impl.hud;

import dev.sakura.module.HudModule;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.utils.animations.Animation;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.DecelerateAnimation;
import dev.sakura.utils.color.ColorUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import net.minecraft.client.option.KeyBinding;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class KeyStrokesHud extends HudModule {

    private final NumberValue<Double> offsetValue = new NumberValue<>("Offset", 3.0, 2.5, 10.0, 0.5);
    private final NumberValue<Double> sizeValue = new NumberValue<>("Size", 25.0, 15.0, 35.0, 1.0);
    private final NumberValue<Double> radiusValue = new NumberValue<>("Radius", 4.0, 0.0, 10.0, 1.0);
    private final NumberValue<Double> opacityValue = new NumberValue<>("Opacity", 0.5, 0.1, 1.0, 0.05);
    private final BoolValue whiteMode = new BoolValue("White", true);
    private final BoolValue blur = new BoolValue("Blur", false);
    private final NumberValue<Double> blurStrength = new NumberValue<>("Blur Strength", 8.0, 1.0, 20.0, 0.5, blur::get);

    private Button keyBindForward;
    private Button keyBindLeft;
    private Button keyBindBack;
    private Button keyBindRight;
    private Button keyBindJump;

    public KeyStrokesHud() {
        super("KeyStrokes", 10, 100);
    }

    @Override
    public void onRenderContent() {
        if (mc.player == null) return;

        if (keyBindForward == null) {
            keyBindForward = new Button(mc.options.forwardKey);
            keyBindLeft = new Button(mc.options.leftKey);
            keyBindBack = new Button(mc.options.backKey);
            keyBindRight = new Button(mc.options.rightKey);
            keyBindJump = new Button(mc.options.jumpKey);
        }

        float offset = offsetValue.get().floatValue();
        float size = sizeValue.get().floatValue();
        float increment = size + offset;

        this.width = increment * 3 - offset;
        this.height = increment * 3 - offset;

        float radius = radiusValue.get().floatValue();

        if (blur.get()) {
            withPixelCoords(x, y, width, height, (px, py, pw, ph) ->
                    Shader2DUtils.drawRoundedBlur(
                            getMatrix(),
                            px, py, pw, ph,
                            (float) (radius * mc.getWindow().getScaleFactor()),
                            new Color(0, 0, 0, 0),
                            blurStrength.get().floatValue(),
                            1.0f
                    ));
        }

        keyBindForward.render(x + width / 2f - size / 2f, y, size, size, radius);
        keyBindLeft.render(x, y + increment, size, size, radius);
        keyBindBack.render(x + increment, y + increment, size, size, radius);
        keyBindRight.render(x + increment * 2, y + increment, size, size, radius);
        keyBindJump.render(x, y + increment * 2, width, size, radius);
    }

    private Color getBaseColor() {
        int rgb = whiteMode.get() ? new Color(255, 255, 255).getRGB() : new Color(30, 30, 30).getRGB();
        return ColorUtil.applyOpacity(new Color(rgb), opacityValue.get().floatValue());
    }

    private class Button {
        private final KeyBinding binding;
        private final Animation clickAnimation = new DecelerateAnimation(125, 1);

        public Button(KeyBinding binding) {
            this.binding = binding;
        }

        public void render(float x, float y, float w, float h, float radius) {
            Color baseColor = getBaseColor();
            clickAnimation.setDirection(binding.isPressed() ? Direction.FORWARDS : Direction.BACKWARDS);

            NanoVGHelper.drawRoundRect(x, y, w, h, radius, baseColor);

            String keyName = getKeyName(binding);
            int font = FontLoader.greycliffBold(h * 0.55f);
            float textWidth = NanoVGHelper.getTextWidth(keyName, font, h * 0.55f);
            float fontHeight = NanoVGHelper.getFontHeight(font, h * 0.55f);

            NanoVGHelper.drawString(
                    keyName,
                    x + w / 2f - textWidth / 2f,
                    y + h / 2f + fontHeight / 3f,
                    font,
                    h * 0.55f,
                    new Color(255, 255, 255, 255)
            );

            if (!clickAnimation.finished(Direction.BACKWARDS)) {
                float animation = clickAnimation.getOutput().floatValue();
                Color pressColor = ColorUtil.applyOpacity(
                        whiteMode.get() ? new Color(255, 255, 255) : new Color(30, 30, 30),
                        0.5f * animation
                );

                long vg = NanoVGRenderer.INSTANCE.getContext();

                nvgSave(vg);

                float centerX = x + w / 2f;
                float centerY = y + h / 2f;
                nvgTranslate(vg, centerX, centerY);
                nvgScale(vg, animation, animation);
                nvgTranslate(vg, -centerX, -centerY);

                float diff = (h / 2f) - radius;
                float animRadius = (h / 2f) - (diff * animation);

                NanoVGHelper.drawRoundRect(x, y, w, h, animRadius, pressColor);

                nvgRestore(vg);
            }
        }

        private String getKeyName(KeyBinding binding) {
            String translationKey = binding.getBoundKeyLocalizedText().getString();
            if (translationKey.length() == 1) {
                return translationKey.toUpperCase();
            }
            if (translationKey.toLowerCase().contains("space")) {
                return "—";
            }
            if (translationKey.toLowerCase().contains("shift")) {
                return "Shift";
            }
            if (translationKey.toLowerCase().contains("control") || translationKey.toLowerCase().contains("ctrl")) {
                return "Ctrl";
            }
            return translationKey;
        }
    }
}