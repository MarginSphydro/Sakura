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
import org.lwjgl.nanovg.NVGPaint;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;

public class ModuleListHud extends HudModule {

    private final NumberValue<Double> animationSpeed = new NumberValue<>("AnimationSpeed", 0.2, 0.05, 0.5, 0.05);
    private final BoolValue showCategory = new BoolValue("ShowCategory", true);
    private final NumberValue<Double> maxWidth = new NumberValue<>("MaxWidth", 150.0, 50.0, 300.0, 5.0);
    private final NumberValue<Double> maxHeight = new NumberValue<>("MaxHeight", 200.0, 50.0, 500.0, 10.0);
    private final BoolValue alignRight = new BoolValue("AlignRight", false);
    private final BoolValue rainbowColor = new BoolValue("RainbowColor", false);
    private final BoolValue hideHudModules = new BoolValue("HideHudModules", false);
    private final NumberValue<Double> itemSpacing = new NumberValue<>("ItemSpacing", 7.0, 0.0, 10.0, 0.5);
    private final NumberValue<Double> hudScale = new NumberValue<>("HudScale", 1.1, 0.5, 2.0, 0.1);

    private final List<ModuleEntry> moduleEntries = new ArrayList<>();
    private float targetWidth = 0;
    private float targetHeight = 0;
    private float currentWidth = 0;
    private float currentHeight = 0;
    private float scrollOffset = 0;

    private int iconImage = -1;

    private final BoolValue showIcon = new BoolValue("ShowIcon", true);
    private final NumberValue<Double> iconSize = new NumberValue<>("IconSize", 26.0, 16.0, 32.0, 2.0);

    private float rotationAngle = 0.0f;
    private long lastUpdateTime = 0;

    private final List<Particle> particles = new ArrayList<>();
    private final BoolValue enableParticles = new BoolValue("Enable Particles", true);
    private final NumberValue<Double> rotationSpeed = new NumberValue<>("Rotation Speed", 1.0, 0.1, 5.0, 0.1);
    private final NumberValue<Integer> particleCount = new NumberValue<>("Particle Count", 10, 0, 50, 1, enableParticles::get);
    private final NumberValue<Double> particleSize = new NumberValue<>("Particle Size", 2.0, 1.0, 5.0, 0.1, enableParticles::get);
    private final NumberValue<Double> particleSpeed = new NumberValue<>("Particle Speed", 1.0, 0.1, 3.0, 0.1, enableParticles::get);


    private static final float PADDING_X = 6f;
    private static final float PADDING_Y = 4f;

    private static final float CATEGORY_ICON_SPACING = 6f;
    private static final Color SUFFIX_COLOR = new Color(180, 180, 180);
    private static final Color BACKGROUND_COLOR = new Color(18, 18, 18, 70);

    private static final String[] ICON_SET = {"U"};
    private static final float ICON_BACKGROUND_WIDTH = 12f;
    private static final float ICON_BACKGROUND_HEIGHT = 12f;

    private static final java.util.Random RANDOM = new java.util.Random();

    public ModuleListHud() {
        super("ModuleList", 10, 10);
        this.currentWidth = 50;
        this.currentHeight = 20;
        this.width = currentWidth;
        this.height = currentHeight;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void renderInGame(DrawContext context) {
        if (isHudEditorOpen()) return;
        this.currentContext = context;
        update();
        ensureWithinScreenBounds();
        NanoVGRenderer.INSTANCE.draw(vg -> renderContent());
    }

    @Override
    public void renderInEditor(DrawContext context, float mouseX, float mouseY) {
        handleDrag(mouseX, mouseY);
        this.currentContext = context;
        update();
        NanoVGRenderer.INSTANCE.draw(vg -> {
            float scaledWidth = currentWidth * hudScale.get().floatValue();
            float scaledHeight = currentHeight * hudScale.get().floatValue();
            NanoVGHelper.drawRect(x, y, scaledWidth, scaledHeight,
                    dragging ? new Color(ClickGui.color(0).getRed(), ClickGui.color(0).getGreen(), ClickGui.color(0).getBlue(), 80) : BACKGROUND_COLOR);

            renderContent();
        });
    }

    public float getRadius() {
        dev.sakura.module.impl.client.HudEditor hudEditor = Managers.MODULE.getModule(dev.sakura.module.impl.client.HudEditor.class);
        if (hudEditor != null) {
            return hudEditor.globalCornerRadius.get().floatValue();
        }
        return 6f;
    }

    @Override
    public void onRenderContent() {
    }

    private boolean isHudEditorOpen() {
        dev.sakura.module.impl.client.HudEditor editor = Managers.MODULE.getModule(dev.sakura.module.impl.client.HudEditor.class);
        return editor != null && editor.isEnabled();
    }

    private void handleDrag(float mouseX, float mouseY) {
        if (!dragging) return;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        if (alignRight.get() && !isHudEditorOpen()) {
            x = sw - (currentWidth * hudScale.get().floatValue());
        } else {
            x = clamp(mouseX - dragX, 0, sw - (currentWidth * hudScale.get().floatValue()));
        }
        y = clamp(mouseY - dragY, 0, sh - (currentHeight * hudScale.get().floatValue()));
        relativeX = x / sw;
        relativeY = y / sh;
    }

    private void update() {
        float oldWidth = currentWidth;
        int oldScreenWidth = mc.getWindow().getScaledWidth();
        int oldScreenHeight = mc.getWindow().getScaledHeight();
        updateModuleList();
        calculateTargetSize();
        float speed = animationSpeed.get().floatValue();
        currentWidth += (targetWidth - currentWidth) * speed;
        currentHeight += (targetHeight - currentHeight) * speed;

        if (alignRight.get() && !isHudEditorOpen()) {
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();

            if (Math.abs(currentWidth - oldWidth) > 0.1f || screenWidth != oldScreenWidth) {
                x = screenWidth - (currentWidth * hudScale.get().floatValue());
                if (x < 0) x = 0;
            }

            if (screenHeight != oldScreenHeight) {
                float scaledHeight = currentHeight * hudScale.get().floatValue();
                if (y + scaledHeight > screenHeight) {
                    y = screenHeight - scaledHeight;
                    if (y < 0) y = 0;
                }
            }
        }

        this.width = currentWidth * hudScale.get().floatValue();
        this.height = currentHeight * hudScale.get().floatValue();
        updateScroll();

        updateRotation();

        updateParticles();

        if (iconImage == -1 && showIcon.get()) {
            loadIcon();
        }
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
                    float scale = hudScale.get().floatValue();
                    float width1 = NanoVGHelper.getTextWidth(displayText1, font, 10 * scale);
                    float width2 = NanoVGHelper.getTextWidth(displayText2, font, 10 * scale);
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

            if (showIcon.get()) {
                float iconRenderSize = iconSize.get().floatValue() * hudScale.get().floatValue();
                targetHeight = (PADDING_Y * 2 + iconRenderSize + 4) * hudScale.get().floatValue();
            }
            return;
        }
        float maxWidthValue = maxWidth.get().floatValue();
        float maxHeightValue = maxHeight.get().floatValue();
        float scale = hudScale.get().floatValue();
        float totalHeight = PADDING_Y * 2 * scale;

        if (showIcon.get()) {
            float iconRenderSize = iconSize.get().floatValue() * scale;
            totalHeight += iconRenderSize + 4 * scale;
        }

        float maxTextWidth = 0;
        int font = FontLoader.medium(10);
        for (ModuleEntry entry : moduleEntries) {
            String text = getDisplayText(entry.module);
            float textWidth = NanoVGHelper.getTextWidth(text, font, 10 * scale);
            if (showCategory.get()) {
                textWidth += (ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING) * scale;
            }
            maxTextWidth = Math.max(maxTextWidth, textWidth);
            totalHeight += (10 + itemSpacing.get().floatValue()) * scale;
        }
        if (!moduleEntries.isEmpty()) {
            totalHeight -= itemSpacing.get().floatValue() * scale;
        }

        if (showIcon.get()) {
            float iconRenderSize = iconSize.get().floatValue() * scale;
            int sakuraFont = FontLoader.bold(18);
            float sakuraTextWidth = NanoVGHelper.getTextWidth("Sakura", sakuraFont, 18 * scale);
            float totalRequiredWidth = iconRenderSize + sakuraTextWidth + 4 * scale;
            maxTextWidth = Math.max(maxTextWidth, totalRequiredWidth);
        }

        targetWidth = Math.min(maxTextWidth + PADDING_X * 2 * scale, maxWidthValue);
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

    private void ensureWithinScreenBounds() {
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float scaledWidth = currentWidth * hudScale.get().floatValue();
        float scaledHeight = currentHeight * hudScale.get().floatValue();

        if (alignRight.get()) {
            x = screenWidth - scaledWidth;
            if (x < 0) x = 0;
        } else {
            if (x < 0) x = 0;
            float rightEdge = x + scaledWidth;
            if (rightEdge > screenWidth) {
                x = screenWidth - scaledWidth;
                if (x < 0) x = 0;
            }
        }

        if (y < 0) y = 0;
        float bottomEdge = y + scaledHeight;
        if (bottomEdge > screenHeight) {
            y = screenHeight - scaledHeight;
            if (y < 0) y = 0;
        }
    }

    private void renderContent() {
        long vg = NanoVGRenderer.INSTANCE.getContext();
        float scale = hudScale.get().floatValue();

        float currentY = y + (PADDING_Y * scale) - (scrollOffset * scale);

        if (showIcon.get() && iconImage != -1) {
            float iconRenderSize = iconSize.get().floatValue() * scale;
            float iconX = alignRight.get() ?
                    x + (currentWidth * scale) - iconRenderSize - (PADDING_X * scale) :
                    x + (PADDING_X * scale);
            float iconY = currentY;

            float centerX = iconX + iconRenderSize / 2;
            float centerY = iconY + iconRenderSize / 2;

            nvgSave(vg);
            nvgTranslate(vg, centerX, centerY);
            nvgRotate(vg, (float) Math.toRadians(rotationAngle));
            nvgTranslate(vg, -iconRenderSize / 2, -iconRenderSize / 2);

            NVGPaint paint = NVGPaint.create();
            nvgImagePattern(vg, 0, 0, iconRenderSize, iconRenderSize, 0, iconImage, 1.0f, paint);
            nvgBeginPath(vg);
            nvgRect(vg, 0, 0, iconRenderSize, iconRenderSize);
            nvgFillPaint(vg, paint);
            nvgFill(vg);

            nvgRestore(vg);

            String sakuraText = "Sakura";
            int font = FontLoader.bold(18);
            float textWidth = NanoVGHelper.getTextWidth(sakuraText, font, 18 * scale);
            float textHeight = NanoVGHelper.getFontHeight(font, 18 * scale);
            float textX = alignRight.get() ?
                    x + (currentWidth * scale) - textWidth - iconRenderSize - (4 * scale) - (PADDING_X * scale) :
                    iconX + iconRenderSize + (4 * scale);
            float textY = currentY + iconRenderSize / 2 + textHeight / 4;

            NanoVGHelper.drawGlowingString(sakuraText, textX, textY, font, 18 * scale, Color.WHITE, 3.0f * scale);

            currentY += iconRenderSize + (4 * scale);
        }

        if (enableParticles.get()) {
            renderParticles();
        }

        int font = FontLoader.medium(10);
        for (ModuleEntry entry : moduleEntries) {
            if (currentY + (10 * scale) < y || currentY > y + (currentHeight * scale)) {
                currentY += ((10 + itemSpacing.get().floatValue()) * scale);
                continue;
            }
            String moduleName = entry.module.getName();
            String suffix = entry.module.getSuffix();
            float moduleNameWidth = NanoVGHelper.getTextWidth(moduleName, font, 10 * scale);
            float suffixWidth = NanoVGHelper.getTextWidth(suffix, font, 10 * scale);
            float textHeight = NanoVGHelper.getFontHeight(font, 10 * scale);
            float totalTextWidth = moduleNameWidth + (suffix.isEmpty() ? 0 : suffixWidth + (2 * scale));
            String categoryIcon = "";
            float iconWidth = 0;
            float iconHeight = 0;
            if (showCategory.get()) {
                int iconFont = FontLoader.icons(10);
                categoryIcon = getRandomCategoryIcon(entry.module);
                iconWidth = NanoVGHelper.getTextWidth(categoryIcon, iconFont, 10 * scale);
                iconHeight = NanoVGHelper.getFontHeight(iconFont, 10 * scale);
                totalTextWidth += (ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING) * scale;
            }
            float itemWidth = totalTextWidth + (PADDING_X * 2 * scale);
            float itemHeight = 10 * scale;
            float itemX = alignRight.get() ? x + (currentWidth * scale) - itemWidth : x;
            float textX;
            float iconBgX = 0;
            float iconX = 0;
            if (alignRight.get() && showCategory.get()) {
                iconBgX = x + (currentWidth * scale) - (PADDING_X * scale) - (ICON_BACKGROUND_WIDTH * scale);
                textX = iconBgX - (CATEGORY_ICON_SPACING * scale) - moduleNameWidth - (suffix.isEmpty() ? 0 : suffixWidth + (2 * scale));
            } else if (!alignRight.get() && showCategory.get()) {
                iconBgX = itemX + (6 * scale);
                textX = iconBgX + (ICON_BACKGROUND_WIDTH * scale) + (CATEGORY_ICON_SPACING * scale);
            } else {
                textX = alignRight.get() ? x + (currentWidth * scale) - (PADDING_X * scale) - moduleNameWidth - (suffix.isEmpty() ? 0 : suffixWidth + (2 * scale)) : itemX + (PADDING_X * scale);
            }
            float textY = currentY + textHeight / 2 + (2 * scale);
            NanoVGHelper.drawRoundRectBloom(
                    alignRight.get() && showCategory.get() ?
                            itemX + (4 * scale) :
                            itemX + (showCategory.get() ? (ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING + 4) * scale : (4 * scale)),
                    currentY - (3 * scale),
                    itemWidth - (showCategory.get() ? (ICON_BACKGROUND_WIDTH + CATEGORY_ICON_SPACING) * scale : 0) - (7 * scale),
                    itemHeight + (3 * scale),
                    getRadius() * scale,
                    BACKGROUND_COLOR
            );
            if (showCategory.get()) {
                NanoVGHelper.drawRoundRectBloom(
                        iconBgX,
                        currentY - (3 * scale),
                        ICON_BACKGROUND_WIDTH * scale,
                        ICON_BACKGROUND_HEIGHT * scale,
                        getRadius() * scale,
                        BACKGROUND_COLOR
                );
                float iconY = currentY + ((ICON_BACKGROUND_HEIGHT * scale) - iconHeight) / 2;
                iconX = iconBgX + ((ICON_BACKGROUND_WIDTH * scale) - iconWidth) / 2;
                int iconFont = FontLoader.icons(10);
                NanoVGHelper.drawGlowingString(categoryIcon, iconX + (0.5f * scale), iconY + (5 * scale), iconFont, 10 * scale, Color.WHITE, 2.0f * scale);
            }
            Color textColor = rainbowColor.get() ?
                    ClickGui.color(0) :
                    Color.WHITE;
            NanoVGHelper.drawString(moduleName, textX, textY, font, 10 * scale, textColor);
            if (!suffix.isEmpty()) {
                float suffixX = textX + moduleNameWidth + (2 * scale);
                NanoVGHelper.drawString(suffix, suffixX, textY, font, 10 * scale, SUFFIX_COLOR);
            }
            currentY += ((10 + itemSpacing.get().floatValue()) * scale);
        }
    }

    private void renderParticles() {
        long vg = NanoVGRenderer.INSTANCE.getContext();

        for (Particle particle : particles) {
            if (particle.isAlive()) {
                Color particleColor = new Color(
                        particle.color.getRed(),
                        particle.color.getGreen(),
                        particle.color.getBlue(),
                        (int) (particle.color.getAlpha() * particle.alpha)
                );

                nvgBeginPath(vg);
                nvgCircle(vg, particle.x, particle.y, particle.size);

                nvgFillColor(vg, NanoVGHelper.nvgColor(particleColor));
                nvgFill(vg);

                nvgBeginPath(vg);
                nvgCircle(vg, particle.x, particle.y, particle.size * 1.5f);
                nvgFillColor(vg, NanoVGHelper.nvgColor(new Color(255, 255, 255, (int) (50 * particle.alpha))));
                nvgFill(vg);
            }
        }
    }

    private void updateRotation() {
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        rotationAngle += (deltaTime * 0.05f * rotationSpeed.get().floatValue()) % 360.0f;
        if (rotationAngle >= 360.0f) {
            rotationAngle -= 360.0f;
        }
    }

    private void updateParticles() {
        if (enableParticles.get()) {
            particles.removeIf(particle -> !particle.isAlive());

            if (particles.size() < particleCount.get()) {
                if (showIcon.get() && iconImage != -1) {
                    float scale = hudScale.get().floatValue();
                    float iconRenderSize = iconSize.get().floatValue() * scale;
                    float iconX = alignRight.get() ?
                            x + (currentWidth * scale) - iconRenderSize - (PADDING_X * scale) :
                            x + (PADDING_X * scale);
                    float iconY = y + (PADDING_Y * scale);

                    for (int i = particles.size(); i < particleCount.get(); i++) {
                        float angle = (float) (Math.random() * Math.PI * 2);
                        float distance = (float) (Math.random() * iconRenderSize * 0.8f);
                        float particleX = iconX + iconRenderSize / 2 + (float) Math.cos(angle) * distance;
                        float particleY = iconY + iconRenderSize / 2 + (float) Math.sin(angle) * distance;

                        Particle newParticle = new Particle(particleX, particleY);
                        newParticle.size = particleSize.get().floatValue();
                        float speed = particleSpeed.get().floatValue();
                        double particleAngle = Math.random() * Math.PI * 2;
                        newParticle.velocityX = (float) (Math.cos(particleAngle) * speed);
                        newParticle.velocityY = (float) (Math.sin(particleAngle) * speed);
                        particles.add(newParticle);
                    }
                }
            }

            for (Particle particle : particles) {
                particle.update();
            }
        }
    }

    private void loadIcon() {
        iconImage = NanoVGHelper.loadTexture("/assets/sakura/icons/icon_32x32.png");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (iconImage != -1) {
            NanoVGHelper.deleteTexture(iconImage);
            iconImage = -1;
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

    private static class Particle {
        public float x, y;
        public float velocityX, velocityY;
        public float size;
        public Color color;
        public float alpha;
        public float life;
        public float maxLife;

        public Particle(float x, float y) {
            this.x = x;
            this.y = y;
            this.size = 1.0f + (float) (Math.random() * 2.0f);
            this.color = ClickGui.mainColor.get();
            this.alpha = 1.0f;
            this.maxLife = 100f + (float) (Math.random() * 100f);
            this.life = maxLife;
            float speed = 0.5f + (float) (Math.random() * 1.5f);
            double angle = Math.random() * Math.PI * 2;
            this.velocityX = (float) (Math.cos(angle) * speed);
            this.velocityY = (float) (Math.sin(angle) * speed);
        }

        public void update() {
            x += velocityX;
            y += velocityY;
            life -= 1.0f;
            alpha = life / maxLife;
        }

        public boolean isAlive() {
            return life > 0;
        }
    }
}