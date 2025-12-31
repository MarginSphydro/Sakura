package dev.sakura.client.module.impl.movement.velocity;

import dev.sakura.client.events.packet.PacketEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/21 03:55
 * @Filename：Normal
 */
public class VelocityNormal {

    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static void onPacket(Velocity velocity, PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            double velocityX = packet.getVelocityX();
            double velocityY = packet.getVelocityY();
            double velocityZ = packet.getVelocityZ();

            if (velocity.motionX.get() != 100 || velocity.motionY.get() != 100 || velocity.motionZ.get() != 100) {
                event.cancel();
                mc.player.setVelocity(velocityX * velocity.motionX.get() / 100, velocityY * velocity.motionY.get() / 100, velocityZ * velocity.motionZ.get() / 100);
            }
        }
    }
}
