package dev.sakura.gui.dropdown.component.values;

import dev.sakura.Sakura;
import dev.sakura.gui.Component;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.EaseOutSine;
import dev.sakura.utils.color.ColorUtil;
import dev.sakura.utils.render.RenderUtils;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.MultiBoolValue;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 19:54
 */
public class MultiBoolValueComponent extends Component {
    private final MultiBoolValue setting;
    private final Map<BoolValue, EaseOutSine> select = new HashMap<>();

    public MultiBoolValueComponent(MultiBoolValue setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float offset = 8;
        float heightoff = 0;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawString(setting.getName(), getX(), getY(), FontLoader.greycliffRegular(15), 15, new Color(255, 255, 255, 255));
        });

        for (BoolValue boolValue : setting.getValues()) {
            float off = NanoVGHelper.getTextWidth(boolValue.getName(), FontLoader.greycliffRegular(15), 15) + 8;
            if (offset + off >= getWidth() - 5) {
                offset = 8;
                heightoff += 20;
            }
            select.putIfAbsent(boolValue, new EaseOutSine(250, 1));
            select.get(boolValue).setDirection(boolValue.get() ? Direction.FORWARDS : Direction.BACKWARDS);

            float finalOffset = offset;
            float finalHeightoff = heightoff;
            NanoVGRenderer.INSTANCE.draw(vg -> {
                NanoVGHelper.drawString(boolValue.getName(), getX() + finalOffset + 8, getY() + 4 + finalHeightoff + NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(15), 15), FontLoader.greycliffRegular(15), 15, new Color(ColorUtil.interpolateColor2(new Color(150, 150, 150), new Color(255, 255, 255), (float) select.get(boolValue).getOutput().floatValue())));
            });

            offset += off;
        }

        setHeight(46 + heightoff);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float scaledMouseX = (float) (mouseX * Sakura.mc.options.getGuiScale().getValue());
        float scaledMouseY = (float) (mouseY * Sakura.mc.options.getGuiScale().getValue());
        float offset = 8;
        float heightoff = 0;
        for (BoolValue boolValue : setting.getValues()) {
            float off = NanoVGHelper.getTextWidth(boolValue.getName(), FontLoader.greycliffRegular(15), 15) + 8;
            if (offset + off >= getWidth() - 5) {
                offset = 8;
                heightoff += 20;
            }
            if (RenderUtils.isHovering(getX() + offset, getY() + 2 + heightoff, NanoVGHelper.getTextWidth(boolValue.getName(), FontLoader.greycliffRegular(15), 15), NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(15), 15), scaledMouseX, scaledMouseY) && mouseButton == 0) {
                boolValue.set(!boolValue.get());
            }
            offset += off;
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean isVisible() {
        return setting.isAvailable();
    }
}
