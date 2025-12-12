package dev.sakura.mixin.entity;

import dev.sakura.Sakura;
import dev.sakura.events.player.MotionEvent;
import dev.sakura.events.player.UpdateEvent;
import dev.sakura.events.type.EventType;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @Author：Gu-Yuemang
 * @Date：12/7/2025 2:11 PM
 */
@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    private MotionEvent motionEvent;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onUpdate(CallbackInfo ci) {
        Sakura.EVENT_BUS.post(new UpdateEvent());
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void sendMovementPacketsHeadInject(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        motionEvent = new MotionEvent(EventType.PRE, player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.isOnGround());
        Sakura.EVENT_BUS.post(motionEvent);
        if (motionEvent.isCancelled()) ci.cancel();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double xPositionSendEventRedirect(ClientPlayerEntity player) {
        return motionEvent.getX();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double yPositionSendEventRedirect(ClientPlayerEntity player) {
        return motionEvent.getY();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double zPositionSendEventRedirect(ClientPlayerEntity player) {
        return motionEvent.getZ();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float yawSendEventRedirect(ClientPlayerEntity player) {
        return motionEvent.getYaw();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float pitchSendEventRedirect(ClientPlayerEntity player) {
        return motionEvent.getPitch();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean onGroundPositionSendEventRedirect(ClientPlayerEntity player) {
        return motionEvent.isOnGround();
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void sendMovementPacketsTailInject(CallbackInfo info) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        Sakura.EVENT_BUS.post(new MotionEvent(EventType.POST, player.getYaw(), player.getPitch()));
    }
}
