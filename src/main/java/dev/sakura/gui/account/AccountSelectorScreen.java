package dev.sakura.gui.account;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.gui.account.list.AccountEntry;
import dev.sakura.gui.account.list.AccountListWidget;
import dev.sakura.gui.component.SakuraButton;
import dev.sakura.gui.component.SakuraSearchField;
import dev.sakura.gui.theme.SakuraTheme;
import dev.sakura.manager.Managers;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.utils.animations.Animation;
import dev.sakura.utils.animations.Direction;
import dev.sakura.utils.animations.impl.DecelerateAnimation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.nanovg.NanoVG;

public final class AccountSelectorScreen extends Screen {

    private final Screen parent;
    private AccountListWidget accountListWidget;
    private SakuraSearchField searchWidget;

    private final Animation fadeAnim = new DecelerateAnimation(300, 1.0);

    public AccountSelectorScreen(final Screen parent) {
        super(Text.of("Account Selector"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        fadeAnim.setDirection(Direction.FORWARDS);

        int panelWidth = Math.min(600, width - 40);
        int panelHeight = Math.min(400, height - 40);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        clearChildren();

        // Search Widget
        searchWidget = new SakuraSearchField(client.textRenderer, panelX + 20, panelY + 20, panelWidth - 40, 24, Text.of("Search..."));
        searchWidget.setPlaceholderText("Search accounts...");
        searchWidget.setChangedListener(s -> {
            if (accountListWidget != null) {
                accountListWidget.setSearchFilter(s);
            }
        });
        addDrawableChild(searchWidget);

        // List Widget
        int listY = panelY + 55;
        int listHeight = panelHeight - 110;
        accountListWidget = new AccountListWidget(client, panelWidth - 40, listHeight, listY, 30);
        accountListWidget.setX(panelX + 20);
        accountListWidget.populateEntries();
        addDrawableChild(accountListWidget);

        // Buttons
        int buttonWidth = 100;
        int buttonHeight = 24;
        int bottomY = panelY + panelHeight - 35;
        int spacing = 10;

        // Back Button (Bottom Left)
        addDrawableChild(new SakuraButton(panelX + 20, bottomY, 80, buttonHeight, "Back",
                (b) -> client.setScreen(parent)));

        // Encrypt Button (Bottom Right)
        addDrawableChild(new SakuraButton(panelX + panelWidth - 100, bottomY, 80, buttonHeight, "Encrypt",
                (b) -> client.setScreen(new AccountEncryptionScreen(this))));

        // Center Buttons: Add, Login, Delete
        int centerGroupWidth = (buttonWidth * 3) + (spacing * 2);
        int centerStartX = panelX + (panelWidth - centerGroupWidth) / 2;

        addDrawableChild(new SakuraButton(centerStartX, bottomY, buttonWidth, buttonHeight, "Add",
                (b) -> client.setScreen(new AccountAddAccountScreen(this))));

        addDrawableChild(new SakuraButton(centerStartX + buttonWidth + spacing, bottomY, buttonWidth, buttonHeight, "Login",
                (b) -> {
                    AccountEntry entry = accountListWidget.getSelectedOrNull();
                    if (entry != null) {
                        Session session = entry.getAccount().login();
                        if (session != null) Managers.ACCOUNT.setSession(session);
                    }
                }));

        addDrawableChild(new SakuraButton(centerStartX + (buttonWidth + spacing) * 2, bottomY, buttonWidth, buttonHeight, "Delete",
                (b) -> {
                    AccountEntry entry = accountListWidget.getSelectedOrNull();
                    if (entry == null) return;

                    if (InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT)) {
                        Managers.ACCOUNT.unregister(entry.getAccount());
                        accountListWidget.populateEntries();
                        return;
                    }
                    client.setScreen(new ConfirmScreen((value) -> {
                        if (value) Managers.ACCOUNT.unregister(entry.getAccount());
                        client.setScreen(this);
                    }, Text.of("Delete account?"),
                            Text.of("Are you sure you would like to delete " + entry.getAccount().username() + "?"),
                            Text.of("Yes"), Text.of("No")));
                }));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(600, width - 40);
        int panelHeight = Math.min(400, height - 40);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        // Force Vanilla Render (Backup Layer) - Ensures visibility if NanoVG fails
        // Using a slightly transparent white to act as a base
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE5FFFFFF);
        // Draw a simple border
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFFE0E0E0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVG.nvgResetScissor(vg);

            // Shadow
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, panelX + 3, panelY + 3, panelWidth, panelHeight, SakuraTheme.ROUNDING);
            NanoVG.nvgFillColor(vg, SakuraTheme.color(0, 0, 0, 80));
            NanoVG.nvgFill(vg);

            // Panel Body
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, panelX, panelY, panelWidth, panelHeight, SakuraTheme.ROUNDING);
            NanoVG.nvgFillColor(vg, SakuraTheme.color(SakuraTheme.PANEL_BG));
            NanoVG.nvgFill(vg);

            // Border
            NanoVG.nvgBeginPath(vg);
            NanoVG.nvgRoundedRect(vg, panelX, panelY, panelWidth, panelHeight, SakuraTheme.ROUNDING);
            NanoVG.nvgStrokeColor(vg, SakuraTheme.color(0, 0, 0, 50));
            NanoVG.nvgStrokeWidth(vg, 1.5f);
            NanoVG.nvgStroke(vg);
        });

        super.render(context, mouseX, mouseY, delta);
    }
}
