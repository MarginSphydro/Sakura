package dev.sakura.gui.dropdown.panel;

import dev.sakura.Sakura;
import dev.sakura.gui.IComponent;
import dev.sakura.gui.dropdown.component.ModuleComponent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.EaseInOutQuad;
import dev.sakura.utils.render.RenderUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class CategoryPanel implements IComponent {
    private float x, y, dragX, dragY;
    private float width = 220, height;
    private final Category category;
    private boolean dragging, opened;
    private final ObjectArrayList<ModuleComponent> moduleComponents = new ObjectArrayList<>();
    public static int i;
    private final EaseInOutQuad openAnimation = new EaseInOutQuad(250, 1);

    public CategoryPanel(Category category) {
        this.category = category;
        this.opened = true;
        this.openAnimation.setDirection(Direction.BACKWARDS);
        for (i = 0; i < (Sakura.MODULE.getModsByCategory(category).size()); ++i) {
            Module module = Sakura.MODULE.getModsByCategory(category).get(i);
            moduleComponents.add(new ModuleComponent(module));
        }
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        update(mouseX, mouseY);

        NanoVGRenderer.INSTANCE.draw(vg -> {
            Color bgColor = ClickGui.backgroundColor.get();
            NanoVGHelper.drawRoundRectBloom(x, y - 2, width, (float) (36 + ((height - 36))), 14, new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 100));
            NanoVGHelper.drawString(category.name(), x + 8, y + 23.5f, FontLoader.greycliffBold(20), 20, new Color(255, 255, 255, 255));
            NanoVGHelper.drawString(category.icon, x + 8 + width - NanoVGHelper.getTextWidth(category.icon, FontLoader.icons(30), 30) - (category == Category.Render ? 14 : 6), y + 26.5f, FontLoader.icons(30), 30, new Color(255, 255, 255, 255));
        });

        float componentOffsetY = 36;

        for (ModuleComponent component : moduleComponents) {
            component.setX(x);
            component.setY(y + componentOffsetY);
            component.setWidth(width);
            if (openAnimation.getOutput() > 0.7f) {
                component.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            componentOffsetY += (float) (component.getHeight() * openAnimation.getOutput());
        }

        height = componentOffsetY + 18;

        IComponent.super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isHovered((int) mouseX, (int) mouseY)) {
            switch (mouseButton) {
                case 0 -> {
                    dragging = true;
                    dragX = (float) (x - mouseX * Sakura.mc.options.getGuiScale().getValue());
                    dragY = (float) (y - mouseY * Sakura.mc.options.getGuiScale().getValue());
                }
                case 1 -> opened = !opened;
            }
            return true;
        }

        boolean handled = false;
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseClicked(mouseX, mouseY, mouseButton)) {
                handled = true;
            }
        }

        return handled || IComponent.super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = false;
        for (ModuleComponent component : moduleComponents) {
            if (component.keyPressed(keyCode, scanCode, modifiers)) {
                handled = true;
            }
        }
        return handled || IComponent.super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean handled = false;
        for (ModuleComponent component : moduleComponents) {
            if (component.charTyped(chr, modifiers)) {
                handled = true;
            }
        }
        return handled || IComponent.super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (state == 0) dragging = false;

        boolean handled = false;
        for (ModuleComponent component : moduleComponents) {
            if (component.mouseReleased(mouseX, mouseY, state)) {
                handled = true;
            }
        }

        return handled || IComponent.super.mouseReleased(mouseX, mouseY, state);
    }

    public void update(int mouseX, int mouseY) {
        this.openAnimation.setDirection(opened ? Direction.FORWARDS : Direction.BACKWARDS);
        if (dragging) {
            x = (mouseX * Sakura.mc.options.getGuiScale().getValue() + dragX);
            y = (mouseY * Sakura.mc.options.getGuiScale().getValue() + dragY);
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return RenderUtils.isHovering(x, y, width, 36, mouseX * Sakura.mc.options.getGuiScale().getValue(), mouseY * Sakura.mc.options.getGuiScale().getValue());
    }

    // Getter methods
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getDragX() {
        return dragX;
    }

    public float getDragY() {
        return dragY;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isOpened() {
        return opened;
    }

    public ObjectArrayList<ModuleComponent> getModuleComponents() {
        return moduleComponents;
    }

    public EaseInOutQuad getOpenAnimation() {
        return openAnimation;
    }

    // Setter methods
    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setDragX(float dragX) {
        this.dragX = dragX;
    }

    public void setDragY(float dragY) {
        this.dragY = dragY;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }
}
