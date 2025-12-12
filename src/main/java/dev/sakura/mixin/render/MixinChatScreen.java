package dev.sakura.mixin.render;

import dev.sakura.Sakura;
import dev.sakura.events.client.ChatMessageEvent;
import dev.sakura.mixin.accessor.IChatInputSuggestor;
import dev.sakura.mixin.accessor.ISuggestionWindow;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.util.NanoVGHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

import static dev.sakura.Sakura.mc;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Shadow
    protected TextFieldWidget chatField;
    @Shadow
    ChatInputSuggestor chatInputSuggestor;

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderPost(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (chatField == null || Sakura.COMMAND == null) return;
        if (!chatField.getText().startsWith(Sakura.COMMAND.getPrefix())) return;

        float s = (float) mc.getWindow().getScaleFactor();
        var window = chatInputSuggestor != null ? ((IChatInputSuggestor) chatInputSuggestor).getWindow() : null;

        NanoVGRenderer.INSTANCE.draw(vg -> {
            final float PAD = 0.5F;
            final Color SAKURA = new Color(255, 183, 197);

            NanoVGHelper.drawRectOutline((2 - PAD) * s, (mc.getWindow().getScaledHeight() - 14 - PAD) * s,
                    (mc.getWindow().getScaledWidth() - 4 + PAD * 2) * s, (12 + PAD * 2) * s, 2f, SAKURA);

            if (window != null) {
                Rect2i a = ((ISuggestionWindow) window).getArea();
                NanoVGHelper.drawRectOutline((a.getX() - PAD) * s, (a.getY() - PAD) * s,
                        (a.getWidth() + PAD * 2) * s, (a.getHeight() + PAD * 2) * s, 2f, SAKURA);
            }
        });
    }

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void hookSendMessage(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (Sakura.EVENT_BUS.post(new ChatMessageEvent.Client(chatText)).isCancelled()) ci.cancel();
    }
}
