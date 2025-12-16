package dev.sakura.gui.hud;

import dev.sakura.Sakura;
import dev.sakura.gui.IComponent;
import dev.sakura.gui.hud.component.HudModuleComponent;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.EaseInOutQuad;
import dev.sakura.utils.render.RenderUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class HudPanel implements IComponent {
    private float x, y, dragX, dragY;
    private float width = 110, height;
    private boolean dragging, opened;
    private final ObjectArrayList<HudModuleComponent> hudComponents = new ObjectArrayList<>();
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);

    public HudPanel() {
        this.opened = true;
        this.openAnimation.setDirection(Direction.BACKWARDS);

        for (Module module : Sakura.MODULE.getAllModules()) {
            if (module instanceof HudModule) {
                hudComponents.add(new HudModuleComponent((HudModule) module));
            }
        }
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        update(mouseX, mouseY);

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawRoundRectBloom(x, y - 1, width, (float) (18 + ((height - 18))), 7, new Color(30, 30, 30, 180));

            NanoVGHelper.drawString("HUD", x + 4, y + 12f, FontLoader.greycliffBold(10), 10, new Color(255, 255, 255, 255));
            NanoVGHelper.drawString("H", x + 4 + width - NanoVGHelper.getTextWidth("H", FontLoader.icons(15), 15) - 3, y + 13f, FontLoader.icons(15), 15, new Color(255, 255, 255, 255));
        });

        float componentOffsetY = 18;

        for (HudModuleComponent component : hudComponents) {
            component.setX(x);
            component.setY(y + componentOffsetY);
            component.setWidth(width);
            if (openAnimation.getOutput() > 0.7f) {
                component.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            componentOffsetY += (float) (component.getHeight() * openAnimation.getOutput());
        }

        height = componentOffsetY + 9;

        IComponent.super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isHovered((int) mouseX, (int) mouseY)) {
            switch (mouseButton) {
                case 0 -> {
                    dragging = true;
                    dragX = (float) (x - mouseX);
                    dragY = (float) (y - mouseY);
                }
                case 1 -> opened = !opened;
            }
            return true;
        }

        boolean handled = false;
        for (HudModuleComponent component : hudComponents) {
            if (component.mouseClicked(mouseX, mouseY, mouseButton)) {
                handled = true;
            }
        }

        return handled || IComponent.super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0) dragging = false;

        boolean handled = false;
        for (HudModuleComponent component : hudComponents) {
            if (component.mouseReleased(mouseX, mouseY, state)) {
                handled = true;
            }
        }

        return handled || IComponent.super.mouseReleased(mouseX, mouseY, state);
    }

    public void update(int mouseX, int mouseY) {
        this.openAnimation.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        if (dragging) {
            x = mouseX + dragX;
            y = mouseY + dragY;
        }
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

    public float getHeight() {
        return height;
    }

    public boolean isDragging() {
        return dragging;
    }
}
