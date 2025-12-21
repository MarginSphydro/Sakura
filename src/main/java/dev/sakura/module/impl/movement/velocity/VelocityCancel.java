package dev.sakura.module.impl.movement.velocity;

import dev.sakura.events.packet.PacketEvent;
import dev.sakura.module.impl.movement.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

import static dev.sakura.Sakura.mc;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/21 03:55
 * @Filename：Normal
 */
public class VelocityCancel {
    public static void onPacket(Velocity velocity, PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            event.cancel();
        }
    }
}
