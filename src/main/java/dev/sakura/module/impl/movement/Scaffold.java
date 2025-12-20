package dev.sakura.module.impl.movement;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.player.StrafeEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.manager.Managers;
import dev.sakura.manager.impl.PlaceManager;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.math.MathUtils;
import dev.sakura.utils.player.FindItemResult;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.player.MovementUtil;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.rotation.RaytraceUtil;
import dev.sakura.utils.rotation.RotationUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.awt.*;

public class Scaffold extends Module {
    public Scaffold() {
        super("Scaffold", Category.Movement);
    }

    private final EnumValue<SwapMode> swapMode = new EnumValue<>("Swap Mode", SwapMode.Silent);
    private final BoolValue swingHand = new BoolValue("Swing Hand", true);
    private final BoolValue telly = new BoolValue("Telly", false);
    private final NumberValue<Integer> tellyTick = new NumberValue<>("Telly Tick", 1, 0, 8, 1, telly::get);
    private final BoolValue keepY = new BoolValue("Keep Y", true, telly::get);
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("Rotation Speed", 10, 0, 10, 1);
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("Rotation Back Speed", 10, 0, 10, 1);
    private final BoolValue moveFix = new BoolValue("Movement Fix", true);
    private final BoolValue render = new BoolValue("Render", true);
    private final BoolValue shrink = new BoolValue("Shrink", true, render::get);
    private final ColorValue sideColor = new ColorValue("Side Color", new Color(255, 183, 197, 100), render::get);
    private final ColorValue lineColor = new ColorValue("Line Color", new Color(255, 105, 180), render::get);

    private int yLevel;
    private BlockCache blockCache;
    private int airTicks;

    private enum SwapMode {
        Normal,
        Silent
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
                    Vector2f calculate = RotationUtil.calculate(getVec3(blockCache.position, blockCache.facing));
                    MovementFix movementFix = moveFix.get() ? MovementFix.NORMAL : MovementFix.OFF;
                    RotationManager.setRotations(calculate, rotationSpeed.get(), movementFix);
                    place();
                }
                airTicks++;
            }
        } else if (blockCache != null) {
            Vector2f calculate = RotationUtil.calculate(getVec3(blockCache.position, blockCache.facing));
            MovementFix movementFix = moveFix.get() ? MovementFix.NORMAL : MovementFix.OFF;
            RotationManager.setRotations(calculate, rotationSpeed.get(), movementFix);
            place();
        }
    }

    @EventHandler
    public void onStrafe(StrafeEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isOnGround() && MovementUtil.isMoving() && telly.get() && !mc.options.jumpKey.isPressed()) mc.player.jump();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        //TODO:Render3DUtils.drawFullBox();
    }

    public int getYLevel() {
        if (keepY.get() && !mc.options.jumpKey.isPressed() && MovementUtil.isMoving() && telly.get()) {
            return yLevel;
        } else {
            return (int) (mc.player.getY() - 1);
        }
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(mc.world.getBlockState(pos));
    }

    public void place() {
        boolean hasRotated = RaytraceUtil.overBlock(RotationManager.getRotation(), blockCache.facing, blockCache.position, false);
        FindItemResult item = InvUtil.findInHotbar(itemStack -> validItem(itemStack, blockCache.position));

        if (hasRotated) {
            BlockPos targetPos = blockCache.position.offset(blockCache.facing);
            if (!PlaceManager.isReplaceable(targetPos)) return;
            if (!mc.world.getOtherEntities(null, new Box(targetPos)).isEmpty()) return;

            PlaceManager.placeBlock(
                    new BlockHitResult(getVec3(blockCache.position, blockCache.facing), blockCache.facing, blockCache.position, false),
                    item, swingHand.get(), swapMode.is(SwapMode.Silent)
            );

            if (render.get()) {
                Managers.RENDER.add(targetPos, sideColor.get(), lineColor.get(), 1000, shrink.get());
            }
        }
    }

    public void getBlockInfo() {
        Vec3d baseVec = mc.player.getEyePos();
        BlockPos base = BlockPos.ofFloored(baseVec.x, getYLevel(), baseVec.z);
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
                            if (checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), getYLevel() - y, baseZ + (rev2 == 0 ? z : -z))))
                                return;
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3d baseVec, BlockPos pos) {
        if (!(mc.world.getBlockState(pos).getBlock() instanceof AirBlock)) return false;

        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        for (Direction dir : Direction.values()) {
            Vec3d hit = center.add(new Vec3d(dir.getVector()).multiply(0.5));
            Vec3i baseBlock = pos.add(dir.getVector());
            BlockPos baseBlockPos = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());

            if (!mc.world.getBlockState(baseBlockPos).hasSolidTopSurface(mc.world, baseBlockPos, mc.player)) continue;

            Vec3d relevant = hit.subtract(baseVec);
            if (relevant.lengthSquared() <= 4.5 * 4.5 && relevant.dotProduct(new Vec3d(dir.getVector())) >= 0) {
                if (dir.getOpposite() == Direction.UP && !telly.get() && MovementUtil.isMoving() && !mc.options.jumpKey.isPressed())
                    continue;
                blockCache = new BlockCache(new BlockPos(baseBlock), dir.getOpposite());
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

    private record BlockCache(BlockPos position, Direction facing) {
    }
}
