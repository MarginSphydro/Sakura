package dev.sakura.utils.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class RotationUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public float rotationYaw;
    public float rotationPitch;

    public RotationUtil() {
        if (mc.player != null) {
            this.rotationYaw = mc.player.getYaw();
            this.rotationPitch = mc.player.getPitch();
        }
    }

    public void snapAt(float yaw, float pitch) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public void updateRotations() {
        if (mc.player != null) {
            this.rotationYaw = mc.player.getYaw();
            this.rotationPitch = mc.player.getPitch();
        }
    }
}