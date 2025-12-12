package dev.sakura.utils.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class CombatUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void attackCrystal(BlockPos pos, boolean rotate, boolean swing) {
        if (mc.world == null || mc.player == null) return;
        Box box = new Box(pos);
        for (Entity entity : mc.world.getOtherEntities(null, box)) {
            if (entity instanceof EndCrystalEntity crystal) {
                attackEntity(crystal, swing);
            }
        }
    }

    public static void attackEntity(Entity entity, boolean swing) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        if (swing) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }
}