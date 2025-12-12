package dev.sakura.gui.dropdown.component.values;

import dev.sakura.Sakura;
import dev.sakura.gui.Component;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Animation;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.DecelerateAnimation;
import dev.sakura.utils.math.MathUtils;
import dev.sakura.utils.render.RenderUtils;
import dev.sakura.values.impl.NumberValue;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.text.DecimalFormat;

public class NumberValueComponent extends Component {
    private static NumberValueComponent currentEditing = null;

    private final NumberValue<? extends Number> setting;
    private boolean dragging;
    private final Animation drag = new DecelerateAnimation(250, 1);
    private float anim;
    private final boolean isInteger;

    private boolean editing = false;
    private String tempText = "";
    private int cursorPos = 0;
    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;

    public NumberValueComponent(NumberValue<? extends Number> setting) {
        this.setting = setting;
        this.isInteger = setting.getValue() instanceof Integer;
        drag.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        setHeight(60);
        int w = 200;
        double min = setting.getMin().doubleValue();
        double max = setting.getMax().doubleValue();
        double current = setting.getValue().doubleValue();

        anim = RenderUtils.animate(anim, (float) (w * (current - min) / (max - min)), 50);
        float sliderWidth = anim;
        drag.setDirection(dragging ? Direction.FORWARDS : Direction.BACKWARDS);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > 530) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = currentTime;
        }

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawString(setting.getName(), getX(), getY(), FontLoader.greycliffRegular(15), 15, new Color(255, 255, 255, 255));

            if (editing) {
                float inputWidth = w;
                float inputX = getX();
                float inputY = getY() + 8;
                float inputHeight = 24;

                NanoVGHelper.drawRoundRect(inputX, inputY, inputWidth, inputHeight, 4, new Color(60, 60, 80));
                NanoVGHelper.drawRoundRectOutline(inputX, inputY, inputWidth, inputHeight, 4, 1.5f, new Color(100, 100, 150));

                String displayText = tempText;
                NanoVGHelper.drawString(displayText, inputX + 6, inputY + 17, FontLoader.greycliffRegular(13), 13, new Color(255, 255, 255));

                if (cursorVisible) {
                    String beforeCursor = tempText.substring(0, Math.min(cursorPos, tempText.length()));
                    float cursorX = inputX + 6 + NanoVGHelper.getTextWidth(beforeCursor, FontLoader.greycliffRegular(13), 13);
                    if (cursorX < inputX + inputWidth - 6) {
                        NanoVGHelper.drawRect(cursorX, inputY + 4, 1, inputHeight - 8, new Color(255, 255, 255));
                    }
                }

                String hint = "Enter to confirm";
                float hintWidth = NanoVGHelper.getTextWidth(hint, FontLoader.greycliffRegular(10), 10);
                NanoVGHelper.drawString(hint, inputX + inputWidth - hintWidth - 6, inputY + inputHeight / 2 + 4, FontLoader.greycliffRegular(10), 10, new Color(150, 150, 150));
            } else {
                String minStr = isInteger ? String.valueOf(setting.getMin().intValue()) : String.valueOf(setting.getMin());
                String maxStr = isInteger ? String.valueOf(setting.getMax().intValue()) : String.valueOf(setting.getMax());
                String currentStr;
                if (isInteger) {
                    currentStr = String.valueOf(setting.getValue().intValue());
                } else {
                    DecimalFormat df = new DecimalFormat("#0.00");
                    currentStr = df.format(current);
                }

                NanoVGHelper.drawString(minStr, getX(), getY() + 36, FontLoader.greycliffRegular(12), 12, new Color(255, 255, 255, 255));
                NanoVGHelper.drawCenteredString(currentStr, getX() + getWidth() / 2, getY() + 36 + NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(12), 12) / 2, FontLoader.greycliffRegular(12), 12, new Color(255, 255, 255, 255));
                NanoVGHelper.drawString(maxStr, getX() + getWidth() - NanoVGHelper.getTextWidth(maxStr, FontLoader.greycliffRegular(12), 12) - 20, getY() + 36, FontLoader.greycliffRegular(12), 12, new Color(255, 255, 255, 255));

                NanoVGHelper.drawRoundRect(getX(), getY() + 8, w, 8, 4, new Color(200, 200, 200, 255));
                NanoVGHelper.drawGradientRRect2(getX(), getY() + 8, sliderWidth, 8, 2, ClickGui.color(0), ClickGui.color2(0));
                NanoVGHelper.drawCircle(getX() + 2 + sliderWidth, getY() + 12, 7, new Color(255, 255, 255));
            }
        });

        if (dragging && !editing) {
            float scaledMouseX = (float) (mouseX * Sakura.mc.options.getGuiScale().getValue());
            final double difference = max - min;
            final double value = min + MathUtils.clamp_double((scaledMouseX - getX()) / w, 0, 1) * difference;
            setValueFromDouble(MathUtils.incValue(value, setting.getStep().doubleValue()));
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @SuppressWarnings("unchecked")
    private void setValueFromDouble(double value) {
        if (isInteger) {
            ((NumberValue<Integer>) setting).setValue((int) value);
        } else {
            ((NumberValue<Double>) setting).setValue(value);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        int w = 200;
        float scaledMouseX = (float) (mouseX * Sakura.mc.options.getGuiScale().getValue());
        float scaledMouseY = (float) (mouseY * Sakura.mc.options.getGuiScale().getValue());

        if (RenderUtils.isHovering(getX(), getY(), w, 24, scaledMouseX, scaledMouseY)) {
            if (mouseButton == 0 && !editing) {
                dragging = true;
            } else if (mouseButton == 1 && !editing) {
                if (currentEditing != null && currentEditing != this) {
                    currentEditing.finishEditing();
                }
                currentEditing = this;
                editing = true;
                if (isInteger) {
                    tempText = String.valueOf(setting.getValue().intValue());
                } else {
                    tempText = String.valueOf(setting.getValue().doubleValue());
                }
                cursorPos = tempText.length();
                lastBlinkTime = System.currentTimeMillis();
                cursorVisible = true;
                return true;
            }
        } else if (editing && mouseButton == 0) {
            finishEditing();
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                finishEditing();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                editing = false;
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursorPos > 0) {
                    tempText = tempText.substring(0, cursorPos - 1) + tempText.substring(cursorPos);
                    cursorPos--;
                    resetCursor();
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursorPos < tempText.length()) {
                    tempText = tempText.substring(0, cursorPos) + tempText.substring(cursorPos + 1);
                    resetCursor();
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorPos > 0) {
                    cursorPos--;
                    resetCursor();
                }
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorPos < tempText.length()) {
                    cursorPos++;
                    resetCursor();
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursorPos = 0;
                resetCursor();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursorPos = tempText.length();
                resetCursor();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!editing) return false;

        if (!Character.isDigit(chr) && chr != '.' && chr != '-') {
            return false;
        }

        if (chr == '.' && (isInteger || tempText.contains("."))) {
            return false;
        }

        if (chr == '-' && cursorPos != 0) {
            return false;
        }

        tempText = tempText.substring(0, cursorPos) + chr + tempText.substring(cursorPos);
        cursorPos++;
        resetCursor();

        return true;
    }

    private void finishEditing() {
        editing = false;
        if (currentEditing == this) {
            currentEditing = null;
        }
        try {
            double value = Double.parseDouble(tempText);
            double min = setting.getMin().doubleValue();
            double max = setting.getMax().doubleValue();
            value = Math.max(min, Math.min(max, value));
            setValueFromDouble(value);
        } catch (NumberFormatException ignored) {
        }
    }

    private void resetCursor() {
        lastBlinkTime = System.currentTimeMillis();
        cursorVisible = true;
    }

    @Override
    public boolean isVisible() {
        return setting.isAvailable();
    }
}
