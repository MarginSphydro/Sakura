package dev.sakura.gui.mainmenu;

import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.MainMenuShader;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.shaders.SplashShader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.sakura.Sakura.mc;

public class MainMenuScreen extends Screen {
    private final List<MenuButton> buttons = new ArrayList<>();
    private int iconImage = -1;
    private int iconWidth, iconHeight;

    private float accumulatedTime = 0f;
    private long lastFrameTime = System.nanoTime();
    private static final long FADE_DURATION = 800;
    private static final long BUTTON_STAGGER = 80;

    private float globalAlpha = 1.0f;

    public MainMenuScreen() {
        super(Text.of("SakuraMainMenuScreen"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        if (buttons.isEmpty()) {
            accumulatedTime = 0f;
            lastFrameTime = System.nanoTime();
        }
        buttons.clear();

        int centerX = this.width / 2;
        int startY = this.height / 2 + 20;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 24;

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY,
                buttonWidth, buttonHeight,
                I18n.translate("menu.singleplayer"),
                () -> mc.setScreen(new SelectWorldScreen(this))
        ));

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY + spacing,
                buttonWidth, buttonHeight,
                I18n.translate("menu.multiplayer"),
                () -> {
                    Screen screen = mc.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
                    mc.setScreen(screen);
                },
                !isMultiplayerDisabled()
        ));

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY + spacing * 2,
                buttonWidth, buttonHeight,
                I18n.translate("menu.online"),
                () -> mc.setScreen(new RealmsMainScreen(this)),
                !isMultiplayerDisabled()
        ));

        int bottomY = startY + spacing * 3 + 12;

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2 - 2, bottomY,
                buttonWidth / 2, buttonHeight,
                I18n.translate("menu.options"),
                () -> mc.setScreen(new OptionsScreen(this, mc.options))
        ));

        buttons.add(new MenuButton(
                centerX + 2, bottomY,
                buttonWidth / 2, buttonHeight,
                I18n.translate("menu.quit"),
                mc::scheduleStop
        ));

        buttons.add(new ShaderButton(
                centerX - buttonWidth / 2, this.height - 30,
                buttonWidth, buttonHeight
        ));

        loadIcon();
    }

    private void loadIcon() {
        if (iconImage != -1) return;

        iconImage = NanoVGHelper.loadTexture("/assets/sakura/icons/icon.png");
        if (iconImage != -1) {
            iconWidth = 2334;
            iconHeight = 860;
        }
    }

    private boolean isMultiplayerDisabled() {
        if (mc.isMultiplayerEnabled()) {
            return false;
        }
        if (mc.isUsernameBanned()) {
            return true;
        }
        return mc.getMultiplayerBanDetails() != null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = currentTime;

        // 限制最大deltaTime，防止卡顿后动画跳跃
        deltaTime = Math.min(deltaTime, 0.05f);

        accumulatedTime += deltaTime;

        float transitionProgress = 1.0f;
        boolean inTransition = false;

        try {
            SplashShader splash = SplashShader.getInstance();
            if (splash != null && splash.isTransitionStarted() && !splash.isTransitionComplete()) {
                transitionProgress = Math.min(1.0f, accumulatedTime / 2.0f);
                inTransition = true;
            }
        } catch (Exception ignored) {
        }

        if (inTransition) {
            globalAlpha = transitionProgress;
        } else {
            globalAlpha = 1.0f;
        }

        final float finalTransitionProgress = transitionProgress;

        MainMenuShader.getSharedInstance().render(this.width, this.height, finalTransitionProgress);

        renderButtonBlurs(context, finalTransitionProgress);

        final float finalDeltaTime = deltaTime;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            renderIcon(finalTransitionProgress);
            renderButtons(vg, mouseX, mouseY, finalTransitionProgress, finalDeltaTime);
            renderVersionText(vg, finalTransitionProgress);
            renderCopyright(vg, finalTransitionProgress);
        });
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0f;
    }

    private void renderButtonBlurs(DrawContext context, float transitionProgress) {
        long elapsed = (long) (accumulatedTime * 1000);

        float spreadProgress = easeInOutCubic(transitionProgress);
        float centerX = width / 2f;
        float centerY = height / 2f;

        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);

            long staggeredTime = elapsed - (i * BUTTON_STAGGER);
            float fadeProgress = 1.0f;

            if (staggeredTime > 0 && staggeredTime < FADE_DURATION) {
                fadeProgress = staggeredTime / (float) FADE_DURATION;
                fadeProgress = 1.0f - (1.0f - fadeProgress) * (1.0f - fadeProgress);
            } else if (staggeredTime <= 0) {
                fadeProgress = 0.0f;
            }

            float finalAlpha = fadeProgress * transitionProgress;
            if (finalAlpha <= 0) continue;

            float buttonScale = 0.8f + fadeProgress * 0.2f;

            float introScale = 0.2f + 0.8f * spreadProgress;
            float totalScale = buttonScale * introScale;

            float buttonCenterX = button.x + button.width / 2f;
            float buttonCenterY = button.y + button.height / 2f;

            float currentCenterX = centerX + (buttonCenterX - centerX) * spreadProgress;
            float currentCenterY = centerY + (buttonCenterY - centerY) * spreadProgress;

            float scaledW = button.width * totalScale;
            float scaledH = button.height * totalScale;
            float scaledX = currentCenterX - scaledW / 2f;
            float scaledY = currentCenterY - scaledH / 2f;

            Shader2DUtils.drawRoundedBlur(
                    context.getMatrices(),
                    scaledX,
                    scaledY,
                    scaledW,
                    scaledH,
                    4 * totalScale,
                    new Color(0, 0, 0, 0),
                    10f,
                    finalAlpha
            );
        }
    }

    private void renderIcon(float transitionProgress) {
        if (iconImage == -1) return;

        float scale = 0.15f;
        float displayWidth = iconWidth * scale;
        float displayHeight = iconHeight * scale;

        float spreadProgress = easeInOutCubic(transitionProgress);
        float centerX = width / 2f;
        float centerY = height / 2f;

        float targetX = (width - displayWidth) / 2f;
        float targetY = height / 4f - displayHeight / 2f + 10;

        float targetCenterX = targetX + displayWidth / 2f;
        float targetCenterY = targetY + displayHeight / 2f;

        float currentCenterX = centerX + (targetCenterX - centerX) * spreadProgress;
        float currentCenterY = centerY + (targetCenterY - centerY) * spreadProgress;

        float currentX = currentCenterX - displayWidth / 2f;
        float currentY = currentCenterY - displayHeight / 2f;

        float easeT = 1.0f - (float) Math.pow(1.0f - transitionProgress, 3.0);
        float iconScale = (0.5f + easeT * 0.5f) * (0.2f + 0.8f * spreadProgress);

        NanoVGHelper.drawTexture(iconImage, currentX, currentY, displayWidth, displayHeight, transitionProgress, iconScale, iconScale, 0);
    }

    private void renderButtons(long vg, int mouseX, int mouseY, float transitionProgress, float deltaTime) {
        long elapsed = (long) (accumulatedTime * 1000);

        float spreadProgress = easeInOutCubic(transitionProgress);
        float centerX = width / 2f;
        float centerY = height / 2f;

        for (int i = 0; i < buttons.size(); i++) {
            MenuButton button = buttons.get(i);

            long staggeredTime = elapsed - (i * BUTTON_STAGGER);
            float fadeProgress = 1.0f;

            if (staggeredTime > 0 && staggeredTime < FADE_DURATION) {
                fadeProgress = staggeredTime / (float) FADE_DURATION;
                fadeProgress = 1.0f - (1.0f - fadeProgress) * (1.0f - fadeProgress);
            } else if (staggeredTime <= 0) {
                fadeProgress = 0.0f;
            }

            float finalAlpha = fadeProgress * transitionProgress;
            float buttonScale = 0.8f + fadeProgress * 0.2f;

            float introScale = 0.2f + 0.8f * spreadProgress;
            float totalScale = buttonScale * introScale;

            float buttonCenterX = button.x + button.width / 2f;
            float buttonCenterY = button.y + button.height / 2f;

            float currentCenterX = centerX + (buttonCenterX - centerX) * spreadProgress;
            float currentCenterY = centerY + (buttonCenterY - centerY) * spreadProgress;

            float renderX = currentCenterX - button.width / 2f;
            float renderY = currentCenterY - button.height / 2f;

            boolean hovered = mouseX >= button.x && mouseX <= button.x + button.width
                    && mouseY >= button.y && mouseY <= button.y + button.height;

            // 如果正在动画中，暂时禁用hover效果，避免位置错位时的视觉干扰
            if (spreadProgress < 0.95f) hovered = false;

            button.render(vg, renderX, renderY, hovered, finalAlpha, totalScale, deltaTime);
        }
    }

    private void renderVersionText(long vg, float transitionProgress) {
        String version = "Minecraft " + SharedConstants.getGameVersion().getName();
        if (mc.isDemo()) {
            version += " Demo";
        } else {
            String versionType = mc.getVersionType();
            if (!"release".equalsIgnoreCase(versionType)) {
                version += "/" + versionType;
            }
        }

        if (MinecraftClient.getModStatus().isModded()) {
            version += I18n.translate("menu.modded");
        }

        int font = FontLoader.greycliffRegular(12);
        int alpha = (int) (255 * transitionProgress);
        Color color = new Color(255, 255, 255, alpha);

        NanoVGHelper.drawString(version, 2, height - 2, font, 12, color);
    }

    private void renderCopyright(long vg, float transitionProgress) {
        String copyright = I18n.translate("title.credits");
        int font = FontLoader.greycliffRegular(12);
        float textWidth = NanoVGHelper.getTextWidth(copyright, font, 12);

        int alpha = (int) (255 * transitionProgress);
        Color color = new Color(255, 255, 255, alpha);

        NanoVGHelper.drawString(copyright, width - textWidth - 2, height - 2, font, 12, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (MenuButton menuButton : buttons) {
            if (menuButton.isHovered((int) mouseX, (int) mouseY) && menuButton.enabled) {
                if (menuButton instanceof ShaderButton shaderButton) {
                    if (button == 0) {
                        shaderButton.nextShader();
                        return true;
                    } else if (button == 1) {
                        shaderButton.previousShader();
                        return true;
                    }
                } else if (button == 0) {
                    menuButton.onClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        super.removed();
        if (iconImage != -1) {
            NanoVGHelper.deleteTexture(iconImage);
            iconImage = -1;
        }
    }

    private static class MenuButton {
        int x, y, width, height;
        String text;
        Runnable action;
        boolean enabled;

        private float hoverProgress = 0f;

        MenuButton(int x, int y, int width, int height, String text, Runnable action) {
            this(x, y, width, height, text, action, true);
        }

        MenuButton(int x, int y, int width, int height, String text, Runnable action, boolean enabled) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.action = action;
            this.enabled = enabled;
        }

        boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        void onClick() {
            if (enabled && action != null) {
                action.run();
            }
        }

        void render(long vg, float renderX, float renderY, boolean hovered, float alpha, float scale, float deltaTime) {
            if (alpha <= 0) return;

            float targetHover = hovered && enabled ? 1.0f : 0.0f;

            float speed = 15.0f;
            hoverProgress += (targetHover - hoverProgress) * Math.min(1.0f, deltaTime * speed);

            int baseAlpha = (int) (alpha * 255);
            int bgAlpha = enabled ? (int) (baseAlpha * (0.4f + hoverProgress * 0.2f)) : (int) (baseAlpha * 0.2f);

            Color bgColor = enabled ? new Color(255, 200, 220, bgAlpha) : new Color(128, 128, 128, bgAlpha);
            NanoVGHelper.drawRoundRectScaled(renderX, renderY, width, height, 4, bgColor, scale);

            int borderAlpha = (int) (baseAlpha * (0.5f + hoverProgress * 0.3f));
            Color borderColor = enabled ? new Color(255, 150, 180, borderAlpha) : new Color(100, 100, 100, borderAlpha);
            NanoVGHelper.drawRoundRectOutlineScaled(renderX, renderY, width, height, 4, 1, borderColor, scale);

            int font = FontLoader.greycliffRegular(14);
            float textWidth = NanoVGHelper.getTextWidth(text, font, 14);
            float centerX = renderX + width / 2f;
            float centerY = renderY + height / 2f;

            NanoVG.nvgSave(vg);
            NanoVG.nvgTranslate(vg, centerX, centerY);
            NanoVG.nvgScale(vg, scale, scale);
            NanoVG.nvgTranslate(vg, -centerX, -centerY);

            float textX = renderX + (width - textWidth) / 2f;
            float textY = renderY + height / 2f + 5;

            int textAlpha = enabled ? baseAlpha : (int) (baseAlpha * 0.6f);
            Color textColor = new Color(255, 255, 255, textAlpha);
            NanoVGHelper.drawString(text, textX, textY, font, 14, textColor);

            NanoVG.nvgRestore(vg);
        }
    }

    private static class ShaderButton extends MenuButton {
        ShaderButton(int x, int y, int width, int height) {
            super(x, y, width, height, "背景: " + MainMenuShader.getSharedInstance().getCurrentShaderType().getDisplayName(), null, true);
        }

        void nextShader() {
            MainMenuShader.getSharedInstance().nextShader();
            this.text = "背景: " + MainMenuShader.getSharedInstance().getCurrentShaderType().getDisplayName();
        }

        void previousShader() {
            MainMenuShader.getSharedInstance().previousShader();
            this.text = "背景: " + MainMenuShader.getSharedInstance().getCurrentShaderType().getDisplayName();
        }
    }
}
