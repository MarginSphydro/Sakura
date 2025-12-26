package dev.sakura.module.impl.client;

import dev.sakura.Sakura;
import dev.sakura.gui.clickgui.ClickGuiScreen;
import dev.sakura.gui.hud.HudEditorScreen;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.NumberValue;

public class HudEditor extends Module {

    public final NumberValue<Double> globalCornerRadius = new NumberValue<>("GlobalCornerRadius", 6.0, 0.0, 20.0, 1.0);

    public HudEditor() {
        super("HudEditor", "自定义界面", Category.Client);
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

        Sakura.CONFIG.saveDefaultConfig();
    }
}
