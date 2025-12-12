package dev.sakura.mixin.entity;

import dev.sakura.Sakura;
import dev.sakura.events.entity.SwingSpeedEvent;
import dev.sakura.events.entity.UpdateServerPositionEvent;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.sakura.Sakura.mc;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

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
