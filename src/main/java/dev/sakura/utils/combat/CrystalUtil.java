package dev.sakura.utils.combat;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

import static dev.sakura.Sakura.mc;

public class CrystalUtil {
    public static boolean canPlaceCrystal(BlockPos pos, LivingEntity ignoredEntity) {
        if (!canPlaceCrystalOn(pos)) return false;
        if (!hasValidSpaceForCrystal(pos)) return false;

        Box placingBB = getCrystalPlacingBB(pos);
        List<Entity> entities = mc.world.getOtherEntities(null, placingBB);
        for (Entity entity : entities) {
            if (!entity.isAlive()) continue;
            if (ignoredEntity != null && entity.equals(ignoredEntity)) continue;
            if (entity instanceof EndCrystalEntity)
                continue;

            return false;
        }

        return true;
    }

    public static boolean canPlaceCrystal(BlockPos pos) {
        return canPlaceCrystal(pos, null);
    }

    public static boolean canPlaceCrystalOn(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
    }

    public static boolean hasValidSpaceForCrystal(BlockPos pos) {
        if (mc.world == null) return false;
        return mc.world.isAir(pos.up());
    }

    public static Box getCrystalPlacingBB(BlockPos pos) {
        return new Box(
                pos.getX(), pos.getY() + 1.0, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 3.0, pos.getZ() + 1.0
        );
    }

    public static boolean crystalPlaceBoxIntersectsCrystalBox(BlockPos placePos, EndCrystalEntity crystal) {
        return crystalPlaceBoxIntersectsCrystalBox(placePos, crystal.getX(), crystal.getY(), crystal.getZ());
    }

    public static boolean crystalPlaceBoxIntersectsCrystalBox(BlockPos placePos, double crystalX, double crystalY, double crystalZ) {
        int cY = (int) Math.floor(crystalY);
        int cX = (int) Math.floor(crystalX);
        int cZ = (int) Math.floor(crystalZ);

        int dY = cY - placePos.getY();
        int dX = cX - placePos.getX();
        int dZ = cZ - placePos.getZ();

        return (dY > 0 && dY < 2) && (dX > -1 && dX < 1) && (dZ > -1 && dZ < 1);
    }

    public static boolean crystalIntersects(BlockPos crystal1, BlockPos crystal2) {
        int dY = Math.abs(crystal2.getY() - crystal1.getY());
        int dX = Math.abs(crystal2.getX() - crystal1.getX());
        int dZ = Math.abs(crystal2.getZ() - crystal1.getZ());

        return dY < 2 && dX < 2 && dZ < 2;
    }
}
