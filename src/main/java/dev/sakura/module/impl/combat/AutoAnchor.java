package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.DamageUtil;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.render.Render3DUtil;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.rotation.RotationUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.List;
import java.util.*;

public class AutoAnchor extends Module {
    public static AutoAnchor INSTANCE;

    public enum Page { General, Rotate, Calc, Render }
    public enum PlaceMode { Head, Feet, Both }
    public enum SwitchMode { None, Normal, Silent, InvSilent }

    private final EnumValue<Page> page = new EnumValue<>("Page", Page.General);

    private final EnumValue<PlaceMode> placeMode = new EnumValue<>("PlaceMode", PlaceMode.Feet, () -> page.is(Page.General));
    private final EnumValue<SwitchMode> switchMode = new EnumValue<>("Switch", SwitchMode.Silent, () -> page.is(Page.General));
    private final NumberValue<Double> range = new NumberValue<>("Range", 5.0, 1.0, 6.0, 0.1, () -> page.is(Page.General));
    private final NumberValue<Double> targetRange = new NumberValue<>("TargetRange", 8.0, 1.0, 12.0, 0.1, () -> page.is(Page.General));
    private final BoolValue breakCrystal = new BoolValue("BreakCrystal", true, () -> page.is(Page.General));
    private final BoolValue fastPlace = new BoolValue("FastPlace", true, () -> page.is(Page.General));
    private final BoolValue usingPause = new BoolValue("UsingPause", true, () -> page.is(Page.General));
    private final NumberValue<Integer> placeDelay = new NumberValue<>("PlaceDelay", 50, 0, 500, 1, () -> page.is(Page.General));
    private final NumberValue<Integer> calcDelay = new NumberValue<>("CalcDelay", 50, 0, 1000, 1, () -> page.is(Page.General));

    private final BoolValue rotate = new BoolValue("Rotate", true, () -> page.is(Page.Rotate));
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("RotationSpeed", 10, 0, 10, 1, () -> page.is(Page.Rotate) && rotate.get());
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("BackSpeed", 10, 0, 10, 1, () -> page.is(Page.Rotate) && rotate.get());

    private final BoolValue noSuicide = new BoolValue("NoSuicide", true, () -> page.is(Page.Calc));
    private final BoolValue terrainIgnore = new BoolValue("TerrainIgnore", true, () -> page.is(Page.Calc));
    private final NumberValue<Double> minDamage = new NumberValue<>("MinDamage", 4.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));
    private final NumberValue<Double> maxSelfDamage = new NumberValue<>("MaxSelf", 8.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));
    private final NumberValue<Integer> predictTicks = new NumberValue<>("Predict", 2, 0, 20, 1, () -> page.is(Page.Calc));

    private final BoolValue render = new BoolValue("Render", true, () -> page.is(Page.Render));
    private final BoolValue shrink = new BoolValue("Shrink", true, () -> page.is(Page.Render) && render.get());
    private final ColorValue boxColor = new ColorValue("BoxColor", new Color(255, 255, 255, 255), () -> page.is(Page.Render) && render.get());
    private final ColorValue fillColor = new ColorValue("FillColor", new Color(255, 255, 255, 50), () -> page.is(Page.Render) && render.get());
    private final NumberValue<Double> fadeSpeed = new NumberValue<>("FadeSpeed", 0.2, 0.01, 1.0, 0.01, () -> page.is(Page.Render) && render.get());

    private final TimerUtil delayTimer = new TimerUtil();
    private final TimerUtil calcTimer = new TimerUtil();

    public BlockPos currentPos;
    private BlockPos tempPos;
    public PlayerEntity target;
    private double lastDamage;
    private boolean isRotating;

    private Vec3d renderPos;
    private Vec3d animatedPos;
    private double fade;
    private long lastPosTime;

    public AutoAnchor() {
        super("AutoAnchor", Category.Combat);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        currentPos = null;
        tempPos = null;
        target = null;
        renderPos = null;
        animatedPos = null;
        fade = 0;
        didInvSwitch = false;
        isRotating = false;
    }

    @Override
    protected void onDisable() {
        currentPos = null;
        target = null;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isDead()) return;

        setSuffix(placeMode.get().name());
        isRotating = false;

        if (calcTimer.hasReached(calcDelay.get())) {
            calcTimer.reset();
            calculate();
        }

        currentPos = tempPos;

        if (currentPos == null) {
            target = null;
            if (rotate.get()) {
                RotationManager.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
            }
            return;
        }

        int anchorSlot = findItem(Blocks.RESPAWN_ANCHOR.asItem());
        int glowstoneSlot = findItem(Blocks.GLOWSTONE.asItem());

        if (anchorSlot == -1 || glowstoneSlot == -1) return;
        if (usingPause.get() && mc.player.isUsingItem()) return;
        if (switchMode.is(SwitchMode.None) && mc.player.getMainHandStack().getItem() != Blocks.RESPAWN_ANCHOR.asItem()) return;

        if (!delayTimer.hasReached(placeDelay.get())) return;
        delayTimer.reset();

        BlockState state = mc.world.getBlockState(currentPos);

        if (state.isAir() || state.isReplaceable()) {
            if (canPlaceAt(currentPos)) {
                doRotation(currentPos);
                placeAnchor(currentPos, anchorSlot);
            }
        } else if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
            int charges = state.get(net.minecraft.block.RespawnAnchorBlock.CHARGES);
            if (charges == 0) {
                doRotation(currentPos);
                chargeAnchor(currentPos, glowstoneSlot);
            } else {
                doRotation(currentPos);
                explodeAnchor(currentPos);
                if (fastPlace.get()) {
                    placeAnchor(currentPos, anchorSlot);
                }
            }
        }

        if (!isRotating && rotate.get()) {
            RotationManager.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
        }
    }

    private void doRotation(BlockPos pos) {
        if (!rotate.get()) return;
        Vector2f rotation = RotationUtil.calculate(pos.toCenterPos());
        RotationManager.setRotations(rotation, rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
        isRotating = true;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        if (currentPos != null) {
            renderPos = currentPos.toCenterPos();
            lastPosTime = System.currentTimeMillis();
        }

        if (renderPos == null) return;

        boolean shouldFade = System.currentTimeMillis() - lastPosTime > 300;
        double targetFade = shouldFade ? 0 : 0.5;
        fade = animate(fade, targetFade, fadeSpeed.get() / 10);

        if (fade <= 0.01) {
            animatedPos = null;
            return;
        }

        if (animatedPos == null) {
            animatedPos = renderPos;
        } else {
            animatedPos = new Vec3d(
                    animate(animatedPos.x, renderPos.x, 0.2),
                    animate(animatedPos.y, renderPos.y, 0.2),
                    animate(animatedPos.z, renderPos.z, 0.2)
            );
        }

        double size = shrink.get() ? fade : 0.5;
        Box box = new Box(animatedPos, animatedPos).expand(size);

        int alpha = (int) (fade * 2 * 255);
        Color fill = new Color(fillColor.get().getRed(), fillColor.get().getGreen(), fillColor.get().getBlue(), Math.min(alpha, fillColor.get().getAlpha()));
        Color line = new Color(boxColor.get().getRed(), boxColor.get().getGreen(), boxColor.get().getBlue(), Math.min(alpha, boxColor.get().getAlpha()));

        Render3DUtil.drawFilledBox(event.getMatrices(), box, fill);
        Render3DUtil.drawBoxOutline(event.getMatrices(), box, line.getRGB(), 1.5f);
    }

    private void calculate() {
        tempPos = null;
        target = null;

        List<PlayerEntity> enemies = getEnemies();
        if (enemies.isEmpty()) return;

        double bestDamage = minDamage.get();

        if (placeMode.is(PlaceMode.Head) || placeMode.is(PlaceMode.Both)) {
            for (PlayerEntity enemy : enemies) {
                BlockPos headPos = BlockPos.ofFloored(enemy.getPos()).up(2);
                if (trySetPos(headPos, enemy, minDamage.get())) {
                    return;
                }
            }
        }

        if (placeMode.is(PlaceMode.Feet) || placeMode.is(PlaceMode.Both)) {
            for (PlayerEntity enemy : enemies) {
                BlockPos feetPos = BlockPos.ofFloored(enemy.getPos());

                for (BlockPos pos : getAroundFeet(feetPos)) {
                    if (!canPlaceAt(pos)) continue;

                    double damage = DamageUtil.getAnchorDamage(pos, enemy, terrainIgnore.get());
                    if (damage < bestDamage) continue;

                    double selfDamage = DamageUtil.getAnchorDamage(pos, mc.player, terrainIgnore.get());
                    if (selfDamage > maxSelfDamage.get()) continue;
                    if (noSuicide.get() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()) continue;

                    if (mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range.get() * range.get()) continue;

                    lastDamage = damage;
                    bestDamage = damage;
                    target = enemy;
                    tempPos = pos;
                }
            }
        }
    }

    private List<BlockPos> getAroundFeet(BlockPos feetPos) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(feetPos);
        positions.add(feetPos.north());
        positions.add(feetPos.south());
        positions.add(feetPos.east());
        positions.add(feetPos.west());
        positions.add(feetPos.down());
        positions.add(feetPos.north().down());
        positions.add(feetPos.south().down());
        positions.add(feetPos.east().down());
        positions.add(feetPos.west().down());
        return positions;
    }

    private boolean trySetPos(BlockPos pos, PlayerEntity enemy, double minDmg) {
        if (pos == null) return false;
        if (!canPlaceAt(pos)) return false;
        if (mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos()) > range.get() * range.get()) return false;

        double damage = DamageUtil.getAnchorDamage(pos, enemy, terrainIgnore.get());
        if (damage < minDmg) return false;

        double selfDamage = DamageUtil.getAnchorDamage(pos, mc.player, terrainIgnore.get());
        if (selfDamage > maxSelfDamage.get()) return false;
        if (noSuicide.get() && selfDamage > mc.player.getHealth() + mc.player.getAbsorptionAmount()) return false;

        lastDamage = damage;
        target = enemy;
        tempPos = pos;
        return true;
    }

    private boolean canPlaceAt(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() == Blocks.RESPAWN_ANCHOR) return true;
        if (!state.isReplaceable()) return false;

        Box box = new Box(pos);
        return mc.world.getOtherEntities(null, box, e -> e instanceof PlayerEntity).isEmpty();
    }

    private boolean didInvSwitch = false;

    private void placeAnchor(BlockPos pos, int slot) {
        Direction side = getPlaceSide(pos);
        BlockPos clickPos;
        Direction clickDir;
        
        if (side != null) {
            clickPos = pos.offset(side);
            clickDir = side.getOpposite();
        } else {
            clickPos = pos;
            clickDir = Direction.DOWN;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        doSwitch(slot);
        clickBlock(clickPos, clickDir);
        undoSwitch(oldSlot);
    }

    private void chargeAnchor(BlockPos pos, int slot) {
        Direction side = getClickSide(pos);
        if (side == null) side = Direction.UP;

        int oldSlot = mc.player.getInventory().selectedSlot;
        doSwitch(slot);
        clickBlock(pos, side);
        undoSwitch(oldSlot);
    }

    private void explodeAnchor(BlockPos pos) {
        Direction side = getClickSide(pos);
        if (side == null) side = Direction.UP;

        int oldSlot = mc.player.getInventory().selectedSlot;
        int emptySlot = findEmptyHotbar();
        if (emptySlot != -1 && emptySlot != oldSlot) {
            mc.player.getInventory().selectedSlot = emptySlot;
            mc.interactionManager.syncSelectedSlot();
        }
        clickBlock(pos, side);
        if (emptySlot != -1 && emptySlot != oldSlot) {
            mc.player.getInventory().selectedSlot = oldSlot;
            mc.interactionManager.syncSelectedSlot();
        }
    }

    private int findItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        if (switchMode.is(SwitchMode.InvSilent)) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == item) return i;
            }
        }
        return -1;
    }

    private int findEmptyHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private void doSwitch(int slot) {
        didInvSwitch = false;
        if (slot < 0) return;
        if (slot == mc.player.getInventory().selectedSlot) return;

        if (slot < 9) {
            mc.player.getInventory().selectedSlot = slot;
            mc.interactionManager.syncSelectedSlot();
        } else if (slot < 36 && switchMode.is(SwitchMode.InvSilent)) {
            if (InvUtil.invSwitch(slot)) {
                didInvSwitch = true;
            }
        }
    }

    private void undoSwitch(int oldSlot) {
        if (didInvSwitch) {
            InvUtil.invSwapBack();
            didInvSwitch = false;
        } else if (switchMode.is(SwitchMode.Silent) || switchMode.is(SwitchMode.InvSilent)) {
            if (mc.player.getInventory().selectedSlot != oldSlot) {
                mc.player.getInventory().selectedSlot = oldSlot;
                mc.interactionManager.syncSelectedSlot();
            }
        }
    }

    private void clickBlock(BlockPos pos, Direction side) {
        if (pos == null) return;

        Vec3d hitVec = Vec3d.ofCenter(pos).add(
                side.getOffsetX() * 0.5,
                side.getOffsetY() * 0.5,
                side.getOffsetZ() * 0.5
        );

        mc.player.swingHand(Hand.MAIN_HAND);
        BlockHitResult result = new BlockHitResult(hitVec, side, pos, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));
    }

    private Direction getPlaceSide(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos offset = pos.offset(dir);
            BlockState state = mc.world.getBlockState(offset);
            if (!state.isReplaceable() && state.isSolidBlock(mc.world, offset)) {
                return dir;
            }
        }
        return null;
    }

    private Direction getClickSide(BlockPos pos) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);

        Direction best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            Vec3d sideVec = center.add(dir.getOffsetX() * 0.5, dir.getOffsetY() * 0.5, dir.getOffsetZ() * 0.5);
            double dist = eyePos.squaredDistanceTo(sideVec);

            BlockHitResult ray = mc.world.raycast(new RaycastContext(eyePos, sideVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (ray.getType() == HitResult.Type.MISS || ray.getBlockPos().equals(pos)) {
                if (dist < bestDist) {
                    bestDist = dist;
                    best = dir;
                }
            }
        }
        return best;
    }

    private List<PlayerEntity> getEnemies() {
        List<PlayerEntity> enemies = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isDead() || player.getHealth() <= 0) continue;
            if (mc.player.squaredDistanceTo(player) > targetRange.get() * targetRange.get()) continue;
            enemies.add(player);
        }
        return enemies;
    }

    private double animate(double current, double target, double speed) {
        double diff = target - current;
        return current + diff * Math.min(1, speed);
    }
}