package dev.sakura.gui.dropdown.component;

import dev.sakura.Sakura;
import dev.sakura.gui.Component;
import dev.sakura.gui.IComponent;
import dev.sakura.gui.dropdown.component.values.*;
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
    private static final int MODULE_HEIGHT = 36;

    private float x, y, width, height = MODULE_HEIGHT;
    private final Module module;
    private boolean opened;
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
        float yOffset = 36;
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
            yOffset += (float) (8 * openAnimation.getOutput());
        }

        this.height = yOffset;

        final boolean finalHasVisibleSettings = hasVisibleSettings;
        final float finalYOffset = yOffset;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            if (module.isEnabled()) {
                NanoVGHelper.drawGradientRRect2(x, y, width, 36, 0, ClickGui.color(0), ClickGui.color2(0));
            }
            NanoVGHelper.drawRect(x, y, width, MODULE_HEIGHT, ColorUtil.applyOpacity(ClickGui.backgroundColor.get(), 0.4f));

            if (finalHasVisibleSettings && openAnimation.getOutput() > 0) {
                float expandedHeight = (float) ((finalYOffset - 36) * openAnimation.getOutput());
                NanoVGHelper.drawRect(x, y + 36, width, expandedHeight,
                        ColorUtil.applyOpacity(ClickGui.expandedBackgroundColor.get(), (float) (0.3f * openAnimation.getOutput())));
            }

            NanoVGHelper.drawString(module.getName(), x + 8, y + 23, FontLoader.greycliffRegular(15), 15, Color.WHITE);

            int keyCode = module.getKey();
            if (keyCode != 0 && keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                String keyName = getKeyName(keyCode);
                String modeIndicator = module.getBindMode() == Module.BindMode.Hold ? "H" : "";
                String displayText = modeIndicator.isEmpty() ? keyName : keyName + " " + modeIndicator;

                float fontSize = 10;
                int font = FontLoader.greycliffRegular(fontSize);
                float boxWidth = 36;
                float boxHeight = 16;
                float boxX = x + width - boxWidth - 8;
                float boxY = y + (MODULE_HEIGHT - boxHeight) / 2;

                Color themeColor = ClickGui.color(0);
                Color bgColor = ColorUtil.applyOpacity(themeColor, 0.6f);
                Color borderColor = ColorUtil.applyOpacity(themeColor, 0.9f);

                NanoVGHelper.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 4, bgColor);
                NanoVGHelper.drawRoundRectOutline(boxX, boxY, boxWidth, boxHeight, 4, 1f, borderColor);

                float textWidth = NanoVGHelper.getTextWidth(displayText, font, fontSize);
                float textX = boxX + (boxWidth - textWidth) / 2;
                NanoVGHelper.drawString(displayText, textX, boxY + boxHeight - 4, font, fontSize, Color.WHITE);
            }
        });

        float componentYOffset = 36;
        for (Component component : settings) {
            if (!component.isVisible()) continue;
            component.setX(x + 8);
            component.setY((float) (y + 22 + componentYOffset * openAnimation.getOutput()));
            component.setWidth(width);
            if (openAnimation.getOutput() > .7f) {
                component.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            componentYOffset += component.getHeight();
        }

        IComponent.super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isHovered((int) mouseX, (int) mouseY)) {
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
        int scale = Sakura.mc.options.getGuiScale().getValue();
        return RenderUtils.isHovering(x, y, width, MODULE_HEIGHT, mouseX * scale, mouseY * scale);
    }

    // Getter methods
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

    // Setter methods
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