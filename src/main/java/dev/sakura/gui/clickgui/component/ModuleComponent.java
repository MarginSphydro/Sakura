package dev.sakura.gui.clickgui.component;

import dev.sakura.gui.Component;
import dev.sakura.gui.IComponent;
import dev.sakura.gui.clickgui.component.values.*;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.EaseInOutQuad;
import dev.sakura.utils.animations.impl.EaseOutSine;
import dev.sakura.utils.color.ColorUtil;
import dev.sakura.utils.render.RenderUtils;
import dev.sakura.values.Value;
import dev.sakura.values.impl.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleComponent implements IComponent {
    private static final int MODULE_HEIGHT = 18;

    private float x, y, width, height = MODULE_HEIGHT;
    private final Module module;
    private boolean opened;
    private boolean listening = false;
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);
    private final EaseOutSine toggleAnimation = new EaseOutSine(300, 1);
    private final EaseOutSine hoverAnimation = new EaseOutSine(200, 1);
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();

    public ModuleComponent(Module module) {
        this.module = module;
        openAnimation.setDirection(Direction.BACKWARDS);
        toggleAnimation.setDirection(Direction.BACKWARDS);
        hoverAnimation.setDirection(Direction.BACKWARDS);
        for (Value<?> value : module.getValues()) {
            if (value instanceof BoolValue boolValue) {
                settings.add(new BoolValueComponent(boolValue));
            } else if (value instanceof NumberValue<?> numberValue) {
                settings.add(new NumberValueComponent(numberValue));
            } else if (value instanceof EnumValue<?> modeComponent) {
                settings.add(new EnumValueComponent(modeComponent));
            } else if (value instanceof ColorValue colorSetting) {
                settings.add(new ColorValueComponent(colorSetting));
            } else if (value instanceof MultiBoolValue multiBoolValue) {
                settings.add(new MultiBoolValueComponent(multiBoolValue));
            } else if (value instanceof StringValue stringValue) {
                settings.add(new StringValueComponent(stringValue));
            }
        }
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float yOffset = 18;
        openAnimation.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        toggleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
        hoverAnimation.setDirection(isHovered(mouseX, mouseY) ? Direction.FORWARDS : Direction.BACKWARDS);

        boolean hasVisibleSettings = false;
        for (Component component : settings) {
            if (!component.isVisible()) continue;
            hasVisibleSettings = true;
            yOffset += (float) (component.getHeight() * openAnimation.getOutput());
        }

        if (hasVisibleSettings && openAnimation.getOutput() > 0) {
            yOffset += (float) (4 * openAnimation.getOutput());
        }

        this.height = yOffset;

        final boolean finalHasVisibleSettings = hasVisibleSettings;
        final float finalYOffset = yOffset;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            if (module.isEnabled()) {
                NanoVGHelper.drawGradientRRect2(x, y, width, 18, 0, ClickGui.color(0), ClickGui.color2(0));
            }
            NanoVGHelper.drawRect(x, y, width, MODULE_HEIGHT, ColorUtil.applyOpacity(ClickGui.backgroundColor.get(), 0.4f));

            if (finalHasVisibleSettings && openAnimation.getOutput() > 0) {
                float expandedHeight = (float) ((finalYOffset - 18) * openAnimation.getOutput());
                NanoVGHelper.drawRect(x, y + 18, width, expandedHeight,
                        ColorUtil.applyOpacity(ClickGui.expandedBackgroundColor.get(), (float) (0.3f * openAnimation.getOutput())));
            }

            NanoVGHelper.drawString(module.getName(), x + 4, y + 11, FontLoader.greycliffRegular(7.5f), 7.5f, Color.WHITE);

            float boxWidth = 18;
            float boxHeight = 8;
            float boxX = x + width - boxWidth - 4;
            float boxY = y + (MODULE_HEIGHT - boxHeight) / 2;

            int keyCode = module.getKey();
            boolean hasKey = keyCode != 0 && keyCode != GLFW.GLFW_KEY_UNKNOWN;
            boolean isHold = module.getBindMode() == Module.BindMode.Hold;

            Color themeColor = ClickGui.color(0);
            Color bgColor;
            Color borderColor;

            if (listening) {
                bgColor = new Color(255, 100, 100, 180);
                borderColor = new Color(255, 150, 150, 220);
            } else {
                bgColor = ColorUtil.applyOpacity(themeColor, hasKey ? 0.6f : 0.3f);
                borderColor = ColorUtil.applyOpacity(themeColor, hasKey ? 0.9f : 0.5f);
            }

            NanoVGHelper.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 2, bgColor);
            NanoVGHelper.drawRoundRectOutline(boxX, boxY, boxWidth, boxHeight, 2, 0.5f, borderColor);

            float fontSize = 5;
            int font = FontLoader.greycliffRegular(fontSize);
            String displayText = listening ? "..." : (hasKey ? getKeyName(keyCode) : "");
            float textWidth = NanoVGHelper.getTextWidth(displayText, font, fontSize);
            float textX = boxX + (boxWidth - textWidth) / 2;
            float textY = boxY + boxHeight - 2;
            if (!displayText.isEmpty()) NanoVGHelper.drawString(displayText, textX, textY, font, fontSize, Color.WHITE);

            if (isHold && !listening) {
                float lineWidth = hasKey ? textWidth + 1 : 6;
                float lineX = hasKey ? textX - 0.5f : boxX + (boxWidth - lineWidth) / 2;
                float lineY = boxY + boxHeight - 1;
                NanoVGHelper.drawRect(lineX, lineY, lineWidth, 0.5f, Color.WHITE);
            }
        });

        float componentYOffset = 18;
        for (Component component : settings) {
            if (!component.isVisible()) continue;
            component.setX(x + 4);
            component.setY((float) (y + 10 + componentYOffset * openAnimation.getOutput()));
            component.setWidth(width - 8);
            if (openAnimation.getOutput() > .7f) {
                component.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            componentYOffset += component.getHeight();
        }

        IComponent.super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isBindBoxHovered((int) mouseX, (int) mouseY)) {
            if (mouseButton == 0) {
                listening = !listening;
                return true;
            } else if (mouseButton == 2) {
                module.setBindMode(module.getBindMode() == Module.BindMode.Toggle ? Module.BindMode.Hold : Module.BindMode.Toggle);
                return true;
            }
        } else if (listening) {
            listening = false;
        }

        if (isHovered((int) mouseX, (int) mouseY) && !isBindBoxHovered((int) mouseX, (int) mouseY)) {
            switch (mouseButton) {
                case 0 -> module.toggle();
                case 1 -> opened = !opened;
            }
        }
        if (opened && !isHovered((int) mouseX, (int) mouseY)) {
            settings.forEach(setting -> setting.mouseClicked(mouseX, mouseY, mouseButton));
        }
        return IComponent.super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (opened && !isHovered((int) mouseX, (int) mouseY)) {
            settings.forEach(setting -> setting.mouseReleased(mouseX, mouseY, state));
        }
        return IComponent.super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                module.setKey(0);
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                module.setKey(keyCode);
            }
            listening = false;
            return true;
        }
        if (opened) {
            settings.forEach(setting -> setting.keyPressed(keyCode, scanCode, modifiers));
        }
        return IComponent.super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (opened) {
            for (Component setting : settings) {
                if (setting.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return IComponent.super.charTyped(chr, modifiers);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return RenderUtils.isHovering(x, y, width, MODULE_HEIGHT, mouseX, mouseY);
    }

    public boolean isBindBoxHovered(int mouseX, int mouseY) {
        float boxWidth = 18;
        float boxHeight = 8;
        float boxX = x + width - boxWidth - 4;
        float boxY = y + (MODULE_HEIGHT - boxHeight) / 2;
        return RenderUtils.isHovering(boxX, boxY, boxWidth, boxHeight, mouseX, mouseY);
    }

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Module getModule() {
        return module;
    }

    public boolean isOpened() {
        return opened;
    }

    public EaseInOutQuad getOpenAnimation() {
        return openAnimation;
    }

    public EaseOutSine getToggleAnimation() {
        return toggleAnimation;
    }

    public EaseOutSine getHoverAnimation() {
        return hoverAnimation;
    }

    public CopyOnWriteArrayList<Component> getSettings() {
        return settings;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    private String getKeyName(int keyCode) {
        if (keyCode < 0) {
            return "M" + (-keyCode);
        }
        try {
            InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(keyCode);
            String name = key.getLocalizedText().getString();
            if (name.length() > 6) {
                name = name.substring(0, 5) + ".";
            }
            return name.toUpperCase();
        } catch (Exception e) {
            return "?";
        }
    }
}
