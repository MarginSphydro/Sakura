package dev.sakura.gui.hud;

import dev.sakura.manager.Managers;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.HudEditor;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

import static dev.sakura.Sakura.mc;

public class HudEditorScreen extends Screen {
    private final HudPanel hudPanel;
    private float accumulatedScroll = 0;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
        this.hudPanel = new HudPanel();
        hudPanel.setX(50);
        hudPanel.setY(20);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        NanoVGRenderer.INSTANCE.draw(canvas -> NanoVGHelper.drawCenteredString(
                "拖拽HUD模块来调整位置 | 按ESC退出",
                mc.getWindow().getScaledWidth() / 2f,
                20,
                FontLoader.greycliffRegular(14),
                14,
                new Color(255, 255, 255, 200)
        ));

        hudPanel.render(context, mouseX, mouseY, delta);

        for (Module module : Managers.MODULE.getAllModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.renderInEditor(context, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hudPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (Module module : Managers.MODULE.getAllModules()) {
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
        hudPanel.mouseReleased(mouseX, mouseY, button);

        for (Module module : Managers.MODULE.getAllModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.mouseReleased((float) mouseX, (float) mouseY, button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hudPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (hudPanel.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        accumulatedScroll += (float) scrollY;
        if (!hudPanel.isDragging()) {
            hudPanel.setY(hudPanel.getY() + (scrollY > 0 ? 15 : -15));
        }
        return true;
    }

    @Override
    public void close() {
        super.close();
        HudEditor hudEditor = Managers.MODULE.getModule(HudEditor.class);
        if (hudEditor != null) {
            hudEditor.setState(false);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public HudPanel getHudPanel() {
        return hudPanel;
    }
}
