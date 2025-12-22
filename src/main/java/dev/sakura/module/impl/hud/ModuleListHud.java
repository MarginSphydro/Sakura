package dev.sakura.module.impl.hud;

import dev.sakura.manager.Managers;
import dev.sakura.module.HudModule;
import dev.sakura.module.Module;
import dev.sakura.module.impl.client.ClickGui;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import net.minecraft.client.gui.DrawContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleListHud extends HudModule {

    private final NumberValue<Double> animationSpeed = new NumberValue<>("AnimationSpeed", 0.2, 0.05, 0.5, 0.05);
    private final BoolValue showCategory = new BoolValue("ShowCategory", true);
    private final NumberValue<Double> maxWidth = new NumberValue<>("MaxWidth", 150.0, 50.0, 300.0, 5.0);
    private final NumberValue<Double> maxHeight = new NumberValue<>("MaxHeight", 200.0, 50.0, 500.0, 10.0);
    private final BoolValue alignRight = new BoolValue("AlignRight", false);
    private final BoolValue rainbowColor = new BoolValue("RainbowColor", false);
    private final BoolValue hideHudModules = new BoolValue("HideHudModules", false);
    private final NumberValue<Double> itemSpacing = new NumberValue<>("ItemSpacing", 2.0, 0.0, 10.0, 0.5);
    private final NumberValue<Double> hudScale = new NumberValue<>("HudScale", 1.0, 0.5, 2.0, 0.1);

    private final List<ModuleEntry> moduleEntries = new ArrayList<>();
    private float targetWidth = 0;
    private float targetHeight = 0;
    private float currentWidth = 0;
    private float currentHeight = 0;
    private float scrollOffset = 0;

    private static final float PADDING_X = 6f;
    private static final float PADDING_Y = 4f;

    private static final float CATEGORY_ICON_SPACING = 6f;
    private static final Color SUFFIX_COLOR = new Color(180, 180, 180);
    private static final Color BACKGROUND_COLOR = new Color(18, 18, 18, 70);
    
    private static final String[] ICON_SET = {"R", "D", "V", "W", "X", "O", "Z"};
    private static final float ICON_BACKGROUND_WIDTH = 12f;
    private static final float ICON_BACKGROUND_HEIGHT = 12f;
    
    private static final java.util.Random RANDOM = new java.util.Random();

    public ModuleListHud() {
        super("ModuleList", 10, 10);
        this.currentWidth = 50;
        this.currentHeight = 20;
        float scale = hudScale.get().floatValue();
        this.width = currentWidth * scale;
        this.height = currentHeight * scale;
    }
    @Override
    public void renderInGame(DrawContext context) {
        if (isHudEditorOpen()) return;
        this.currentContext = context;
        update();
        NanoVGRenderer.INSTANCE.draw(vg -> renderContent());
    }
    @Override
    public void renderInEditor(DrawContext context, float mouseX, float mouseY) {
        handleDrag(mouseX, mouseY);
        this.currentContext = context;
        update();
        NanoVGRenderer.INSTANCE.draw(vg -> {
            renderContent();
            float scale = hudScale.get().floatValue();
            NanoVGHelper.drawRect(x, y, currentWidth * scale, currentHeight * scale,
                    dragging ? new Color(ClickGui.color(0).getRed(), ClickGui.color(0).getGreen(), ClickGui.color(0).getBlue(), 80) : BACKGROUND_COLOR);
        });
    }
    @Override
    public void onRenderContent() {
        // 实际渲染在renderContent方法中完成
    }
    private boolean isHudEditorOpen() {
        dev.sakura.module.impl.client.HudEditor editor = Managers.MODULE.getModule(dev.sakura.module.impl.client.HudEditor.class);
        return editor != null && editor.isEnabled();
    }

    private void handleDrag(float mouseX, float mouseY) {
        if (!dragging) return;
        float scale = hudScale.get().floatValue();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        x = clamp(mouseX - dragX, 0, sw - currentWidth * scale);
        y = clamp(mouseY - dragY, 0, sh - currentHeight * scale);
        relativeX = x / sw;
        relativeY = y / sh;
    }
    private void update() {
        updateModuleList();
        calculateTargetSize();
        float speed = animationSpeed.get().floatValue();
        currentWidth += (targetWidth - currentWidth) * speed;
        currentHeight += (targetHeight - currentHeight) * speed;
        float scale = hudScale.get().floatValue();
        this.width = currentWidth * scale;
        this.height = currentHeight * scale;
        updateScroll();
    }
    private final java.util.Map<Module, String> moduleIconMap = new java.util.HashMap<>();
    private void updateModuleList() {
        moduleEntries.clear();
        List<Module> enabledModules = Managers.MODULE.getAllModules().stream()
                .filter(Module::isEnabled)
                .filter(module -> !module.isHidden())
                .filter(module -> !hideHudModules.get() || !(module instanceof HudModule))
                .sorted((m1, m2) -> {
                    String displayText1 = getDisplayText(m1);
                    String displayText2 = getDisplayText(m2);
                    int font = FontLoader.medium(10);
                    float width1 = NanoVGHelper.getTextWidth(displayText1, font, 10);
                    float width2 = NanoVGHelper.getTextWidth(displayText2, font, 10);
                    return Float.compare(width2, width1);
                })
                .toList();
        java.util.Iterator<java.util.Map.Entry<Module, String>> iterator = moduleIconMap.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Module, String> entry = iterator.next();
            Module module = entry.getKey();
            if (!enabledModules.contains(module)) {
                iterator.remove();
            }
        }
        for (Module module : enabledModules) {
            moduleEntries.add(new ModuleEntry(module));
            if (!moduleIconMap.containsKey(module)) {
                String icon = ICON_SET[RANDOM.nextInt(ICON_SET.length)];
                moduleIconMap.put(module, icon);
            }
        }
    }
    private void calculateTargetSize() {
        if (moduleEntries.isEmpty()) {
            targetWidth = 50;
            targetHeight = 20;
            return;
        }
        float maxWidthValue = maxWidth.get().floatValue();
        float maxHeightValue = maxHeight.get().floatValue();
        float totalHeight = PADDING_Y * 2;
        float maxTextWidth = 0;
        int font = FontLoader.medium(10);
        for (ModuleEntry entry : moduleEntries) {
            String text = getDisplayText(entry.module);
            float textWidth = NanoVGHelper.getTextWidth(text, font, 10);
            if (showCategory.get()) {
                textWidth += ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING;
            }
            maxTextWidth = Math.max(maxTextWidth, textWidth);
            totalHeight += 10 + itemSpacing.get().floatValue();
        }
        if (!moduleEntries.isEmpty()) {
            totalHeight -= itemSpacing.get().floatValue();
        }
        targetWidth = Math.min(maxTextWidth + PADDING_X * 2, maxWidthValue);
        targetHeight = Math.min(totalHeight, maxHeightValue);
    }
    private void updateScroll() {
        float maxHeightValue = maxHeight.get().floatValue();
        if (targetHeight > maxHeightValue) {
            scrollOffset = Math.max(0, scrollOffset);
        } else {
            scrollOffset = 0;
        }
    }
    private void renderContent() {
        float scale = hudScale.get().floatValue();
        float currentY = y + PADDING_Y * scale - scrollOffset;
        int font = FontLoader.medium(10);
        for (ModuleEntry entry : moduleEntries) {
            if (currentY + 10 * scale < y || currentY > y + currentHeight * scale) {
                currentY += (10 + itemSpacing.get().floatValue()) * scale;
                continue;
            }
            String moduleName = entry.module.getName();
            String suffix = entry.module.getSuffix();
            float moduleNameWidth = NanoVGHelper.getTextWidth(moduleName, font, 10);
            float suffixWidth = NanoVGHelper.getTextWidth(suffix, font, 10);
            float textHeight = NanoVGHelper.getFontHeight(font, 10);
            float totalTextWidth = moduleNameWidth + (suffix.isEmpty() ? 0 : suffixWidth + 2);
            String categoryIcon = "";
            float iconWidth = 0;
            float iconHeight = 0;
            if (showCategory.get()) {
                float iconFontSize = 10 * scale;
                int iconFont = FontLoader.icons(iconFontSize);
                categoryIcon = getRandomCategoryIcon(entry.module);
                iconWidth = NanoVGHelper.getTextWidth(categoryIcon, iconFont, iconFontSize);
                iconHeight = NanoVGHelper.getFontHeight(iconFont, iconFontSize);
                totalTextWidth += ICON_BACKGROUND_WIDTH * scale + CATEGORY_ICON_SPACING * scale;
            }
            float itemWidth = (totalTextWidth + PADDING_X * 2) * scale;
            float itemHeight = 10 * scale;
            float itemX = alignRight.get() ? x + currentWidth * scale - itemWidth : x;
            float textX;
            float iconBgX = 0;
            float iconX = 0;
            if (alignRight.get()) {
                if (showCategory.get()) {
                    iconBgX = x + currentWidth * scale - (PADDING_X * scale) - ICON_BACKGROUND_WIDTH * scale;
                    textX = iconBgX - (CATEGORY_ICON_SPACING * scale) - (moduleNameWidth * scale) - (suffix.isEmpty() ? 0 : (suffixWidth * scale + 2 * scale));
                } else {
                    textX = x + currentWidth * scale - (PADDING_X * scale) - (moduleNameWidth * scale) - (suffix.isEmpty() ? 0 : (suffixWidth * scale + 2 * scale));
                }
            } else {
                if (showCategory.get()) {
                    iconBgX = itemX + 6;
                    textX = iconBgX + (ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING) * scale;
                } else {
                    textX = itemX + (PADDING_X * scale);
                }
            }
            float textY = currentY + (textHeight * scale) / 2 + 2 * scale;
            NanoVGHelper.drawRoundRectBloom(
                alignRight.get() && showCategory.get() ?
                    itemX + 4 :
                    itemX + (showCategory.get() ? (ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING) * scale + 4 : 4),
                currentY - 3,
                itemWidth - (showCategory.get() ? ICON_BACKGROUND_WIDTH * scale + CATEGORY_ICON_SPACING * scale : 0) - 7,
                itemHeight + 3,
                2 * scale,
                BACKGROUND_COLOR
            );
            if (showCategory.get()) {
                NanoVGHelper.drawRoundRectBloom(
                    iconBgX,
                    currentY - 3,
                    ICON_BACKGROUND_WIDTH * scale,
                    ICON_BACKGROUND_HEIGHT * scale,
                    2 * scale,
                    BACKGROUND_COLOR
                );
                float iconY = currentY + (ICON_BACKGROUND_HEIGHT * scale - iconHeight) / 2;
                iconX = iconBgX + (ICON_BACKGROUND_WIDTH * scale - iconWidth) / 2;
                float iconFontSize = 10 * scale;
                int iconFont = FontLoader.icons(iconFontSize);
                NanoVGHelper.drawGlowingString(categoryIcon, iconX+2, iconY+5, iconFont, iconFontSize, Color.WHITE, 2.0f);
            }
            Color textColor = rainbowColor.get() ?
                ClickGui.color(0) :
                Color.WHITE;
            NanoVGHelper.drawString(moduleName, textX, textY, font, 10 * scale, textColor);
            if (!suffix.isEmpty()) {
                float suffixX = textX + moduleNameWidth * scale + 2 * scale;
                NanoVGHelper.drawString(suffix, suffixX, textY, font, 10 * scale, SUFFIX_COLOR);
            }
            currentY += (10 + itemSpacing.get().floatValue()) * scale;
        }
    }

    private String getDisplayText(Module module) {
        String name = module.getName();
        String suffix = module.getSuffix();
        return name + (suffix.isEmpty() ? "" : " " + suffix);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
    private String getRandomCategoryIcon(Module module) {
        if (!moduleIconMap.containsKey(module)) {
            String icon = ICON_SET[RANDOM.nextInt(ICON_SET.length)];
            moduleIconMap.put(module, icon);
        }
        return moduleIconMap.get(module);
    }
    private static class ModuleEntry {
        final Module module;
        ModuleEntry(Module module) {
            this.module = module;
        }
    }
}