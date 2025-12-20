package dev.sakura.mixin.render;

import dev.sakura.manager.Managers;
import dev.sakura.module.impl.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class MixinParticleManager {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        NoRender noRender = Managers.MODULE.getModule(NoRender.class);
        if (noRender != null && noRender.noExplosionParticles()) {
            if (parameters.getType() == ParticleTypes.EXPLOSION ||
                    parameters.getType() == ParticleTypes.EXPLOSION_EMITTER ||
                    parameters.getType() == ParticleTypes.SMOKE ||
                    parameters.getType() == ParticleTypes.LARGE_SMOKE) {
                cir.setReturnValue(null);
            }
        }
    }
}