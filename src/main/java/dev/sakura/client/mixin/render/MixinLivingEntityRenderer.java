package dev.sakura.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sakura.client.manager.impl.RotationManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.sakura.client.Sakura.mc;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    private float hookBodyYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != mc.player) {
            return original;
        }

        if (RotationManager.isActive()) {
            return MathHelper.lerp(tickDelta, RotationManager.getPrevRenderYawOffset(), RotationManager.getRenderYawOffset());
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    private float hookHeadYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != mc.player) {
            return original;
        }

        if (RotationManager.isActive()) {
            return MathHelper.lerpAngleDegrees(tickDelta, RotationManager.getPrevRotationYawHead(), RotationManager.getRotationYawHead());
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float hookPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != mc.player) {
            return original;
        }

        if (RotationManager.isActive()) {
            return MathHelper.lerp(tickDelta, RotationManager.getPrevRenderPitch(), RotationManager.getRenderPitch());
        }

        return original;
    }
}
