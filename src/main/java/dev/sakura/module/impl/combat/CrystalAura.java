package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.manager.Managers;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.CombatUtil;
import dev.sakura.utils.combat.CrystalUtil;
import dev.sakura.utils.combat.DamageUtil;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.render.Render3DUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.world.BlockUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CrystalAura extends Module {
    public CrystalAura() {
        super("CrystalAura", Category.Combat);
    }

    private final EnumValue<Page> page = new EnumValue<>("Page", Page.General);

    // --- General Settings ---
    private final EnumValue<TargetMode> targetMode = new EnumValue<>("Target Mode", TargetMode.Closest, () -> page.is(Page.General));
    private final NumberValue<Double> targetRange = new NumberValue<>("Target Range", 10.0, 1.0, 15.0, 0.5, () -> page.is(Page.General));
    private final EnumValue<SwitchMode> autoSwitch = new EnumValue<>("Auto Switch", SwitchMode.Normal, () -> page.is(Page.General));
    private final EnumValue<RotateMode> rotate = new EnumValue<>("Rotate", RotateMode.Packet, () -> page.is(Page.General));

    // --- Place Settings ---
    private final BoolValue place = new BoolValue("Place", true, () -> page.is(Page.Place));
    private final NumberValue<Double> placeDelay = new NumberValue<>("Place Delay", 50.0, 0.0, 500.0, 10.0, () -> page.is(Page.Place) && place.get());
    private final NumberValue<Double> placeRange = new NumberValue<>("Place Range", 5.0, 1.0, 6.0, 0.1, () -> page.is(Page.Place) && place.get());
    private final NumberValue<Double> minPlaceDamage = new NumberValue<>("Min Place Dmg", 6.0, 0.0, 20.0, 0.5, () -> page.is(Page.Place) && place.get());
    private final NumberValue<Double> maxSelfPlace = new NumberValue<>("Max Self Dmg", 8.0, 0.0, 20.0, 0.5, () -> page.is(Page.Place) && place.get());
    private final BoolValue facePlace = new BoolValue("Face Place", true, () -> page.is(Page.Place));
    private final NumberValue<Double> facePlaceHealth = new NumberValue<>("Face Place HP", 8.0, 1.0, 20.0, 0.5, () -> page.is(Page.Place) && facePlace.get());
    private final BoolValue placeCollision = new BoolValue("Place Raytrace", true, () -> page.is(Page.Place));
    private final NumberValue<Double> collisionOffset = new NumberValue<>("Raytrace Offset", 0.1, 0.0, 0.5, 0.05, () -> page.is(Page.Place) && placeCollision.get());

    // --- Break Settings ---
    private final BoolValue explode = new BoolValue("Break", true, () -> page.is(Page.Break));
    private final NumberValue<Double> breakDelay = new NumberValue<>("Break Delay", 50.0, 0.0, 500.0, 10.0, () -> page.is(Page.Break) && explode.get());
    private final NumberValue<Double> breakRange = new NumberValue<>("Break Range", 5.0, 1.0, 6.0, 0.1, () -> page.is(Page.Break) && explode.get());
    private final NumberValue<Double> minBreakDamage = new NumberValue<>("Min Break Dmg", 4.0, 0.0, 20.0, 0.5, () -> page.is(Page.Break) && explode.get());
    private final NumberValue<Double> maxSelfBreak = new NumberValue<>("Max Self Break", 8.0, 0.0, 20.0, 0.5, () -> page.is(Page.Break) && explode.get());
    private final NumberValue<Integer> breakAttempts = new NumberValue<>("Break Attempts", 1, 1, 5, 1, () -> page.is(Page.Break) && explode.get());

    // --- Render Settings ---
    private final BoolValue render = new BoolValue("Render", true, () -> page.is(Page.Render));
    private final ColorValue renderColor = new ColorValue("Color", new Color(255, 0, 0, 100), () -> page.is(Page.Render) && render.get());
    private final EnumValue<SwingMode> swing = new EnumValue<>("Swing", SwingMode.Main, () -> page.is(Page.Render));

    // --- Calculation Settings ---
    private final NumberValue<Double> calcDelay = new NumberValue<>("Calc Delay", 5.0, 0.0, 500.0, 5.0, () -> page.is(Page.Calc));
    private final NumberValue<Double> predictTicks = new NumberValue<>("Predict Ticks", 2.0, 0.0, 10.0, 1.0, () -> page.is(Page.Calc));
    private final BoolValue extrapolation = new BoolValue("Extrapolation", true, () -> page.is(Page.Calc));

    private final TimerUtil calcTimer = new TimerUtil();
    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil breakTimer = new TimerUtil();

    private final List<PlayerEntity> activeTargets = new ArrayList<>();
    private final List<EndCrystalEntity> activeCrystals = new ArrayList<>();

    private CalculationResult currentResult = new CalculationResult(null, null, 0, null);

    private enum Page {
        General,
        Place,
        Break,
        Render,
        Calc,
        Misc
    }

    private enum SwitchMode {
        None,
        Normal,
        Silent
    }

    private enum SwingMode {
        Main,
        Off,
        None
    }

    private enum RotateMode {
        Off,
        On,
        Packet
    }

    private enum TargetMode {
        Closest,
        Health,
        Damage
    }

    @Override
    public void onEnable() {
        calcTimer.reset();
        placeTimer.reset();
        breakTimer.reset();
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        activeTargets.clear();
        activeCrystals.clear();
        currentResult = new CalculationResult(null, null, 0, null);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (calcTimer.hasReached(calcDelay.get())) {
            updateEntities();
            currentResult = performCalculation();
            calcTimer.reset();
        }

        handleBreakage();
        handlePlacement();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!render.get() || currentResult.placePos() == null) return;
        Render3DUtil.drawFilledBox(event.getMatrices(), currentResult.placePos(), renderColor.get());

        if (!extrapolation.get()) return;
        activeTargets.forEach(playerEntity -> {
            Box box = Managers.EXTRAPOLATION.extrapolate((AbstractClientPlayerEntity) playerEntity, predictTicks.get().intValue(), 2);
            Render3DUtil.drawFilledBox(event.getMatrices(), box, renderColor.get());
        });
    }

    private void updateEntities() {
        activeTargets.clear();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (isValidTarget(p)) {
                activeTargets.add(p);
            }
        }

        switch (targetMode.get()) {
            case Closest -> activeTargets.sort(Comparator.comparingDouble(p -> mc.player.distanceTo(p)));
            case Health -> activeTargets.sort(Comparator.comparingDouble(p -> p.getHealth() + p.getAbsorptionAmount()));
        }

        activeCrystals.clear();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof EndCrystalEntity crystal && e.isAlive() && mc.player.distanceTo(e) <= breakRange.get()) {
                activeCrystals.add(crystal);
            }
        }
    }

    private boolean isValidTarget(PlayerEntity p) {
        return p != mc.player
                && p.isAlive()
                && !p.isSpectator()
                && mc.player.distanceTo(p) <= targetRange.get();
    }

    private CalculationResult performCalculation() {
        if (activeTargets.isEmpty()) return new CalculationResult(null, null, 0, null);
        if (!place.get() && !explode.get()) return new CalculationResult(null, null, 0, null);

        EndCrystalEntity bestCrystal = null;
        double maxBreakDmg = 0.0;

        PlayerEntity self = mc.player;

        // 1. Calculate Best Break
        if (explode.get()) {
            for (EndCrystalEntity crystal : activeCrystals) {
                if (mc.player.distanceTo(crystal) > breakRange.get()) continue;

                double selfDamage = DamageUtil.calculateDamage(crystal.getX(), crystal.getY(), crystal.getZ(), self, self, 6);
                if (selfDamage > maxSelfBreak.get()) continue;

                for (PlayerEntity target : activeTargets) {
                    double dmg = calculateDamage(crystal.getX(), crystal.getY(), crystal.getZ(), target);
                    if (dmg > maxBreakDmg && dmg >= minBreakDamage.get()) {
                        maxBreakDmg = dmg;
                        bestCrystal = crystal;
                    }
                }
            }
        }

        BlockPos bestPlacePos = null;
        double maxPlaceDmg = 0.0;
        Vec3d bestHitVec = null;
        EndCrystalEntity blockingCrystal = null;

        // 2. Calculate Best Place
        if (place.get()) {
            List<BlockPos> sphere = BlockUtil.getSphere(placeRange.get().floatValue());
            List<PlaceCandidate> candidates = new ArrayList<>();

            for (BlockPos pos : sphere) {
                // Check if we can place (standard check)
                if (!CrystalUtil.canPlaceCrystal(pos)) {
                    // Check if blocked ONLY by a crystal (and we can break it)
                    if (CrystalUtil.canPlaceCrystalOn(pos) && CrystalUtil.hasValidSpaceForCrystal(pos)) {
                        EndCrystalEntity blocker = getBlockingCrystal(pos);
                        if (blocker != null && explode.get()) {
                            // This is a "Break before Place" candidate
                            double selfDamage = DamageUtil.calculateDamage(pos, self);
                            if (selfDamage > maxSelfPlace.get()) continue;

                            for (PlayerEntity target : activeTargets) {
                                double dmg = calculateDamage(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, target);
                                // Prioritize this if it's good
                                if (dmg >= minPlaceDamage.get()) {
                                    candidates.add(new PlaceCandidate(pos, dmg, selfDamage, blocker));
                                }
                            }
                        }
                    }
                    continue;
                }

                double selfDamage = DamageUtil.calculateDamage(pos, self);
                if (selfDamage > maxSelfPlace.get()) continue;

                for (PlayerEntity target : activeTargets) {
                    double dmg = calculateDamage(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, target);

                    boolean isFacePlace = facePlace.get() && (target.getHealth() + target.getAbsorptionAmount()) <= facePlaceHealth.get();
                    double minDmg = isFacePlace ? 2.0 : minPlaceDamage.get();

                    if (dmg >= minDmg) {
                        candidates.add(new PlaceCandidate(pos, dmg, selfDamage, null));
                    }
                }
            }

            // Sort by Damage
            candidates.sort(Comparator.comparingDouble(PlaceCandidate::damage).reversed());

            // Select best valid candidate
            for (PlaceCandidate c : candidates) {
                Vec3d hitVec = getVisibleVec(c.pos);
                if (hitVec != null) {
                    if (c.damage > maxPlaceDmg) {
                        maxPlaceDmg = c.damage;
                        bestPlacePos = c.pos;
                        bestHitVec = hitVec;
                        if (c.blocker != null) {
                            blockingCrystal = c.blocker;
                        }
                    }
                    break;
                }
            }
        }

        //这个傻逼ai
        EndCrystalEntity finalBreak = bestCrystal;
        if (blockingCrystal != null) {
            finalBreak = blockingCrystal;
        }

        return new CalculationResult(bestPlacePos, finalBreak, Math.max(maxBreakDmg, maxPlaceDmg), bestHitVec);
    }

    private record PlaceCandidate(BlockPos pos, double damage, double selfDamage, EndCrystalEntity blocker) {
    }

    private EndCrystalEntity getBlockingCrystal(BlockPos pos) {
        Box box = CrystalUtil.getCrystalPlacingBB(pos);
        for (Entity e : mc.world.getOtherEntities(null, box)) {
            if (e instanceof EndCrystalEntity crystal && e.isAlive()) return crystal;
        }
        return null;
    }

    private Vec3d getVisibleVec(BlockPos pos) {
        if (!placeCollision.get()) return pos.toCenterPos();

        // Check center top surface
        Vec3d center = pos.toCenterPos().add(0, 0.5, 0); // Top of block
        if (canSee(center)) return center;

        // Try offsets
        double off = collisionOffset.get();
        if (off == 0) return null;

        Vec3d[] offsets = {
                center.add(off, 0, off),
                center.add(-off, 0, off),
                center.add(off, 0, -off),
                center.add(-off, 0, -off),
                center.add(off, 0, 0),
                center.add(-off, 0, 0),
                center.add(0, 0, off),
                center.add(0, 0, -off)
        };

        for (Vec3d v : offsets) {
            if (canSee(v)) return v;
        }

        return null;
    }

    private boolean canSee(Vec3d to) {
        Vec3d eyes = mc.player.getEyePos();
        // Raycast to the point. If we hit air until the point, good.
        // If we hit a block, it should be the block at 'to' (implied, but 'to' is usually air/surface).
        // Using SHAPE_TYPE.COLLIDER so we hit blocks.
        HitResult result = mc.world.raycast(new RaycastContext(eyes, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result == null || result.getType() == HitResult.Type.MISS || result.getPos().distanceTo(to) < 0.1;
    }

    private double calculateDamage(double x, double y, double z, PlayerEntity target) {
        if (extrapolation.get()) {
            Box box = Managers.EXTRAPOLATION.extrapolate((AbstractClientPlayerEntity) target, predictTicks.get().intValue(), 2);
            if (box != null) {
                return DamageUtil.calculateDamage(x, y, z, target, box, 6);
            }
        }
        return DamageUtil.calculateDamage(x, y, z, target, target, 6);
    }

    private void handleBreakage() {
        if (!explode.get() || currentResult.breakEntity() == null) return;
        // Check delay only if we are NOT in "Break to Place" mode? 
        // No, always check delay to prevent ban.
        if (!breakTimer.hasReached(breakDelay.get())) return;

        EndCrystalEntity crystal = currentResult.breakEntity();
        if (crystal != null && crystal.isAlive()) {
            rotate(crystal.getPos());
            for (int i = 0; i < breakAttempts.get(); i++) {
                CombatUtil.attackEntity(crystal, swing.get() != SwingMode.None);
            }
            if (swing.get() != SwingMode.None) swingHand();
            breakTimer.reset();
        }
    }

    private void handlePlacement() {
        if (!place.get() || currentResult.placePos() == null) return;
        if (!placeTimer.hasReached(placeDelay.get())) return;

        BlockPos pos = currentResult.placePos();
        Vec3d hitVec = currentResult.hitVec();
        if (hitVec == null) hitVec = pos.toCenterPos(); // Fallback

        // Switch Item
        if (!ensureCrystalInHand()) return;

        rotate(hitVec);

        // Use the hitVec for interaction
        BlockHitResult result = new BlockHitResult(hitVec, Direction.UP, pos, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, 0));

        if (swing.get() != SwingMode.None) swingHand();

        if (autoSwitch.get().equals(SwitchMode.Silent)) InvUtil.swapBack();

        placeTimer.reset();
    }

    private boolean ensureCrystalInHand() {
        if (isHoldingCrystal()) return true;

        if (autoSwitch.get() != SwitchMode.None) {
            int slot = InvUtil.findInHotbar(Items.END_CRYSTAL).slot();
            if (slot != -1) {
                InvUtil.swap(slot, autoSwitch.get() == SwitchMode.Silent);
                return true;
            }
        }
        return false;
    }

    private boolean isHoldingCrystal() {
        return mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ||
                mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL;
    }

    private void rotate(Vec3d pos) {
        switch (rotate.get()) {
            case Packet -> RotationManager.lookAt(pos, 100);
            case On -> {
            }
            case Off -> {
            }
        }
    }

    private void swingHand() {
        if (swing.get() == SwingMode.Main) mc.player.swingHand(Hand.MAIN_HAND);
        else if (swing.get() == SwingMode.Off) mc.player.swingHand(Hand.OFF_HAND);
    }

    private record CalculationResult(BlockPos placePos, EndCrystalEntity breakEntity, double damage, Vec3d hitVec) {
    }
}
