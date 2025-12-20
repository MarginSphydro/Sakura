package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.manager.impl.PlaceManager;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.player.FindItemResult;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.List;

public class Surround extends Module {
    private final NumberValue<Integer> delay = new NumberValue<>("Delay", 0, 0, 10, 1);
    private final NumberValue<Integer> blocksPerTick = new NumberValue<>("Blocks Per Tick", 4, 1, 10, 1);
    private final BoolValue rotate = new BoolValue("Rotate", true);
    private final BoolValue center = new BoolValue("Center", true);
    private final BoolValue extend = new BoolValue("Extend", true);
    private final BoolValue support = new BoolValue("Support", true);
    private final BoolValue attack = new BoolValue("Attack", true);

    private final TimerUtil timer = new TimerUtil();
    private final List<BlockPos> insideBlocks = new ArrayList<>();
    private final List<BlockPos> surroundBlocks = new ArrayList<>();
    private final List<BlockPos> supportPositions = new ArrayList<>();

    public Surround() {
        super("Surround", Category.Combat);
    }

    @Override
    public void onEnable() {
        timer.reset();
        if (center.get()) {
            Vec3d centerPos = Vec3d.ofBottomCenter(mc.player.getBlockPos());
            //mc.player.setPosition(centerPos.getX(), mc.player.getY(), centerPos.getZ());
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(centerPos.getX(), mc.player.getY(), centerPos.getZ(), mc.player.isOnGround(), mc.player.horizontalCollision));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (!timer.delay(delay.get())) return;

        if (!mc.player.isOnGround()) {
            setState(false);
            return;
        }

        updateBlocks();

        if (support.get()) {
            // 辅助放置，后续加个airplace检测。 如果airplace就不用放
            updateSupport();
        }

        FindItemResult obsidian = InvUtil.findInHotbar(Items.OBSIDIAN);
        FindItemResult enderChest = InvUtil.findInHotbar(Items.ENDER_CHEST);
        FindItemResult result = obsidian.found() ? obsidian : enderChest;

        if (!result.found()) {
            toggle();
            return;
        }

        int placed = 0;

        for (BlockPos pos : supportPositions) {
            if (placed >= blocksPerTick.get()) break;
            if (placeBlock(pos, result)) {
                placed++;
            }
        }

        for (BlockPos pos : surroundBlocks) {
            if (placed >= blocksPerTick.get()) break;
            if (placeBlock(pos, result)) {
                placed++;
            }
        }

        if (placed > 0) {
            timer.reset();
        }
    }

    private void updateBlocks() {
        insideBlocks.clear();
        surroundBlocks.clear();

        BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
        insideBlocks.add(playerPos);

        if (extend.get()) {
            Box playerBox = mc.player.getBoundingBox();
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos offset = playerPos.offset(dir);
                if (playerBox.intersects(new Box(offset))) {
                    insideBlocks.add(offset);
                }
            }
        }

        for (BlockPos pos : insideBlocks) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos offset = pos.offset(dir);
                if (!insideBlocks.contains(offset) && !surroundBlocks.contains(offset)) {
                    surroundBlocks.add(offset);
                }
            }
        }
    }

    private void updateSupport() {
        supportPositions.clear();
        for (BlockPos pos : surroundBlocks) {
            addSupport(pos);
        }
    }

    private void addSupport(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return;
        if (PlaceManager.calcSide(pos) != null) return;

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos neighbor = pos.offset(dir);

            if (surroundBlocks.contains(neighbor) || insideBlocks.contains(neighbor)) continue;
            if (!mc.world.getBlockState(neighbor).isReplaceable()) continue;

            if (PlaceManager.calcSide(neighbor) == null) continue;

            supportPositions.add(neighbor);
            return;
        }
    }

    private boolean placeBlock(BlockPos pos, FindItemResult result) {
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        if (attack.get()) {
            // 便利当前碰撞箱碰到的水晶实体并且攻击它
            for (Entity entity : mc.world.getOtherEntities(null, new Box(pos))) {
                if (entity instanceof EndCrystalEntity) {
                    mc.interactionManager.attackEntity(mc.player, entity);
                }
            }
        }

        if (!mc.world.getOtherEntities(null, new Box(pos)).isEmpty()) return false;

        Direction side = PlaceManager.calcSide(pos);
        if (side == null) return false;

        BlockPos neighbor = pos.offset(side);
        Direction opposite = side.getOpposite();
        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(opposite.getVector().getX() * 0.5, opposite.getVector().getY() * 0.5, opposite.getVector().getZ() * 0.5);

        if (rotate.get()) {
            Vector2f rotation = getRotationTo(hitVec);
            RotationManager.setRotations(rotation, 100, MovementFix.NORMAL);
        }

        BlockHitResult hitResult = new BlockHitResult(hitVec, opposite, neighbor, false);
        PlaceManager.placeBlock(hitResult, result, true, true);

        return true;
    }

    private Vector2f getRotationTo(Vec3d posTo) {
        Vec3d eyePos = mc.player.getEyePos();
        double d = posTo.x - eyePos.x;
        double d2 = posTo.y - eyePos.y;
        double d3 = posTo.z - eyePos.z;
        double d4 = Math.sqrt(d * d + d3 * d3);
        float f = (float) (MathHelper.atan2(d3, d) * 57.2957763671875) - 90.0f;
        float f2 = (float) (-(MathHelper.atan2(d2, d4) * 57.2957763671875));
        return new Vector2f(f, f2);
    }
}
