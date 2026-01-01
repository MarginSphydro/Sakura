package dev.sakura.client.gui.mainmenu;

import dev.sakura.client.Sakura;
import dev.sakura.client.gui.clickgui.component.ModuleComponent;
import dev.sakura.client.gui.clickgui.panel.CategoryPanel;
import dev.sakura.client.gui.component.AdvancedColorPicker;
import dev.sakura.client.module.Category;
import dev.sakura.client.module.impl.client.ClickGui;
import dev.sakura.client.nanovg.NanoVGRenderer;
import dev.sakura.client.nanovg.font.FontLoader;
import dev.sakura.client.nanovg.util.NanoVGHelper;
import dev.sakura.client.shaders.MainMenuShader;
import dev.sakura.client.utils.TranslationManager;
import dev.sakura.client.utils.render.Shader2DUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.nanovg.NanoVG.nvgCreateImageMem;

public class WelcomeScreen extends Screen {
    private final List<MenuButton> buttons = new ArrayList<>();
    private AdvancedColorPicker mainColorPicker;
    private final List<CategoryPanel> previewPanels = new ArrayList<>();
    private DrawContext currentContext;

    private boolean exiting = false;
    private long exitTime = 0;
    private long initTime;

    private int currentStep = 0;
    private final int totalSteps = 4;
    private boolean transitioningStep = false;
    private long stepTransitionStart = 0;
    private int direction = 1; // 1: 下一个, -1: 上一个
    private float contentScale = 1.0f;

    private int pi8Texture = -1;
    private boolean isImageLoading = false;
    private float imageScrollY = 0;
    private float targetImageScrollY = 0;

    private MenuButton btnPrev, btnNext, btnLanguage, btnColorMode;

    public WelcomeScreen() {
        super(Text.of("Welcome"));
    }

    public DrawContext getDrawContext() {
        return currentContext;
    }

    @Override
    protected void init() {
        if (initTime == 0) {
            initTime = System.currentTimeMillis();
        }

        previewPanels.clear();
        float xOffset = 0;
        for (Category category : Category.values()) {
            CategoryPanel panel = new CategoryPanel(category);
            panel.setX(xOffset);
            panel.setY(0);
            panel.setOpened(true);

            // 预览展开脚手架，甲飞和水晶模型
            for (ModuleComponent component : panel.getModuleComponents()) {
                if (component.getModule().getEnglishName().equalsIgnoreCase("Scaffold") || component.getModule().getEnglishName().equalsIgnoreCase("ArmorFly") || component.getModule().getEnglishName().equalsIgnoreCase("Crystal")) {
                    component.setOpened(true);
                } else if (Math.random() > 0.9) {
                    component.setPreviewEnabled(true);
                }
            }

            previewPanels.add(panel);
            xOffset += panel.getWidth() + 10;
        }

        updateLayout();
    }

    private float getLogicX(float physicalX) {
        return (physicalX - width / 2.0f) / contentScale + width / 2.0f;
    }

    private float getLogicY(float physicalY) {
        return (physicalY - height / 2.0f) / contentScale + height / 2.0f;
    }

    private void updateLayout() {
        this.contentScale = Math.min(1.0f, Math.min((float) width / 850f, (float) height / 550f));

        buttons.clear();
        int centerX = width / 2;
        int centerY = height / 2;
        int bottomY = (int) getLogicY(height - 50);
        int buttonWidth = 100;
        int buttonHeight = 30;
        int spacing = 20;

        btnPrev = new MenuButton(centerX - buttonWidth - spacing / 2, bottomY, buttonWidth, buttonHeight, TranslationManager.get("nav.prev"), () -> {
            if (currentStep > 0 && !transitioningStep) {
                changeStep(currentStep - 1);
            }
        });

        btnNext = new MenuButton(centerX + spacing / 2, bottomY, buttonWidth, buttonHeight, TranslationManager.get("nav.next"), () -> {
            if (currentStep < totalSteps - 1 && !transitioningStep) {
                changeStep(currentStep + 1);
            } else if (currentStep == totalSteps - 1) {
                finishWizard();
            }
        });

        btnLanguage = new MenuButton(centerX - 100, centerY, 200, 30, getLanguageText(), () -> {
            if (ClickGui.language.get() == ClickGui.Language.English) {
                ClickGui.language.set(ClickGui.Language.Chinese);
            } else {
                ClickGui.language.set(ClickGui.Language.English);
            }

            btnLanguage.text = getLanguageText();
            btnPrev.text = TranslationManager.get("nav.prev");
            btnNext.text = currentStep == totalSteps - 1 ? TranslationManager.get("nav.finish") : TranslationManager.get("nav.next");
            if (btnColorMode != null) {
                btnColorMode.text = getColorModeText();
            }
        });

        int pickerWidth = 230;

        int pickerHeight = new AdvancedColorPicker("", ClickGui.mainColor, 0, 0).getHeight();
        int colorButtonHeight = 35;
        int colorSpacing = 20;
        int totalGroupHeight = pickerHeight + colorSpacing + colorButtonHeight;

        int pickerX = centerX - 250 - (pickerWidth / 2);
        int pickerY = centerY - (totalGroupHeight / 2);

        if (mainColorPicker == null) {
            mainColorPicker = new AdvancedColorPicker("theme.main_color", ClickGui.mainColor, pickerX, pickerY);
            mainColorPicker.setSecondColor(ClickGui.secondColor);
        } else {
            mainColorPicker = new AdvancedColorPicker("theme.main_color", ClickGui.mainColor, pickerX, pickerY);
            mainColorPicker.setSecondColor(ClickGui.secondColor);
        }

        btnColorMode = new MenuButton(pickerX, pickerY + pickerHeight + colorSpacing, pickerWidth, colorButtonHeight, getColorModeText(), () -> {
            ClickGui.ColorMode[] modes = ClickGui.ColorMode.values();
            int index = ClickGui.colorMode.get().ordinal();
            int nextIndex = (index + 1) % modes.length;
            ClickGui.colorMode.set(modes[nextIndex]);
            btnColorMode.text = getColorModeText();
        });

        updateButtonsState();

        buttons.add(btnPrev);
        buttons.add(btnNext);

        if (currentStep == 0) buttons.add(btnLanguage);
        if (currentStep == 1) buttons.add(btnColorMode);
    }

    private String getLanguageText() {
        return TranslationManager.get("settings.language") + (ClickGui.language.get() == ClickGui.Language.English ? "English" : "中文");
    }

    private String getColorModeText() {
        String modeName = TranslationManager.get("colormode." + ClickGui.colorMode.get().name().toLowerCase());
        return (ClickGui.language.get() == ClickGui.Language.English ? "Color Mode: " : "颜色模式: ") + modeName;
    }

    private void changeStep(int newStep) {
        direction = newStep > currentStep ? 1 : -1;
        currentStep = newStep;
        transitioningStep = true;
        stepTransitionStart = System.currentTimeMillis();
        updateButtonsState();

        buttons.clear();
        buttons.add(btnPrev);
        buttons.add(btnNext);
        if (currentStep == 0) buttons.add(btnLanguage);
        if (currentStep == 1) buttons.add(btnColorMode);
    }

    private void updateButtonsState() {
        if (btnPrev != null) btnPrev.enabled = currentStep > 0;
        if (btnNext != null)
            btnNext.text = currentStep == totalSteps - 1 ? TranslationManager.get("nav.finish") : TranslationManager.get("nav.next");
    }

    private void finishWizard() {
        ClickGui clickGui = Sakura.MODULES.getModule(ClickGui.class);
        if (clickGui != null) {
            clickGui.setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
            if (Math.random() > 0.5) {
                ClickGui.colorMode.set(ClickGui.ColorMode.values()[(int) (Math.random() * ClickGui.ColorMode.values().length)]);
            }
            Sakura.CONFIG.saveDefaultConfig();
        }
        exiting = true;
        exitTime = System.currentTimeMillis();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentStep == 1 && mainColorPicker != null) {
            if (mainColorPicker.keyPressed(keyCode)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (currentStep == 1 && mainColorPicker != null) {
            if (mainColorPicker.charTyped(chr)) return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.currentContext = context;
        long currentTime = System.currentTimeMillis();

        float fadeInProgress = Math.min(1.0f, (currentTime - initTime) / 800.0f);
        float blurStrength = (float) (1 - Math.pow(1 - fadeInProgress, 3)) * 15.0f;

        float scale = 1.0f;
        float alpha = 1.0f;
        if (exiting) {
            float exitProgress = Math.min(1.0f, (currentTime - exitTime) / 500.0f);
            float easeIn = exitProgress * exitProgress * exitProgress;
            scale = 1.0f + easeIn * 0.5f;
            alpha = 1.0f - easeIn;
            blurStrength *= (1.0f - easeIn);

            if (exitProgress >= 1.0f) {
                client.setScreen(new MainMenuScreen());
                return;
            }
        }

        MainMenuShader.getSharedInstance().render(width, height, 1.0f);

        if (blurStrength > 0.1f) {
            Shader2DUtil.drawQuadBlur(context.getMatrices(), 0, 0, width, height, blurStrength, 1.0f);
        }

        final float finalScale = scale * contentScale;
        final float finalAlpha = alpha;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.translate(vg, width / 2f, height / 2f);
            NanoVGHelper.scale(vg, finalScale, finalScale);
            NanoVGHelper.translate(vg, -width / 2f, -height / 2f);
            NanoVGHelper.globalAlpha(vg, finalAlpha);

            double scaledMouseX = (mouseX - width / 2.0) / contentScale + width / 2.0;
            double scaledMouseY = (mouseY - height / 2.0) / contentScale + height / 2.0;

            renderStep(vg, (int) scaledMouseX, (int) scaledMouseY);

            for (MenuButton button : buttons) {
                if (button == btnLanguage || button == btnColorMode) continue;

                boolean hovered = button.isHovered((int) scaledMouseX, (int) scaledMouseY);
                button.render(hovered, 1.0f, hovered ? 1.05f : 1.0f);
            }

            renderProgressDots(vg);
        });
    }

    private void loadPi8Image() {
        if (pi8Texture != -1 || isImageLoading) return;

        isImageLoading = true;
        CompletableFuture.runAsync(() -> {
            try {
                InputStream is = Sakura.class.getResourceAsStream("/assets/sakura/textures/pi8.jpg");
                if (is == null) {
                    isImageLoading = false;
                    return;
                }

                byte[] bytes = is.readAllBytes();
                is.close();

                java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes);
                buffer.flip();

                client.execute(() -> {
                    long vg = NanoVGRenderer.INSTANCE.getContext();
                    pi8Texture = nvgCreateImageMem(vg, 0, buffer);
                    isImageLoading = false;
                });
            } catch (Exception e) {
                isImageLoading = false;
            }
        });
    }

    @Override
    public void removed() {
        if (pi8Texture != -1) {
            NanoVGHelper.deleteTexture(pi8Texture);
            pi8Texture = -1;
        }
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentStep == 2) {
            targetImageScrollY -= (float) verticalAmount * 40;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderStep(long vg, int mouseX, int mouseY) {
        float animProgress = 0;
        float offset = 0;
        float opacity = 1.0f;

        float targetShaderOffset = currentStep * 0.3f;

        if (transitioningStep) {
            float t = (System.currentTimeMillis() - stepTransitionStart) / 400.0f;
            if (t >= 1.0f) {
                transitioningStep = false;
                t = 1.0f;
            }
            animProgress = 1 - (1 - t) * (1 - t) * (1 - t);
            offset = (1 - animProgress) * 200 * direction;
            opacity = animProgress;

            float prevOffset = (currentStep - direction) * 0.3f;
            float currentShaderOffset = prevOffset + (targetShaderOffset - prevOffset) * animProgress;
            MainMenuShader.getSharedInstance().setMouseOffset(currentShaderOffset * width);
        } else {
            MainMenuShader.getSharedInstance().setMouseOffset(targetShaderOffset * width);
        }

        int centerY = height / 2;
        int centerX = width / 2;

        NanoVGHelper.translate(vg, offset, 0);
        NanoVGHelper.globalAlpha(vg, opacity);

        switch (currentStep) {
            case 0:
                String title = TranslationManager.get("welcome.title");
                float fontSize = 40f;
                int font = FontLoader.bold((int) fontSize);
                float textWidth = NanoVGHelper.getTextWidth(title, font, fontSize);

                NanoVGHelper.drawString(title, (width - textWidth) / 2f + 2, centerY - 100 + 2, font, fontSize, new Color(0, 0, 0, 100));
                NanoVGHelper.drawString(title, (width - textWidth) / 2f, centerY - 100, font, fontSize, Color.WHITE);

                String sub = TranslationManager.get("welcome.subtitle");
                float subSize = 20f;
                int subFont = FontLoader.regular((int) subSize);
                float subWidth = NanoVGHelper.getTextWidth(sub, subFont, subSize);
                NanoVGHelper.drawString(sub, (width - subWidth) / 2f, centerY - 60, subFont, subSize, new Color(220, 220, 220));

                boolean hovered = btnLanguage.isHovered(mouseX, mouseY);
                btnLanguage.render(hovered, 1.0f, hovered ? 1.05f : 1.0f);
                break;

            case 1:
                String settingsText = TranslationManager.get("wizard.step.theme");
                float sFontSize = 24f;
                int sFont = FontLoader.bold((int) sFontSize);
                float sTextWidth = NanoVGHelper.getTextWidth(settingsText, sFont, sFontSize);
                NanoVGHelper.drawString(settingsText, (width - sTextWidth) / 2f, getLogicY(50), sFont, sFontSize, Color.WHITE);

                drawPreviewPanel(centerX + 180, centerY - 160);

                mainColorPicker.render(mouseX, mouseY);

                if (btnColorMode != null) {
                    boolean modeHovered = btnColorMode.isHovered(mouseX, mouseY);
                    btnColorMode.render(modeHovered, 1.0f, modeHovered ? 1.05f : 1.0f);
                }
                break;

            case 2:
                loadPi8Image();

                String readText = "请必须完整阅读";
                int rFont = FontLoader.bold(24);
                float rWidth = NanoVGHelper.getTextWidth(readText, rFont, 24);
                NanoVGHelper.drawString(readText, (width - rWidth) / 2f, getLogicY(30), rFont, 24, new Color(255, 100, 100));

                float viewW = 380;
                if (viewW > width * 0.9f) viewW = width * 0.9f;
                float viewY = getLogicY(60);
                float viewH = getLogicY(height - 110) - viewY;
                float viewX = (width - viewW) / 2f;

                float padding = 6;
                float contentX = viewX + padding;
                float contentY = viewY + padding;
                float contentW = viewW - padding * 2;
                float contentH = viewH - padding * 2;

                NanoVGHelper.drawShadow(viewX, viewY, viewW, viewH, 12, new Color(0, 0, 0, 100), 20, 0, 5);

                NanoVGHelper.drawRoundRect(viewX, viewY, viewW, viewH, 12, new Color(30, 30, 30, 200));

                NanoVGHelper.drawRoundRectOutline(viewX, viewY, viewW, viewH, 12, 1.0f, new Color(255, 255, 255, 30));

                if (isImageLoading) {
                    String loading = "Loading...";
                    NanoVGHelper.drawCenteredString(loading, width / 2f, height / 2f, FontLoader.regular(20), 20, Color.WHITE);
                } else if (pi8Texture != -1) {
                    float imgW = contentW;
                    float imgH = imgW * (7082f / 1440f);

                    float maxScroll = Math.max(0, imgH - contentH);

                    imageScrollY = imageScrollY + (targetImageScrollY - imageScrollY) * 0.1f;

                    if (targetImageScrollY < 0) {
                        targetImageScrollY += (0 - targetImageScrollY) * 0.2f;
                    } else if (targetImageScrollY > maxScroll) {
                        targetImageScrollY += (maxScroll - targetImageScrollY) * 0.2f;
                    }

                    NanoVGHelper.scissor(contentX, contentY, contentW, contentH);
                    NanoVGHelper.drawTexture(pi8Texture, contentX, contentY - imageScrollY, imgW, imgH, 1.0f);
                    NanoVGHelper.resetScissor();

                    if (maxScroll > 0) {
                        float barH = Math.max(20, (contentH / imgH) * contentH);
                        float scrollPercent = Math.max(0, Math.min(1, imageScrollY / maxScroll));
                        float barY = contentY + scrollPercent * (contentH - barH);

                        float barX = viewX + viewW - 10;

                        NanoVGHelper.save();

                        NanoVGHelper.drawRoundRect(barX - 1, barY - 1, 6, barH + 2, 3, new Color(0, 0, 0, 100));

                        NanoVGHelper.drawRoundRect(barX, barY, 4, barH, 2, new Color(255, 255, 255, 200));

                        NanoVGHelper.restore();
                    }
                }
                break;

            case 3:
                String finishText = TranslationManager.get("ready.title");
                float fFontSize = 24f;
                int fFont = FontLoader.bold((int) fFontSize);
                float fTextWidth = NanoVGHelper.getTextWidth(finishText, fFont, fFontSize);
                NanoVGHelper.drawString(finishText, (width - fTextWidth) / 2f, centerY - 20, fFont, fFontSize, Color.WHITE);

                String infoText = TranslationManager.get("ready.info");
                float iFontSize = 16f;
                int iFont = FontLoader.regular((int) iFontSize);
                float iTextWidth = NanoVGHelper.getTextWidth(infoText, iFont, iFontSize);
                NanoVGHelper.drawString(infoText, (width - iTextWidth) / 2f, centerY + 10, iFont, iFontSize, new Color(200, 200, 200));
                break;
        }

        NanoVGHelper.translate(vg, -offset, 0);
    }

    private void drawPreviewPanel(float x, float y) {
        if (previewPanels.isEmpty()) return;

        float scale = 0.80f;
        float totalWidth = 0;
        for (CategoryPanel panel : previewPanels) {
            totalWidth += panel.getWidth() + 10;
        }
        totalWidth -= 10;

        NanoVGHelper.save();
        NanoVGHelper.translate(x - (totalWidth * scale) / 2f, y);
        NanoVGHelper.scale(scale, scale);

        DrawContext context = getDrawContext();
        for (CategoryPanel panel : previewPanels) {
            panel.render(context, -1000, -1000, 0);
        }

        NanoVGHelper.restore();
    }

    private void renderProgressDots(long vg) {
        int centerX = width / 2;
        int bottomY = (int) getLogicY(height - 80);
        int dotSpacing = 20;
        int startX = centerX - ((totalSteps - 1) * dotSpacing) / 2;

        for (int i = 0; i < totalSteps; i++) {
            float x = startX + i * dotSpacing;
            boolean active = i == currentStep;

            Color color = active ? new Color(255, 183, 197) : new Color(150, 150, 150, 100);
            float radius = active ? 4 : 3;

            NanoVGHelper.drawCircle(x, bottomY, radius, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaledMouseX = getLogicX((float) mouseX);
        double scaledMouseY = getLogicY((float) mouseY);

        if (currentStep == 1 && mainColorPicker != null) {
            if (mainColorPicker.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }

        for (MenuButton btn : buttons) {
            if (btn.mouseClicked(scaledMouseX, scaledMouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (currentStep == 1 && mainColorPicker != null) {
            mainColorPicker.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
    }
}
