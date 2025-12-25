package dev.sakura.module.impl.movement.velocity;

import dev.sakura.events.packet.PacketEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import static dev.sakura.Sakura.mc;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/21 03:55
 * @Filename：Normal
 */
public class VelocityHeypixelReduce {

    public static void onPacket(Velocity velocity, PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            event.cancel();
            velocity.velocityInput = true;

            velocity.velocityX = packet.getVelocityX();
            velocity.velocityY = packet.getVelocityY();
            velocity.velocityZ = packet.getVelocityZ();

        }
    }

    public static void onPreTick(Velocity velocity) {
        if (velocity.velocityInput) {
            HitResult crosshairTarget = mc.crosshairTarget;
            if (crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity target) {
                for (Integer i = 0; i < velocity.attackCount.get(); i++) {
                    if (mc.player.isSprinting()) mc.player.setSprinting(false);
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    velocity.velocityX *= 0.6;
                    velocity.velocityZ *= 0.6;
                }
            }
            mc.player.setVelocity(velocity.velocityX, velocity.velocityY, velocity.velocityZ);
            velocity.velocityInput = false;
        }
    }
}
