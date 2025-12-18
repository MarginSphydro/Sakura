package dev.sakura.module.impl.movement;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.player.PlayerTickEvent;
import dev.sakura.events.player.StrafeEvent;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.math.MathUtils;
import dev.sakura.utils.player.MovementUtils;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.rotation.RaytraceUtils;
import dev.sakura.utils.rotation.RotationUtils;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.utils.vector.Vector3d;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Scaffold extends Module {
    private final BoolValue telly = new BoolValue("Telly", false);
    private final NumberValue<Integer> tellyTick = new NumberValue<>("TellyTick", 1, 0, 8, 1, () -> telly.get());
    private final BoolValue keepY = new BoolValue("KeepY", true, () -> telly.get());
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("RotationSpeed", 10, 0, 10, 1);
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("RotationBackSpeed", 10, 0, 10, 1);
    private final BoolValue moveFix = new BoolValue("MovementFix", true);

    private int yLevel;
    private BlockCache blockCache;
    private int airTicks;

    public Scaffold() {
        super("Scaffold", Category.Movement);
    }

    public int getYLevel() {
        if (keepY.get() && !mc.options.jumpKey.isPressed() && MovementUtils.isMoving() && telly.get()) {
            return yLevel;
        } else {
            return (int) (mc.player.getY() - 1);
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        getBlockInfo();
        if (telly.get()) {
            if (mc.player.isOnGround()) {
                yLevel = (int) (mc.player.getY() - 1);
                airTicks = 0;
                Vector2f rotation = new Vector2f(mc.player.getYaw(), mc.player.getPitch());
                MovementFix movementFix = moveFix.get() ? MovementFix.NORMAL : MovementFix.OFF;
                RotationManager.setRotations(rotation, rotationBackSpeed.get(), movementFix);
            } else {
                if (airTicks >= tellyTick.get() && blockCache != null) {
                    Vector2f calculate = RotationUtils.calculate(getVec3(blockCache.position, blockCache.facing));
                    MovementFix movementFix = moveFix.get() ? MovementFix.NORMAL : MovementFix.OFF;
                    RotationManager.setRotations(calculate, rotationSpeed.get(), movementFix);
                    place();
                }
                airTicks++;
            }
        } else if (blockCache != null) {
            Vector2f calculate = RotationUtils.calculate(getVec3(blockCache.position, blockCache.facing));
            MovementFix movementFix = moveFix.get() ? MovementFix.NORMAL : MovementFix.OFF;
            RotationManager.setRotations(calculate, rotationSpeed.get(), movementFix);
            place();
        }
    }

    @EventHandler
    public void onStrafe(StrafeEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isOnGround() && MovementUtils.isMoving() && telly.get()) mc.player.jump();
    }

    public void place() {
        boolean b = RaytraceUtils.overBlock(RotationManager.getRotation(), blockCache.facing, blockCache.position, false);
        if (b) {
            if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(getVec3(blockCache.position, blockCache.facing), blockCache.facing, blockCache.position, false)) == ActionResult.SUCCESS) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public void getBlockInfo() {
        Vec3d baseVec = mc.player.getEyePos();
//        BlockPos base = new BlockPos(baseVec.x, baseY + 0.1f, baseVec.z);
        BlockPos base = BlockPos.ofFloored(baseVec.x, getYLevel() , baseVec.z);
        int baseX = base.getX();
        int baseZ = base.getZ();
        if (mc.world.getBlockState(base).hasSolidTopSurface(mc.world, base, mc.player)) return;
        if (checkBlock(baseVec, base)) {
            return;
        }
        for (int d = 1; d <= 6; d++) {
            if (checkBlock(baseVec, new BlockPos(
                    baseX,
                    getYLevel() - d,
                    baseZ
            ))) {
                return;
            }
            for (int x = 1; x <= d; x++) {
                for (int z = 0; z <= d - x; z++) {
                    int y = d - x - z;
                    for (int rev1 = 0; rev1 <= 1; rev1++) {
                        for (int rev2 = 0; rev2 <= 1; rev2++) {
                            if (checkBlock(baseVec, new BlockPos(
                                    baseX + (rev1 == 0 ? x : -x),
                                    getYLevel() - y,
                                    baseZ + (rev2 == 0 ? z : -z)
                            ))) return;
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3d baseVec, BlockPos pos) {
        if (!(mc.world.getBlockState(pos).getBlock() instanceof AirBlock)) return false;
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (Direction 脸 : Direction.values()) {
            Vec3d hit = center.add(new Vec3d(脸.getVector()).multiply(0.5));
            Vec3i baseBlock = pos.add(脸.getVector());
            BlockPos baseBlockPos = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
            if (!mc.world.getBlockState(baseBlockPos).hasSolidTopSurface(mc.world, baseBlockPos, mc.player))
                continue;
            Vec3d relevant = hit.subtract(baseVec);
            if (relevant.lengthSquared() <= 4.5 * 4.5 && relevant.dotProduct(
                    new Vec3d(脸.getVector())
            ) >= 0) {
                if (脸.getOpposite() == Direction.UP && !telly.get() && MovementUtils.isMoving() && !mc.options.jumpKey.isPressed()) continue;
                blockCache = new BlockCache(new BlockPos(baseBlock), 脸.getOpposite());
                return true;
            }
        }
        return false;
    }

    public static Vec3d getVec3(BlockPos pos, Direction face) {
        double x = (double) pos.getX() + 0.5;
        double y = (double) pos.getY() + 0.5;
        double z = (double) pos.getZ() + 0.5;
        if (face == Direction.UP || face == Direction.DOWN) {
            x += MathUtils.getRandom(0.3, -0.3);
            z += MathUtils.getRandom(0.3, -0.3);
        } else {
            y += MathUtils.getRandom(0.3, -0.3);
        }
        if (face == Direction.WEST || face == Direction.EAST) {
            z += MathUtils.getRandom(0.3, -0.3);
        }
        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += MathUtils.getRandom(0.3, -0.3);
        }
        return new Vec3d(x, y, z);
    }


    public static class BlockCache {

        private final BlockPos position;
        private final Direction facing;

        public BlockCache(final BlockPos position, final Direction facing) {
            this.position = position;
            this.facing = facing;
        }

        public BlockPos getPosition() {
            return this.position;
        }

        public Direction getFacing() {
            return this.facing;
        }
    }

}
