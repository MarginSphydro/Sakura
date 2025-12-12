package dev.sakura.mixin.network;

import dev.sakura.Sakura;
import dev.sakura.events.client.ChatMessageEvent;
import dev.sakura.events.client.GameJoinEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChatMessage", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendChatMessage(String content, CallbackInfo ci) {
        if (Sakura.EVENT_BUS.post(new ChatMessageEvent.Server(content)).isCancelled()) ci.cancel();
    }

    @Inject(method = "onGameJoin", at = @At(value = "TAIL"))
    private void hookOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        Sakura.EVENT_BUS.post(new GameJoinEvent());
    }
}
