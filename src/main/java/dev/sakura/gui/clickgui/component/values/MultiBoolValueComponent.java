package dev.sakura.gui.clickgui.component.values;

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
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color GRAY = new Color(150, 150, 150);
    private static final float FONT_SIZE = 7.5f;

    private final MultiBoolValue setting;
    private final Map<BoolValue, EaseOutSine> select = new HashMap<>();

    public MultiBoolValueComponent(MultiBoolValue setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float offset = 4;
        float heightoff = 0;
        int font = FontLoader.greycliffRegular(FONT_SIZE);
        float fontHeight = NanoVGHelper.getFontHeight(font, FONT_SIZE);

        NanoVGRenderer.INSTANCE.draw(vg -> NanoVGHelper.drawString(setting.getName(), getX(), getY(), font, FONT_SIZE, WHITE));

        for (BoolValue boolValue : setting.getValues()) {
            float off = NanoVGHelper.getTextWidth(boolValue.getName(), font, FONT_SIZE) + 4;
            if (offset + off >= getWidth() - 4) {
                offset = 4;
                heightoff += 10;
            }
            select.putIfAbsent(boolValue, new EaseOutSine(250, 1));
            EaseOutSine anim = select.get(boolValue);
            anim.setDirection(boolValue.get() ? Direction.FORWARDS : Direction.BACKWARDS);

            float finalOffset = offset;
            float finalHeightoff = heightoff;
            Color textColor = new Color(ColorUtil.interpolateColor2(GRAY, WHITE, anim.getOutput().floatValue()));
            NanoVGRenderer.INSTANCE.draw(vg -> NanoVGHelper.drawString(boolValue.getName(), getX() + finalOffset + 4, getY() + 2 + finalHeightoff + fontHeight, font, FONT_SIZE, textColor));

            offset += off;
        }

        setHeight(23 + heightoff);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float offset = 4;
        float heightoff = 0;
        int font = FontLoader.greycliffRegular(FONT_SIZE);
        float fontHeight = NanoVGHelper.getFontHeight(font, FONT_SIZE);

        for (BoolValue boolValue : setting.getValues()) {
            float textWidth = NanoVGHelper.getTextWidth(boolValue.getName(), font, FONT_SIZE);
            float off = textWidth + 4;
            if (offset + off >= getWidth() - 3) {
                offset = 4;
                heightoff += 10;
            }
            if (RenderUtils.isHovering(getX() + offset, getY() + 1 + heightoff, textWidth, fontHeight, (float) mouseX, (float) mouseY) && mouseButton == 0) {
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
