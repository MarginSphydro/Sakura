package dev.sakura.gui.account;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.Sakura;
import dev.sakura.account.msa.exception.MSAAuthException;
import dev.sakura.account.type.MinecraftAccount;
import dev.sakura.account.type.impl.CrackedAccount;
import dev.sakura.account.type.impl.MicrosoftAccount;
import dev.sakura.gui.component.SakuraButton;
import dev.sakura.gui.component.SakuraTextField;
import dev.sakura.gui.theme.SakuraTheme;
import dev.sakura.manager.Managers;
import dev.sakura.manager.impl.AccountManager;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.utils.render.Shader2DUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;

public final class AccountAddAccountScreen extends Screen {
    private final Screen parent;
    private SakuraTextField email, password;

    public AccountAddAccountScreen(final Screen parent) {
        super(Text.of(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();

        // Layout calculations
        float panelWidth = 300;
        float panelHeight = 260; // Slightly taller for better spacing
        float panelX = (width - panelWidth) / 2;
        float panelY = (height - panelHeight) / 2;

        // Y positions relative to panel top
        float titleY = panelY + 25;
        float emailY = panelY + 60;
        float passwordY = panelY + 94; // 24 height + 10 gap
        float buttonsY = panelY + 140;
        float buttonGap = 8;
        float buttonHeight = 24;

        float inputWidth = 200;
        float inputX = panelX + (panelWidth - inputWidth) / 2;

        // Email Field
        addDrawableChild(email = new SakuraTextField(client.textRenderer, (int) inputX, (int) emailY, (int) inputWidth, 24, Text.of("")));
        email.setPlaceholder("Email or Username...");

        // Password Field
        addDrawableChild(password = new SakuraTextField(client.textRenderer, (int) inputX, (int) passwordY, (int) inputWidth, 24, Text.of("")));
        password.setPlaceholder("Password (Optional)");
        password.setPasswordMode(true);

        // Buttons
        // Add Button
        addDrawableChild(new SakuraButton((int) inputX, (int) buttonsY, (int) inputWidth, (int) buttonHeight, "Add", (action) ->
        {
            final String accountEmail = email.getText();
            if (accountEmail.length() >= 3) {
                final String accountPassword = password.getText();
                MinecraftAccount account;
                if (!accountPassword.isEmpty()) {
                    account = new MicrosoftAccount(accountEmail, accountPassword);
                } else {
                    account = new CrackedAccount(accountEmail);
                }

                Managers.ACCOUNT.register(account);
                client.setScreen(parent);
            }
        }));

        // Browser Button
        addDrawableChild(new SakuraButton((int) inputX, (int) (buttonsY + buttonHeight + buttonGap), (int) inputWidth, (int) buttonHeight, "Browser...", (action) ->
        {
            try {
                AccountManager.MSA_AUTHENTICATOR.loginWithBrowser((token) ->
                        Sakura.EXECUTOR.execute(() ->
                        {
                            final MicrosoftAccount account = new MicrosoftAccount(token);
                            final Session session = account.login();
                            if (session != null) {
                                Managers.ACCOUNT.setSession(session);
                                Managers.ACCOUNT.register(account);
                                client.setScreen(parent);
                            } else {
                                AccountManager.MSA_AUTHENTICATOR.setLoginStage("Could not login to account");
                            }
                        }));
            } catch (IOException | URISyntaxException | MSAAuthException e) {
                e.printStackTrace();
            }
        }));

        // Go Back Button
        addDrawableChild(new SakuraButton((int) inputX, (int) (buttonsY + (buttonHeight + buttonGap) * 2), (int) inputWidth, (int) buttonHeight, "Go Back", (action) -> client.setScreen(parent)));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Shader2DUtil.drawQuadBlur(context.getMatrices(), 0, 0, width, height, 10, 0.5f);

        float panelWidth = 300;
        float panelHeight = 260;
        float panelX = (width - panelWidth) / 2;
        float panelY = (height - panelHeight) / 2;

        // Force Vanilla Render (Backup Layer)
        context.fill((int) panelX, (int) panelY, (int) (panelX + panelWidth), (int) (panelY + panelHeight), 0xE5FFFFFF);
        context.drawBorder((int) panelX, (int) panelY, (int) panelWidth, (int) panelHeight, 0xFFE0E0E0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVG.nvgResetScissor(vg);

            // Shadow
            NVGPaint shadowPaint = NanoVG.nvgBoxGradient(vg, panelX, panelY + 2, panelWidth, panelHeight, SakuraTheme.ROUNDING, 20,
                    SakuraTheme.color(0, 0, 0, 128), SakuraTheme.color(0, 0, 0, 0), NVGPaint.create());
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRect(vg, panelX - 20, panelY - 20, panelWidth + 40, panelHeight + 40);
            NanoVG.nvgFillPaint(vg, shadowPaint);
            NanoVG.nvgFill(vg);

            // Panel Body
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, panelX, panelY, panelWidth, panelHeight, SakuraTheme.ROUNDING);
            NanoVG.nvgFillColor(vg, SakuraTheme.color(SakuraTheme.PANEL_BG));
            NanoVG.nvgFill(vg);

            // Border
            NanoVG.nvgStrokeColor(vg, SakuraTheme.color(SakuraTheme.PRIMARY, 0.3f));
            NanoVG.nvgStrokeWidth(vg, 1.0f);
            NanoVG.nvgStroke(vg);

            // Validation status (Tiny indicator)
            Color statusColor = email.getText().length() >= 3 ? Color.green : Color.red;
            // Draw a small dot or icon left of email field instead of text asterisk if possible, or just the asterisk
            // Using existing logic but cleaned up position
            float inputX = email.getX();
            NanoVGHelper.drawText("*", inputX - 10, email.getY() + (email.getHeight() / 2f), FontLoader.regular(18), 18, NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_MIDDLE, statusColor);

            // Title
            NanoVGHelper.drawText("Add Account", width / 2f, panelY + 25, FontLoader.regular(24), 24, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE, Color.BLACK);
        });

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
