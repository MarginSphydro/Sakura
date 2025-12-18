package dev.sakura.module.impl.client;

import dev.sakura.Sakura;
import dev.sakura.gui.clickgui.ClickGuiScreen;
import dev.sakura.gui.hud.HudEditorScreen;
import dev.sakura.manager.Managers;
import dev.sakura.module.Category;
import dev.sakura.module.Module;

public class HudEditor extends Module {

    public HudEditor() {
        super("HudEditor", Category.Client);
    }

    @Override
    protected void onEnable() {
        if (mc.currentScreen instanceof ClickGuiScreen) {
            mc.currentScreen.close();
        }

        if (mc.player != null && !(mc.currentScreen instanceof HudEditorScreen)) {
            mc.setScreen(Sakura.HUDEDITOR);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.currentScreen instanceof HudEditorScreen) {
            mc.setScreen(null);
        }

        Managers.CONFIG.saveDefaultConfig();
    }
}
