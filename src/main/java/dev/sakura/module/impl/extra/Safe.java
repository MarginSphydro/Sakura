package dev.sakura.module.impl.extra;

import dev.sakura.Sakura;
import dev.sakura.gui.clickgui.ClickGuiScreen;
import dev.sakura.gui.hud.HudEditorScreen;
import dev.sakura.module.Category;
import dev.sakura.module.Module;

public class Safe extends Module {

    public Safe() {
        super("Safe", "安全功能", Category.Extra);
    }

    @Override
    protected void onEnable() {
        if (mc.currentScreen instanceof ClickGuiScreen) {
            mc.currentScreen.close();
        }

        if (mc.player != null && !(mc.currentScreen instanceof HudEditorScreen)) {
            mc.setScreen(Sakura.SAFE);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.currentScreen instanceof HudEditorScreen) {
            mc.setScreen(null);
        }

        Sakura.CONFIG.saveDefaultConfig();
    }
}
