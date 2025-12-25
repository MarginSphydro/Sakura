package dev.sakura.module.impl.movement.velocity;

import dev.sakura.events.packet.PacketEvent;
import dev.sakura.mixin.accessor.IEntityVelocityUpdateS2CPacket;
import dev.sakura.mixin.accessor.IExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;

import java.util.Optional;

import static dev.sakura.Sakura.mc;

public class VelocityCancel {
    public static void onPacket(Velocity velocity, PacketEvent event) {
        if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet && packet.getEntityId() == mc.player.getId()) {
            ((IEntityVelocityUpdateS2CPacket) packet).setVelocityX(0);
            ((IEntityVelocityUpdateS2CPacket) packet).setVelocityY(0);
            ((IEntityVelocityUpdateS2CPacket) packet).setVelocityZ(0);
        } else if (event.getPacket() instanceof ExplosionS2CPacket packet) {
            ((IExplosionS2CPacket) (Object) packet).setPlayerKnockback(Optional.empty());
        }
    }
}
