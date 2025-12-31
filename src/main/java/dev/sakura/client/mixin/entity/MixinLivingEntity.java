package dev.sakura.client.mixin.entity;

import dev.sakura.client.Sakura;
import dev.sakura.client.events.entity.SwingSpeedEvent;
import dev.sakura.client.events.entity.UpdateServerPositionEvent;
import dev.sakura.client.events.player.JumpEvent;
import dev.sakura.client.events.player.TravelEvent;
import dev.sakura.client.events.type.EventType;
import dev.sakura.client.module.impl.movement.ElytraFly;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.sakura.client.Sakura.mc;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Unique
    private boolean previousElytra = false;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            TravelEvent event = new TravelEvent(EventType.PRE, movementInput);
            Sakura.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this == mc.player) {
            TravelEvent event = new TravelEvent(EventType.POST, movementInput);
            Sakura.EVENT_BUS.post(event);
        }
    }

    @Inject(method = "isGliding", at = @At("TAIL"), cancellable = true)
    private void recastOnLand(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this != mc.player) return;
        boolean elytra = cir.getReturnValue();
        if (previousElytra && !elytra && ElytraFly.INSTANCE != null && ElytraFly.INSTANCE.isEnabled() && ElytraFly.INSTANCE.isBounceMode()) {
            cir.setReturnValue(ElytraFly.recastElytra(MinecraftClient.getInstance().player));
        }
        previousElytra = elytra;
    }

    @Redirect(method = "jump", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"))
    private float redirectGetYawInJump(LivingEntity instance) {
        if ((Object) instance == mc.player) {
            JumpEvent event = new JumpEvent(instance.getYaw());
            Sakura.EVENT_BUS.post(event);
            return event.getYaw();
        }
        return instance.getYaw();
    }

    @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
    private void hookGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        SwingSpeedEvent swingSpeedEvent = new SwingSpeedEvent();
        Sakura.EVENT_BUS.post(swingSpeedEvent);
        if (swingSpeedEvent.isCancelled()) {
            if (swingSpeedEvent.getSelfOnly() && ((Object) this != mc.player)) {
                return;
            }
            cir.cancel();
            cir.setReturnValue(swingSpeedEvent.getSwingSpeed());
        }
    }

    @Inject(method = "updateTrackedPositionAndAngles", at = @At(value = "HEAD"))
    private void hookUpdateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps, CallbackInfo ci) {
        UpdateServerPositionEvent updateServerPositionEvent = new UpdateServerPositionEvent((LivingEntity) (Object) this, x, y, z, yaw, pitch);
        Sakura.EVENT_BUS.post(updateServerPositionEvent);
    }
}