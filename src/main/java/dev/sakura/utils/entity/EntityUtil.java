package dev.sakura.utils.entity;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class EntityUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static BlockPos getPlayerPos(boolean floor) {
        if (mc.player == null) return BlockPos.ORIGIN;
        if (floor) {
            return new BlockPos(
                    (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()),
                    (int) Math.floor(mc.player.getZ())
            );
        }
        return mc.player.getBlockPos();
    }

    public static boolean isInWeb(PlayerEntity player) {
        if (mc.world == null || player == null) return false;
        BlockPos pos = player.getBlockPos();
        return mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB ||
                mc.world.getBlockState(pos.up()).getBlock() == Blocks.COBWEB;
    }

    public static void syncInventory() {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.player.getInventory().updateItems();
    }
}