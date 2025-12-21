package dev.sakura.gui.mainmenu;

import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.MainMenuShader;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.shaders.SplashShader;
import dev.sakura.shaders.WindowResizeCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dev.sakura.Sakura.mc;

public class MainMenuScreen extends Screen {
    private final List<MenuButton> buttons = new ArrayList<>();
    private int iconImage = -1;
    private int iconWidth, iconHeight;

    private long initTime = 0;
    private static final long FADE_DURATION = 800;
    private static final long BUTTON_STAGGER = 80;

    private static final int BASE_WIDTH = 1280;
    private static final int BASE_HEIGHT = 720;
    private static final float MIN_SCALE = 0.8f;

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
        if (initTime == 0) initTime = System.currentTimeMillis();

        WindowResizeCallback.EVENT.register(this::onWindowResized);
        
        updateLayout();
        loadIcon();
    }

    private void onWindowResized(net.minecraft.client.MinecraftClient client, Window window) {
        updateLayout();
    }

    private void updateLayout() {
        buttons.clear();

        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        float scale = Math.min(scaleX, scaleY);
        scale = Math.max(MIN_SCALE, scale);
        int centerX = this.width / 2;
        int startY = this.height / 2 - (int)(40 * scale);
        int buttonWidth = (int)(200 * scale);
        int buttonHeight = (int)(20 * scale);
        int spacing = (int)(24 * scale);

        buttonHeight = Math.max(buttonHeight, 16);
        buttonWidth = Math.max(buttonWidth, 120);

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
                () -> mc.setScreen(new MultiplayerScreen(this)),
                !isMultiplayerDisabled()
        ));

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2, startY + spacing * 2,
                buttonWidth, buttonHeight,
                I18n.translate("menu.online"),
                () -> mc.setScreen(new RealmsMainScreen(this)),
                !isMultiplayerDisabled()
        ));

        int bottomY = startY + spacing * 3 + (int)(4 * scale);

        buttons.add(new MenuButton(
                centerX - buttonWidth / 2 - (int)(2 * scale), bottomY,
                buttonWidth / 2, buttonHeight,
                I18n.translate("menu.options"),
                () -> mc.setScreen(new OptionsScreen(this, mc.options))
        ));

        buttons.add(new MenuButton(
                centerX + (int)(2 * scale), bottomY,
                buttonWidth / 2, buttonHeight,
                I18n.translate("menu.quit"),
                mc::scheduleStop
        ));

        buttons.add(new ShaderButton(
                centerX - buttonWidth / 2, this.height - (int)(30 * scale),
                buttonWidth, buttonHeight
        ));
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
        float transitionProgress = 1.0f;

        try {
            SplashShader splash = SplashShader.getInstance();
            if (splash != null && splash.isTransitionStarted() && !splash.isTransitionComplete()) {
                transitionProgress = splash.getTransitionProgress();
            }
        } catch (Exception ignored) {
        }

        final float finalTransitionProgress = transitionProgress;

        MainMenuShader.getSharedInstance().render(this.width, this.height, finalTransitionProgress);

        renderButtonBlurs(context, finalTransitionProgress);

        NanoVGRenderer.INSTANCE.draw(vg -> {
            renderIcon(finalTransitionProgress);
            renderButtons(mouseX, mouseY, finalTransitionProgress);
            //renderVersionText(finalTransitionProgress);
            //renderCopyright(finalTransitionProgress);
        });
    }

    private void renderButtonBlurs(DrawContext context, float transitionProgress) {
        long elapsed = System.currentTimeMillis() - initTime;

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
            float centerX = button.x + button.width / 2f;
            float centerY = button.y + button.height / 2f;

            float scaledX = centerX - (button.width / 2f) * buttonScale;
            float scaledY = centerY - (button.height / 2f) * buttonScale;
            float scaledW = button.width * buttonScale;
            float scaledH = button.height * buttonScale;

            Shader2DUtils.drawRoundedBlur(context.getMatrices(), scaledX, scaledY, scaledW, scaledH, 4 * buttonScale, new Color(0, 0, 0, 0), 20f, finalAlpha);
        }
    }

    private void renderIcon(float transitionProgress) {
        if (iconImage == -1) return;

        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        float scale = Math.min(scaleX, scaleY);
        scale = Math.max(MIN_SCALE, scale);

        float iconScaleFactor = Math.max(0.4f, scale * 0.7f);
        float displayScale = 0.15f * iconScaleFactor;
        float displayWidth = iconWidth * displayScale;
        float displayHeight = iconHeight * displayScale;

        float x = (width - displayWidth) / 2f;
        float y = height / 5f - displayHeight / 2f;

        float easeT = 1.0f - (float) Math.pow(1.0f - transitionProgress, 3.0);
        float iconScale = 0.5f + easeT * 0.5f;

        NanoVGHelper.drawTexture(iconImage, x, y, displayWidth, displayHeight, transitionProgress, iconScale, iconScale, 0);
    }

    private void renderButtons(int mouseX, int mouseY, float transitionProgress) {
        long elapsed = System.currentTimeMillis() - initTime;

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

            boolean hovered = mouseX >= button.x && mouseX <= button.x + button.width && mouseY >= button.y && mouseY <= button.y + button.height;

            button.render(hovered, finalAlpha, buttonScale);
        }
    }

/*    private void renderVersionText(float transitionProgress) {
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

        int font = FontLoader.regular(12);
        int alpha = (int) (255 * transitionProgress);
        Color color = new Color(255, 255, 255, alpha);

        NanoVGHelper.drawString(version, 2, height - 2, font, 12, color);
    }*/

/*    private void renderCopyright(float transitionProgress) {
        String copyright = "Copyright© Sakura2025.";
        int font = FontLoader.regular(12);
        float textWidth = NanoVGHelper.getTextWidth(copyright, font, 12);

        int alpha = (int) (255 * transitionProgress);
        Color color = new Color(255, 255, 255, alpha);

        NanoVGHelper.drawString(copyright, width - textWidth - 2, height - 2, font, 12, color);
    }*/

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

        void render(boolean hovered, float alpha, float scale) {
            if (alpha <= 0) return;

            float targetHover = hovered && enabled ? 1.0f : 0.0f;
            hoverProgress += (targetHover - hoverProgress) * 0.2f;

            int baseAlpha = (int) (alpha * 255);
            int bgAlpha = enabled ? (int) (baseAlpha * (0.4f + hoverProgress * 0.2f)) : (int) (baseAlpha * 0.2f);

            Color bgColor = enabled ? new Color(255, 200, 220, bgAlpha) : new Color(128, 128, 128, bgAlpha);
            NanoVGHelper.drawRoundRectScaled(x, y, width, height, 4, bgColor, scale);

            int borderAlpha = (int) (baseAlpha * (0.5f + hoverProgress * 0.3f));
            Color borderColor = enabled ? new Color(255, 150, 180, borderAlpha) : new Color(100, 100, 100, borderAlpha);
            NanoVGHelper.drawRoundRectOutlineScaled(x, y, width, height, 4, 1, borderColor, scale);

            float fontSize = Math.max(10f, Math.min(16f, height * 0.7f));
            int font = FontLoader.regular((int)fontSize);
            float textWidth = NanoVGHelper.getTextWidth(text, font, fontSize);
            float textX = x + (width - textWidth) / 2f;
            float textY = y + height / 2f + fontSize/3;

            int textAlpha = enabled ? baseAlpha : (int) (baseAlpha * 0.6f);
            Color textColor = new Color(255, 255, 255, textAlpha);
            NanoVGHelper.drawString(text, textX, textY, font, fontSize, textColor);
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