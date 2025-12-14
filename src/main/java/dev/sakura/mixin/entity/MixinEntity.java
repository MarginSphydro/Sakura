package dev.sakura.mixin.entity;

import dev.sakura.Sakura;
import dev.sakura.events.player.MoveEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.sakura.Sakura.mc;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if ((Object) this == mc.player && type == MovementType.SELF) {
            MoveEvent event = new MoveEvent(movement);
            Sakura.EVENT_BUS.post(event);

            if (event.isCancelled()) {
                ci.cancel();
                return;
            }

            if (event.getX() != movement.x || event.getY() != movement.y || event.getZ() != movement.z) {
                ((Entity) (Object) this).move(type, event.getVec());
                ci.cancel();
            }
        }
    }
}