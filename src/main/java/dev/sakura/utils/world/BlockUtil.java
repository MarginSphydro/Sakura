package dev.sakura.utils.world;

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static dev.sakura.Sakura.mc;

//todo:这个傻逼alien137的东西有个鸡巴用

public class BlockUtil {
    public static List<BlockPos> placedPos = new ArrayList<>();

    public static List<BlockPos> getSphere(float range) {
        List<BlockPos> sphere = new ArrayList<>();
        int iRange = (int) Math.ceil(range);
        float rangeSq = range * range;

        BlockPos center = mc.player.getBlockPos();

        for (int x = -iRange; x <= iRange; x++) {
            for (int y = -iRange; y <= iRange; y++) {
                for (int z = -iRange; z <= iRange; z++) {
                    if (x * x + y * y + z * z <= rangeSq) {
                        sphere.add(center.add(x, y, z));
                    }
                }
            }
        }
        return sphere;
    }

    public static boolean airPlace() {
        return false;
    }

    public static Direction getPlaceSide(BlockPos pos) {
        if (mc.world == null) return null;
        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = pos.offset(direction);
            BlockState state = mc.world.getBlockState(offsetPos);
            if (!state.isReplaceable() && !state.isAir()) {
                return direction;
            }
        }
        return null;
    }

    public static void clickBlock(BlockPos pos, Direction side, boolean rotate, boolean packet) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        Vec3d hitVec = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );

        BlockHitResult hitResult = new BlockHitResult(hitVec, side, pos, false);

        if (packet) {
            mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        } else {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        }
    }
}