package dev.sakura.gui.dropdown.component.values;

import dev.sakura.Sakura;
import dev.sakura.gui.Component;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.SmoothStepAnimation;
import dev.sakura.utils.render.RenderUtils;
import dev.sakura.values.impl.BoolValue;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 18:49
 */
public class BoolValueComponent extends Component {
    private final BoolValue setting;
    private final SmoothStepAnimation toggleAnimation = new SmoothStepAnimation(175, 1);

    public BoolValueComponent(BoolValue setting) {
        this.setting = setting;
        this.toggleAnimation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        setHeight(36);
        this.toggleAnimation.setDirection(setting.getValue() ? Direction.FORWARDS : Direction.BACKWARDS);
        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawString(setting.getName(), getX(), getY(), FontLoader.greycliffRegular(15), 15, new Color(255, 255, 255, 255));
            NanoVGHelper.drawRoundRect(getX() + getWidth() - 46, getY() - 14, 30, 16, 8, setting.get() ? ClickGui.color(0).darker() : new Color(70, 70, 70));
            NanoVGHelper.drawCircle(getX() + getWidth() - 38 + 14 * (float) toggleAnimation.getOutput().floatValue(), getY() - 6, 7, setting.get() ? new Color(255, 255, 255) : new Color(150, 150, 150));
        });
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float scaledMouseX = (float) (mouseX * Sakura.mc.options.getGuiScale().getValue());
        float scaledMouseY = (float) (mouseY * Sakura.mc.options.getGuiScale().getValue());
        if (RenderUtils.isHovering(getX() + getWidth() - 46, getY() - 14, 30, 16, scaledMouseX, scaledMouseY) && mouseButton == 0) {
            this.setting.set(!this.setting.get());
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return this.setting.isAvailable();
    }
}
