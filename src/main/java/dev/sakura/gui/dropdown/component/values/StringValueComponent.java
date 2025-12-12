package dev.sakura.gui.dropdown.component.values;

import dev.sakura.Sakura;
import dev.sakura.gui.Component;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.render.RenderUtils;
import dev.sakura.values.impl.StringValue;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class StringValueComponent extends Component {
    private final StringValue setting;
    private boolean editing = false;
    private String tempText = "";
    private int cursorPos = 0;
    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;

    public StringValueComponent(StringValue setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        setHeight(52);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBlinkTime > 530) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = currentTime;
        }

        NanoVGRenderer.INSTANCE.draw(vg -> {
            // 第一行：绘制名称
            NanoVGHelper.drawString(setting.getName(), getX(), getY(),
                    FontLoader.greycliffRegular(15), 15, new Color(255, 255, 255, 255));

            // 第二行：绘制输入框（死亡手动输入xywh）
            float inputWidth = 200;
            float inputX = getX() + 2;
            float inputY = getY() + 8;
            float inputHeight = 24;

            // 绘制背景
            NanoVGHelper.drawRoundRect(inputX, inputY, inputWidth, inputHeight, 4,
                    editing ? new Color(60, 60, 80) : new Color(40, 40, 40));

            // 绘制轮廓线
            NanoVGHelper.drawRoundRectOutline(inputX, inputY, inputWidth, inputHeight, 4, 1.5f,
                    editing ? new Color(100, 100, 150) : new Color(80, 80, 80));

            String displayText = editing ? tempText : setting.get();
            if (displayText == null) displayText = "";

            float textWidth = NanoVGHelper.getTextWidth(displayText, FontLoader.greycliffRegular(13), 13);
            String trimmedText = displayText;

            if (textWidth > inputWidth - 16) {
                while (textWidth > inputWidth - 16 && !trimmedText.isEmpty()) {
                    if (editing && cursorPos == displayText.length()) {
                        trimmedText = trimmedText.substring(1);
                    } else {
                        trimmedText = trimmedText.substring(0, trimmedText.length() - 1);
                    }
                    textWidth = NanoVGHelper.getTextWidth(trimmedText + (editing && cursorPos == displayText.length() ? "" : "..."),
                            FontLoader.greycliffRegular(13), 13);
                }
                if (!editing || cursorPos < displayText.length()) {
                    trimmedText = trimmedText + "...";
                }
            }

            NanoVGHelper.drawString(trimmedText, inputX + 6, inputY + 17,
                    FontLoader.greycliffRegular(13), 13,
                    editing ? new Color(255, 255, 255) : new Color(200, 200, 200));

            if (editing && cursorVisible) {
                String beforeCursor = tempText.substring(0, Math.min(cursorPos, tempText.length()));
                float cursorX = inputX + 6 + NanoVGHelper.getTextWidth(beforeCursor, FontLoader.greycliffRegular(13), 13);

                if (cursorX < inputX + inputWidth - 6) {
                    NanoVGHelper.drawRect(cursorX, inputY + 4, 1, inputHeight - 8, new Color(255, 255, 255));
                }
            }
        });

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float scaledMouseX = (float) (mouseX * Sakura.mc.options.getGuiScale().getValue());
        float scaledMouseY = (float) (mouseY * Sakura.mc.options.getGuiScale().getValue());

        float inputWidth = 200;
        float inputX = getX() + 2;
        float inputY = getY() + 8;
        float inputHeight = 24;

        if (RenderUtils.isHovering(inputX, inputY, inputWidth, inputHeight, scaledMouseX, scaledMouseY) && mouseButton == 0) {
            if (!editing) {
                editing = true;
                tempText = setting.get();
                cursorPos = tempText.length();
                lastBlinkTime = System.currentTimeMillis();
                cursorVisible = true;
            }
            return true;
        } else if (editing && mouseButton == 0) {
            finishEditing();
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
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

        if (setting.isOnlyNumber() && !Character.isDigit(chr) && chr != '.' && chr != '-') {
            return false;
        }

        tempText = tempText.substring(0, cursorPos) + chr + tempText.substring(cursorPos);
        cursorPos++;
        resetCursor();

        return true;
    }

    private void finishEditing() {
        editing = false;
        setting.setText(tempText);
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
