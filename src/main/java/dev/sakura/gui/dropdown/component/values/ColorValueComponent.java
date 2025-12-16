package dev.sakura.gui.dropdown.component.values;

import dev.sakura.gui.Component;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Animation;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.EaseOutSine;
import dev.sakura.values.impl.ColorValue;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ColorValueComponent extends Component {
    private final ColorValue setting;
    private final Animation open = new EaseOutSine(250, 1);
    private boolean opened, pickingHue, pickingOthers, pickingAlpha;
    private boolean pickingR, pickingG, pickingB;

    private enum EditField {NONE, R, G, B}

    private EditField editField = EditField.NONE;
    private String tempText = "";
    private int cursorPos = 0;
    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;

    public ColorValueComponent(ColorValue setting) {
        this.setting = setting;
        open.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        open.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        float fontHeight = NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(7.5f), 7.5f);

        float baseHeight = fontHeight + 2 + 50 + 3 + 5;
        if (setting.allowAlpha()) {
            baseHeight += 3 + 5;
        }
        baseHeight += 3 + 5 + 8;

        this.setHeight((float) (18 + (baseHeight * open.getOutput())));
        final float[] hsb = new float[]{setting.getHue(), setting.getSaturation(), setting.getBrightness()};
        final float alpha = setting.getAlpha();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > 530) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = currentTime;
        }

        Color currentColor = setting.get();
        int r = currentColor.getRed();
        int g = currentColor.getGreen();
        int b = currentColor.getBlue();

        NanoVGRenderer.INSTANCE.draw(canvas -> {
            NanoVGHelper.drawString(setting.getName(), getX(), getY(), FontLoader.greycliffRegular(7.5f), 7.5f, new Color(255, 255, 255, 255));

            NanoVGHelper.drawCircle(getX() + getWidth() - 9, getY() - 3, 4, setting.get());

            if (open.getOutput() > 0.01) {
                float sliderX = getX();
                float sliderWidth = getWidth();
                float gradientX = getX();
                float gradientY = getY() + fontHeight + 2;
                float gradientWidth = getWidth();
                float gradientHeight = (float) (50 * open.getOutput());

                NanoVGHelper.drawGradientRect3(getX(), gradientY,
                        gradientWidth, gradientHeight, Color.BLACK, Color.WHITE, Color.BLACK, Color.getHSBColor(setting.getHue(), 1, 1));

                for (int max = (int) gradientWidth, i = 0; i < max; i++) {
                    NanoVGHelper.drawRect(getX() + i,
                            gradientY + gradientHeight + 2,
                            1, 5, Color.getHSBColor(i / (float) max, 1, 1));
                }

                float hueSliderY = gradientY + gradientHeight + 2;
                float hueHandleX = sliderX + (sliderWidth * hsb[0]);
                hueHandleX = Math.max(sliderX + 1, Math.min(sliderX + sliderWidth - 1, hueHandleX));
                NanoVGHelper.drawRect((int) hueHandleX, (int) hueSliderY - 1, 1, 7, new Color(255, 255, 255));

                float nextY = hueSliderY + 5;

                if (setting.allowAlpha()) {
                    float alphaSliderY = nextY + 2;
                    drawCheckerboard(getX(), alphaSliderY, sliderWidth, 5);

                    for (int max = (int) sliderWidth, i = 0; i < max; i++) {
                        float alphaValue = i / (float) max;
                        Color alphaColor = new Color(r, g, b, (int) (alphaValue * 255));
                        NanoVGHelper.drawRect(getX() + i, alphaSliderY, 1, 5, alphaColor);
                    }

                    float alphaHandleX = sliderX + (sliderWidth * alpha);
                    alphaHandleX = Math.max(sliderX + 1, Math.min(sliderX + sliderWidth - 1, alphaHandleX));
                    NanoVGHelper.drawRect((int) alphaHandleX, (int) alphaSliderY - 1, 1, 7, new Color(255, 255, 255));

                    nextY = alphaSliderY + 5;
                }

                float rgbY = nextY + 2;
                float rgbBarWidth = (sliderWidth - 4) / 3f;

                drawRgbBar(sliderX, rgbY, rgbBarWidth, r, new Color(255, 0, 0), new Color(50, 0, 0), "R", EditField.R);
                drawRgbBar(sliderX + rgbBarWidth + 2, rgbY, rgbBarWidth, g, new Color(0, 255, 0), new Color(0, 50, 0), "G", EditField.G);
                drawRgbBar(sliderX + (rgbBarWidth + 2) * 2, rgbY, rgbBarWidth, b, new Color(0, 0, 255), new Color(0, 0, 50), "B", EditField.B);

                float pickerY = (gradientY) + (gradientHeight * (1 - hsb[2]));
                float pickerX = (gradientX) + (gradientWidth * hsb[1] - 1);
                pickerY = Math.max(Math.min(gradientY + gradientHeight - 2, pickerY), gradientY - 2);
                pickerX = Math.max(Math.min(gradientX + gradientWidth - 2, pickerX), gradientX - 2);

                if (pickingHue) {
                    setting.setHue(Math.min(1, Math.max(0, (mouseX - gradientX) / gradientWidth)));
                }

                if (pickingOthers) {
                    setting.setBrightness(Math.min(1, Math.max(0, 1 - ((mouseY - gradientY) / gradientHeight))));
                    setting.setSaturation(Math.min(1, Math.max(0, (mouseX - gradientX) / gradientWidth)));
                }

                if (pickingAlpha && setting.allowAlpha()) {
                    float newAlpha = (mouseX - sliderX) / sliderWidth;
                    newAlpha = Math.max(0.0f, Math.min(1.0f, newAlpha));
                    setting.setAlpha(newAlpha);
                }

                if (pickingR) {
                    int newR = (int) (255 * Math.max(0, Math.min(1, (mouseX - sliderX) / rgbBarWidth)));
                    setColorRGB(newR, g, b);
                }
                if (pickingG) {
                    int newG = (int) (255 * Math.max(0, Math.min(1, (mouseX - (sliderX + rgbBarWidth + 4)) / rgbBarWidth)));
                    setColorRGB(r, newG, b);
                }
                if (pickingB) {
                    int newB = (int) (255 * Math.max(0, Math.min(1, (mouseX - (sliderX + (rgbBarWidth + 4) * 2)) / rgbBarWidth)));
                    setColorRGB(r, g, newB);
                }

                NanoVGHelper.drawRect(pickerX, pickerY, 2, 2, new Color(255, 255, 255));
            }
        });

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawRgbBar(float x, float y, float width, int value, Color highColor, Color lowColor, String label, EditField field) {
        NanoVGHelper.drawRect(x, y, width, 5, lowColor);
        float filledWidth = (value / 255f) * width;
        NanoVGHelper.drawRect(x, y, filledWidth, 5, highColor);

        float handleX = x + filledWidth;
        handleX = Math.max(x + 1, Math.min(x + width - 1, handleX));
        NanoVGHelper.drawRect(handleX - 0.5f, y - 1, 1, 7, Color.WHITE);

        float inputY = y + 6;
        float inputHeight = 8;

        if (editField == field) {
            NanoVGHelper.drawRoundRect(x, inputY, width, inputHeight, 1, new Color(60, 60, 80));
            NanoVGHelper.drawRoundRectOutline(x, inputY, width, inputHeight, 1, 0.5f, new Color(100, 100, 150));

            String displayText = tempText;
            NanoVGHelper.drawString(displayText, x + 2, inputY + 6, FontLoader.greycliffRegular(5), 5, Color.WHITE);

            if (cursorVisible) {
                String beforeCursor = tempText.substring(0, Math.min(cursorPos, tempText.length()));
                float cursorX = x + 2 + NanoVGHelper.getTextWidth(beforeCursor, FontLoader.greycliffRegular(5), 5);
                NanoVGHelper.drawRect(cursorX, inputY + 1, 0.5f, inputHeight - 2, Color.WHITE);
            }
        } else {
            NanoVGHelper.drawRoundRect(x, inputY, width, inputHeight, 1, new Color(40, 40, 40));
            String text = label + ": " + value;
            NanoVGHelper.drawString(text, x + 2, inputY + 6, FontLoader.greycliffRegular(5), 5, new Color(200, 200, 200));
        }
    }

    private void setColorRGB(int r, int g, int b) {
        float currentAlpha = setting.getAlpha();
        Color newColor = new Color(r, g, b, (int) (currentAlpha * 255));
        setting.set(newColor);
        setting.setAlpha(currentAlpha);
    }

    private void drawCheckerboard(float x, float y, float width, float height) {
        NanoVGHelper.drawRect(x, y, width, height, new Color(200, 200, 200));

        int squareSize = 4;
        boolean white = true;
        for (int i = 0; i < width; i += squareSize) {
            for (int j = 0; j < height; j += squareSize) {
                if (!white) {
                    Color color = new Color(150, 150, 150);
                    float drawWidth = Math.min(squareSize, width - i);
                    float drawHeight = Math.min(squareSize, height - j);

                    if (i > 2 && i < width - 2 || j > 0 && j < height - 0) {
                        NanoVGHelper.drawRect(x + i, y + j, drawWidth, drawHeight, color);
                    }
                }
                white = !white;
            }
            if (height / squareSize % 2 == 0) {
                white = !white;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isHovering(getX() + getWidth() - 17, getY() - 7, 8, 8, mouseX, mouseY)) {
            opened = !opened;
            if (!opened) {
                editField = EditField.NONE;
            }
        }

        if (opened && open.getOutput() > 0.5) {
            float fontHeight = NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(8), 8);
            float gradientY = getY() + fontHeight + 2;
            float gradientHeight = (float) (50 * open.getOutput());
            float sliderX = getX();
            float sliderWidth = getWidth() - 8;

            if (isHovering(getX(), gradientY, sliderWidth, gradientHeight, mouseX, mouseY) && mouseButton == 0) {
                pickingOthers = true;
                editField = EditField.NONE;
            }

            float hueSliderY = gradientY + gradientHeight + 2;
            if (isHovering(getX(), hueSliderY, sliderWidth, 5, mouseX, mouseY) && mouseButton == 0) {
                pickingHue = true;
                editField = EditField.NONE;
            }

            float nextY = hueSliderY + 5;

            if (setting.allowAlpha()) {
                float alphaSliderY = nextY + 2;
                if (isHovering(getX(), alphaSliderY, sliderWidth, 5, mouseX, mouseY) && mouseButton == 0) {
                    pickingAlpha = true;
                    editField = EditField.NONE;
                }
                nextY = alphaSliderY + 5;
            }

            float rgbY = nextY + 2;
            float rgbBarWidth = (sliderWidth - 4) / 3f;

            if (isHovering(sliderX, rgbY, rgbBarWidth, 5, mouseX, mouseY) && mouseButton == 0) {
                pickingR = true;
                editField = EditField.NONE;
            }
            if (isHovering(sliderX + rgbBarWidth + 2, rgbY, rgbBarWidth, 5, mouseX, mouseY) && mouseButton == 0) {
                pickingG = true;
                editField = EditField.NONE;
            }
            if (isHovering(sliderX + (rgbBarWidth + 2) * 2, rgbY, rgbBarWidth, 5, mouseX, mouseY) && mouseButton == 0) {
                pickingB = true;
                editField = EditField.NONE;
            }

            float inputY = rgbY + 6;
            float inputHeight = 8;
            Color currentColor = setting.get();

            if (mouseButton == 0) {
                if (isHovering(sliderX, inputY, rgbBarWidth, inputHeight, mouseX, mouseY)) {
                    startEditing(EditField.R, currentColor.getRed());
                    return true;
                }
                if (isHovering(sliderX + rgbBarWidth + 2, inputY, rgbBarWidth, inputHeight, mouseX, mouseY)) {
                    startEditing(EditField.G, currentColor.getGreen());
                    return true;
                }
                if (isHovering(sliderX + (rgbBarWidth + 2) * 2, inputY, rgbBarWidth, inputHeight, mouseX, mouseY)) {
                    startEditing(EditField.B, currentColor.getBlue());
                    return true;
                }

                if (editField != EditField.NONE) {
                    finishEditing();
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void startEditing(EditField field, int value) {
        editField = field;
        tempText = String.valueOf(value);
        cursorPos = tempText.length();
        lastBlinkTime = System.currentTimeMillis();
        cursorVisible = true;
    }

    private void finishEditing() {
        if (editField == EditField.NONE) return;

        try {
            int value = Integer.parseInt(tempText.trim());
            value = Math.max(0, Math.min(255, value));

            Color currentColor = setting.get();
            int r = currentColor.getRed();
            int g = currentColor.getGreen();
            int b = currentColor.getBlue();

            switch (editField) {
                case R -> r = value;
                case G -> g = value;
                case B -> b = value;
            }

            setColorRGB(r, g, b);
        } catch (NumberFormatException ignored) {
        }

        editField = EditField.NONE;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0) {
            pickingHue = false;
            pickingOthers = false;
            pickingAlpha = false;
            pickingR = false;
            pickingG = false;
            pickingB = false;
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editField == EditField.NONE) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                finishEditing();
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                editField = EditField.NONE;
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
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editField == EditField.NONE) return false;

        if (chr >= '0' && chr <= '9' && tempText.length() < 3) {
            tempText = tempText.substring(0, cursorPos) + chr + tempText.substring(cursorPos);
            cursorPos++;
            resetCursor();
            return true;
        }

        return false;
    }

    private void resetCursor() {
        lastBlinkTime = System.currentTimeMillis();
        cursorVisible = true;
    }

    @Override
    public boolean isVisible() {
        return this.setting.isAvailable();
    }

    private boolean isHovering(double x, double y, double width, double height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
