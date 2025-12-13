package dev.sakura.utils.world;

import net.minecraft.util.math.BlockPos;

public class BlockPosX extends BlockPos {
    public BlockPosX(double x, double y, double z) {
        super((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public BlockPosX(int x, int y, int z) {
        super(x, y, z);
    }
}