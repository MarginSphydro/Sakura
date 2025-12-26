package dev.sakura.gui.clickgui.component.values;

import dev.sakura.gui.Component;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.render.RenderUtil;
import dev.sakura.values.impl.EnumValue;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 19:42
 */
public class EnumValueComponent extends Component {
    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Color GRAY = new Color(150, 150, 150, 255);
    private static final float FONT_SIZE = 7.5f;

    private final EnumValue<?> setting;

    public EnumValueComponent(EnumValue<?> setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float baseFontSize = (float) ClickGui.getFontSize();
        float fontSize = baseFontSize * 0.75f;
        float offset = 0;
        float heightoff = 0;

        int font = FontLoader.regular(fontSize);
        float fontHeight = NanoVGHelper.getFontHeight(font, fontSize);
        String currentMode = setting.get().name();

        NanoVGRenderer.INSTANCE.draw(vg -> NanoVGHelper.drawString(setting.getDisplayName(), getX(), getY(), font, fontSize, WHITE));

        for (String text : setting.getModeNames()) {
            float off = NanoVGHelper.getTextWidth(text, font, fontSize) + 4 * scale;
            if (offset + off >= (getWidth() - 4 * scale)) {
                offset = 0;
                heightoff += 10 * scale;
            }
            float finalOffset = offset;
            float finalHeightoff = heightoff;
            Color textColor = text.equals(currentMode) ? WHITE : GRAY;
            NanoVGRenderer.INSTANCE.draw(vg -> NanoVGHelper.drawString(text, getX() + finalOffset + 4 * scale, getY() + 6 * scale + finalHeightoff + fontHeight, font, fontSize, textColor));
            offset += off;
        }

        setHeight(27 * scale + heightoff);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float baseFontSize = (float) ClickGui.getFontSize();
        float fontSize = baseFontSize * 0.75f;
        float offset = 0;
        float heightoff = 0;
        int font = FontLoader.regular(fontSize);
        for (String text : setting.getModeNames()) {
            float textWidth = NanoVGHelper.getTextWidth(text, font, fontSize);
            float off = textWidth + 4 * scale;
            if (offset + off >= (getWidth() - 4 * scale)) {
                offset = 0;
                heightoff += 10 * scale;
            }
            if (RenderUtil.isHovering(getX() + offset + 4 * scale, getY() + 6 * scale + heightoff, textWidth, NanoVGHelper.getFontHeight(font, fontSize), (float) mouseX, (float) mouseY) && mouseButton == 0) {
                setting.setMode(text);
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
