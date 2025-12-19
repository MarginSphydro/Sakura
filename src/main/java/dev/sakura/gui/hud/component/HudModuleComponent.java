package dev.sakura.gui.hud.component;

import dev.sakura.gui.Component;
import dev.sakura.gui.IComponent;
import dev.sakura.gui.clickgui.component.values.*;
import dev.sakura.module.HudModule;
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

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class HudModuleComponent implements IComponent {
    private static final int MODULE_HEIGHT = 18;

    private float x, y, width, height = MODULE_HEIGHT;
    private final HudModule hudModule;
    private boolean opened;
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);
    private final EaseOutSine toggleAnimation = new EaseOutSine(300, 1);
    private final EaseOutSine hoverAnimation = new EaseOutSine(200, 1);
    private final CopyOnWriteArrayList<Component> settings = new CopyOnWriteArrayList<>();

    public HudModuleComponent(HudModule hudModule) {
        this.hudModule = hudModule;
        openAnimation.setDirection(Direction.BACKWARDS);
        toggleAnimation.setDirection(Direction.BACKWARDS);
        hoverAnimation.setDirection(Direction.BACKWARDS);

        for (Value<?> value : hudModule.getValues()) {
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
        toggleAnimation.setDirection(hudModule.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
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
            if (hudModule.isEnabled()) {
                NanoVGHelper.drawGradientRRect2(x, y, width, 18, 0, ClickGui.color(0), ClickGui.color2(0));
            }
            NanoVGHelper.drawRect(x, y, width, MODULE_HEIGHT, ColorUtil.applyOpacity(ClickGui.backgroundColor.get(), 0.4f));

            if (finalHasVisibleSettings && openAnimation.getOutput() > 0) {
                float expandedHeight = (float) ((finalYOffset - 18) * openAnimation.getOutput());
                NanoVGHelper.drawRect(x, y + 18, width, expandedHeight,
                        ColorUtil.applyOpacity(ClickGui.expandedBackgroundColor.get(), (float) (0.3f * openAnimation.getOutput())));
            }

            NanoVGHelper.drawString(hudModule.getName(), x + 4, y + 11, FontLoader.regular(7.5f), 7.5f, Color.WHITE);
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
        if (isHovered((int) mouseX, (int) mouseY)) {
            switch (mouseButton) {
                case 0 -> hudModule.toggle();
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
        return RenderUtils.isHovering(x, y, width, 18, mouseX, mouseY);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public HudModule getHudModule() {
        return hudModule;
    }

    public boolean isOpened() {
        return opened;
    }
}
