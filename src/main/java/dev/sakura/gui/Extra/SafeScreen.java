package dev.sakura.gui.Extra;

import dev.sakura.Sakura;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.module.impl.extra.Safe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class SafeScreen extends Screen {
    private final SafePanel SafePanel;

    public SafeScreen() {
        super(Text.literal("Safe"));
        this.SafePanel = new SafePanel();
        SafePanel.setX(50);
        SafePanel.setY(20);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        SafePanel.render(context, mouseX, mouseY, delta);
        for (Module module : Sakura.MODULES.getAllModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.renderInEditor(context, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (SafePanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (Module module : Sakura.MODULES.getAllModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                if (hud.mouseClicked((float) mouseX, (float) mouseY, button)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        SafePanel.mouseReleased(mouseX, mouseY, button);

        for (Module module : Sakura.MODULES.getAllModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.mouseReleased(button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SafePanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (SafePanel.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!SafePanel.isDragging()) {
            SafePanel.setY(SafePanel.getY() + (scrollY > 0 ? 15 : -15));
        }
        return true;
    }

    @Override
    public void close() {
        super.close();
        Safe Safe = Sakura.MODULES.getModule(Safe.class);
        if (Safe != null) {
            Safe.setState(false);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public SafePanel getExtraPanel() {
        return SafePanel;
    }
}
