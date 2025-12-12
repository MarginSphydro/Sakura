package dev.sakura.mixin.render;

import dev.sakura.shaders.MainMenuShader;
import dev.sakura.shaders.MainMenuShaderType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.AccessibilityOnboardingButtons;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.sakura.Sakura.mc;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Unique
    private static MainMenuShader mainMenuShader;

    @Unique
    private ButtonWidget shaderButton;

    @Shadow
    @Nullable
    private SplashTextRenderer splashText;

    @Shadow
    @Final
    private static Text COPYRIGHT;

    @Shadow
    protected abstract int addDemoWidgets(int y, int spacingY);

    @Shadow
    protected abstract int addNormalWidgets(int y, int spacingY);

    @Shadow
    protected abstract int addDevelopmentWidgets(int y, int spacingY);

    @Shadow
    @Nullable
    private RealmsNotificationsScreen realmsNotificationGui;

    @Shadow
    protected abstract boolean isRealmsNotificationsGuiDisplayed();

/*    @Shadow
    private long backgroundFadeStart;

    @Shadow
    private boolean doBackgroundFade;*/

    public MixinTitleScreen(Text title) {
        super(title);
    }

/*    @Inject(method = "render", at = @At("TAIL"))
    public void hookRender(final DrawContext context, final int mouseX, final int mouseY, final float delta, final CallbackInfo info) {
        float f = this.doBackgroundFade ? (float) (Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 1000.0f : 1.0f;
        float g = this.doBackgroundFade ? MathHelper.clamp(f - 1.0f, 0.0f, 1.0f) : 1.0f;
        int i = MathHelper.ceil(g * 255.0f) << 24;
        if ((i & 0xFC000000) == 0) {
            return;
        }
        context.drawTextWithShadow(client.textRenderer, String.format("%s %s (%s%s%s)",
                        Alisa.MOD_NAME, Alisa.MOD_VER, BuildConfig.BUILD_IDENTIFIER, "", ""),
                2, height - (client.textRenderer.fontHeight * 2) - 2, 0xffffff | i);
    }*/

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    public void renderPanoramaBackgroundHook(DrawContext context, float delta, CallbackInfo ci) {
        if (mc.world == null) {
            if (mainMenuShader == null) {
                mainMenuShader = new MainMenuShader(MainMenuShaderType.SAKURA2);
            }
            mainMenuShader.render(this.width, this.height);
            ci.cancel();
        } else if (mainMenuShader != null) {
            mainMenuShader.cleanup();
            mainMenuShader = null;
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I", ordinal = 0), cancellable = true)
    private void hookrender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ci.cancel();

    }

    @Inject(method = "init", at = @At(value = "HEAD"), cancellable = true)
    private void hookInit(CallbackInfo ci) {
        ci.cancel();
        if (this.splashText == null) {
            this.splashText = this.client.getSplashTextLoader().get();
        }
        int i = this.textRenderer.getWidth(COPYRIGHT);
        int j = this.width - i - 2;
        int k = 24;
        int l = this.height / 4 + 48;
        if (this.client.isDemo()) {
            l = this.addDemoWidgets(l, 24);
        } else {
            l = this.addNormalWidgets(l, 24);
        }
        l = this.addDevelopmentWidgets(l, 24);
        TextIconButtonWidget textIconButtonWidget = this.addDrawableChild(AccessibilityOnboardingButtons.createLanguageButton(20, button -> this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager())), true));
        int var10001 = this.width / 2 - 124;
        l += 36;
        textIconButtonWidget.setPosition(var10001, l);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.options"), button -> this.client.setScreen(new OptionsScreen(this, this.client.options))).dimensions(this.width / 2 - 100, l, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.quit"), button -> this.client.scheduleStop()).dimensions(this.width / 2 + 2, l, 98, 20).build());
        TextIconButtonWidget textIconButtonWidget2 = this.addDrawableChild(AccessibilityOnboardingButtons.createAccessibilityButton(20, button -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)), true));
        textIconButtonWidget2.setPosition(this.width / 2 + 104, l);

        // 着色器切换按钮（左键下一个，右键上一个）
        this.shaderButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("背景: " + (mainMenuShader != null ? mainMenuShader.getCurrentShaderType().getDisplayName() : MainMenuShaderType.SAKURA2.getDisplayName())),
                        button -> {
                            if (mainMenuShader != null) {
                                mainMenuShader.nextShader();
                                button.setMessage(Text.literal("背景: " + mainMenuShader.getCurrentShaderType().getDisplayName()));
                            }
                        }
                ).dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build()
        );


        if (this.realmsNotificationGui == null) {
            this.realmsNotificationGui = new RealmsNotificationsScreen();
        }
        if (this.isRealmsNotificationsGuiDisplayed()) {
            this.realmsNotificationGui.init(this.client, this.width, this.height);
        }
    }

    @Inject(method = "addNormalWidgets", at = @At(
            target = "Lnet/minecraft/client/gui/screen/TitleScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;",
            value = "INVOKE", shift = At.Shift.AFTER, ordinal = 2))
    public void hookAddNormalWidgets(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
        // TODO: 实现 AccountSelectorScreen
/*         final ButtonWidget widget = ButtonWidget.builder(Text.of("Account Manager"), (action) -> client.setScreen(new AccountSelectorScreen((Screen) this)))
                 .dimensions(this.width / 2 - 100, y + spacingY * 3, 200, 20)
                 .tooltip(Tooltip.of(Text.of("Allows you to switch your in-game account")))
                 .build();
         widget.active = true;
         addDrawableChild(widget);*/
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.shaderButton != null && this.shaderButton.isMouseOver(mouseX, mouseY)) {
            if (button == 1 && mainMenuShader != null) { // 右键
                mainMenuShader.previousShader();
                this.shaderButton.setMessage(Text.literal("背景: " + mainMenuShader.getCurrentShaderType().getDisplayName()));
                this.shaderButton.playDownSound(this.client.getSoundManager());
                cir.setReturnValue(true);
            }
        }
    }
}
