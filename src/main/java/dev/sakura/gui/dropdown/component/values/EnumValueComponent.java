package dev.sakura.gui.dropdown.component.values;

import dev.sakura.Sakura;
import dev.sakura.gui.Component;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.render.RenderUtils;
import dev.sakura.values.impl.EnumValue;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

/**
 * @Author：Gu-Yuemang
 * @Date：2025/11/14 19:42
 */
public class EnumValueComponent extends Component {
    private final EnumValue<?> setting;

    public EnumValueComponent(EnumValue<?> setting) {
        this.setting = setting;
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float offset = 0;
        float heightoff = 0;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawString(setting.getName(), getX(), getY(), FontLoader.greycliffRegular(15), 15, new Color(255, 255, 255, 255));
        });

        for (String text : setting.getModeNames()) {
            float off = NanoVGHelper.getTextWidth(text, FontLoader.greycliffRegular(15), 15) + 8;
            if (offset + off >= (getWidth() - 5)) {
                offset = 0;
                heightoff += 20;
            }
            float finalOffset = offset;
            float finalHeightoff = heightoff;
            NanoVGRenderer.INSTANCE.draw(vg -> {
                if (text.equals(setting.get().name())) {
                    NanoVGHelper.drawString(text, getX() + finalOffset + 8, getY() + 12 + finalHeightoff + NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(15), 15), FontLoader.greycliffRegular(15), 15, new Color(255, 255, 255, 255));
                } else {
                    NanoVGHelper.drawString(text, getX() + finalOffset + 8, getY() + 12 + finalHeightoff + NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(15), 15), FontLoader.greycliffRegular(15), 15, new Color(150, 150, 150, 255));
                }
            });

            offset += off;

        }

        setHeight(54 + heightoff);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        float scaledMouseX = (float) (mouseX * Sakura.mc.options.getGuiScale().getValue());
        float scaledMouseY = (float) (mouseY * Sakura.mc.options.getGuiScale().getValue());
        float offset = 0;
        float heightoff = 0;
        for (String text : setting.getModeNames()) {
            float off = NanoVGHelper.getTextWidth(text, FontLoader.greycliffRegular(15), 15) + 8;
            if (offset + off >= (getWidth() - 5)) {
                offset = 0;
                heightoff += 20;
            }
            if (RenderUtils.isHovering(getX() + offset + 8, getY() + 12 + heightoff, NanoVGHelper.getTextWidth(text, FontLoader.greycliffRegular(15), 15), NanoVGHelper.getFontHeight(FontLoader.greycliffRegular(15), 15), scaledMouseX, scaledMouseY) && mouseButton == 0) {
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
