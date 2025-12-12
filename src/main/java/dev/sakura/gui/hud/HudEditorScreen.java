package dev.sakura.gui.hud;

import dev.sakura.Sakura;
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
        float scale = (float) mc.getWindow().getScaleFactor();
        NanoVGRenderer.INSTANCE.draw(canvas -> {
            // 绘制半透明背景
            NanoVGHelper.drawRect(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), new Color(18, 18, 18, 100));

            // 绘制提示文字
            NanoVGHelper.drawCenteredString(
                    "拖拽HUD模块来调整位置 | 按ESC退出",
                    mc.getWindow().getScaledWidth() / 2f * scale,
                    20 * scale,
                    FontLoader.greycliffRegular(14),
                    14 * scale,
                    new Color(255, 255, 255, 200)
            );
        });

        hudPanel.render(context, mouseX, mouseY, delta);

        for (Module module : Sakura.MODULE.getAllModules()) {
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

        for (Module module : Sakura.MODULE.getAllModules()) {
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

        for (Module module : Sakura.MODULE.getAllModules()) {
            if (module instanceof HudModule hud && hud.isEnabled()) {
                hud.mouseReleased((float) mouseX, (float) mouseY, button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        super.close();
        HudEditor hudEditor = Sakura.MODULE.getModule(HudEditor.class);
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
