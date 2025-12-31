package dev.sakura.client.gui.clickgui.component.values;

import dev.sakura.client.gui.Component;
import dev.sakura.client.module.impl.client.ClickGui;
import dev.sakura.client.nanovg.NanoVGRenderer;
import dev.sakura.client.nanovg.font.FontLoader;
import dev.sakura.client.nanovg.util.NanoVGHelper;
import dev.sakura.client.utils.animations.Direction;
import dev.sakura.client.utils.animations.impl.SmoothStepAnimation;
import dev.sakura.client.utils.render.RenderUtil;
import dev.sakura.client.values.impl.BoolValue;
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
        float baseFontSize = (float) ClickGui.getFontSize();
        float scaledHeight = 14 * scale;
        setHeight(scaledHeight);
        this.toggleAnimation.setDirection(setting.get() ? Direction.FORWARDS : Direction.BACKWARDS);
        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawString(setting.getDisplayName(), getX(), getY(), FontLoader.regular(baseFontSize * 0.75f), baseFontSize * 0.75f, Color.WHITE);
            float toggleWidth = 15 * scale;
            float toggleHeight = 8 * scale;
            float toggleRadius = 4 * scale;
            float circleRadius = 3 * scale;
            NanoVGHelper.drawRoundRect(getX() + getWidth() - toggleWidth, getY() - 7 * scale, toggleWidth, toggleHeight, toggleRadius, setting.get() ? ClickGui.color(0).darker() : new Color(70, 70, 70));
            NanoVGHelper.drawCircle(getX() + getWidth() - 11 * scale + 7 * scale * toggleAnimation.getOutput().floatValue(), getY() - 3 * scale, circleRadius, setting.get() ? Color.WHITE : new Color(150, 150, 150));
        });
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float toggleWidth = 15 * scale;
        float toggleHeight = 8 * scale;
        if (RenderUtil.isHovering(getX() + getWidth() - toggleWidth, getY() - 7 * scale, toggleWidth, toggleHeight, (float) mouseX, (float) mouseY) && mouseButton == 0) {
            this.setting.set(!this.setting.get());
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return this.setting.isAvailable();
    }
}
