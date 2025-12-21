package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.manager.Managers;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.DamageUtil;
import dev.sakura.utils.entity.EntityUtil;
import dev.sakura.utils.player.FindItemResult;
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
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CrystalAura extends Module {
    public CrystalAura() {
        super("CrystalAura", Category.Combat);
    }

    public enum Page {
        General,
        Timing,
        Place,
        Render
    }

    public enum SwitchMode {
        None,
        Normal,
        Silent,
        InvSilent
    }

    public enum FadeMode {
        Up,
        Down,
        Normal
    }

    private final EnumValue<Page> page = new EnumValue<>("Page", Page.General);

    private final NumberValue<Double> targetRange = new NumberValue<>("Target Range", 10.0, 1.0, 20.0, 0.1, () -> page.is(Page.General));
    private final NumberValue<Double> minDamage = new NumberValue<>("Min Damage", 4.0, 0.0, 36.0, 0.1, () -> page.is(Page.General));
    private final NumberValue<Double> maxSelfDamage = new NumberValue<>("Max Self Damage", 8.0, 0.0, 36.0, 0.1, () -> page.is(Page.General));
    private final NumberValue<Double> forcePlaceHealth = new NumberValue<>("Force Place Health", 8.0, 0.0, 36.0, 0.1, () -> page.is(Page.General));

    private final BoolValue rotate = new BoolValue("Rotate", true, () -> page.is(Page.Timing));
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("Rotation Speed", 10, 0, 10, 1, () -> page.is(Page.Timing) && rotate.get());
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("Back Speed", 10, 0, 10, 1, () -> page.is(Page.Timing) && rotate.get());
    private final NumberValue<Integer> placeDelay = new NumberValue<>("Place Delay", 0, 0, 750, 1, () -> page.is(Page.Timing));
    private final NumberValue<Integer> attackDelay = new NumberValue<>("Attack Delay", 0, 0, 750, 1, () -> page.is(Page.Timing));
    private final NumberValue<Integer> forcePlaceDelay = new NumberValue<>("Force Place Delay", 0, 0, 750, 1, () -> page.is(Page.Timing));

    private final BoolValue place = new BoolValue("Place", true, () -> page.is(Page.Place));
    private final NumberValue<Double> placeRange = new NumberValue<>("Place Range", 5.0, 1.0, 6.0, 0.1, () -> page.is(Page.Place));
    private final BoolValue houyuepingMode = new BoolValue("1.12 Place", false, () -> page.is(Page.Place));
    private final BoolValue attack = new BoolValue("Attack", true, () -> page.is(Page.Place));
    private final NumberValue<Double> breakRange = new NumberValue<>("Break Range", 5.0, 1.0, 6.0, 0.1, () -> page.is(Page.Place));
    private final EnumValue<SwitchMode> autoSwitch = new EnumValue<>("Switch", SwitchMode.Normal, () -> page.is(Page.Place));
    private final BoolValue swingHand = new BoolValue("Place Swing", true, () -> page.is(Page.Place));
    private final BoolValue attackSwing = new BoolValue("Attack Swing", true, () -> page.is(Page.Place));

    private final BoolValue render = new BoolValue("Render", true, () -> page.is(Page.Render));
    private final BoolValue renderDamageText = new BoolValue("Render Damage", false, () -> page.is(Page.Render) && render.get());
    private final EnumValue<FadeMode> fadeMode = new EnumValue<>("Fade Mode", FadeMode.Normal, () -> page.is(Page.Render) && render.get());
    private final NumberValue<Double> animationSpeed = new NumberValue<>("Animation Speed", 5.0, 0.1, 20.0, 0.1, () -> page.is(Page.Render) && render.get());
    private final NumberValue<Double> animationExponent = new NumberValue<>("Animation Exp", 3.0, 0.1, 10.0, 0.1, () -> page.is(Page.Render) && render.get());
    private final BoolValue smoothBox = new BoolValue("Smooth Box", true, () -> page.is(Page.Render) && render.get());
    private final BoolValue breathing = new BoolValue("Breathing", true, () -> page.is(Page.Render) && render.get());
    private final ColorValue sideColor = new ColorValue("Side Color", new Color(255, 192, 203, 50), () -> page.is(Page.Render) && render.get());
    private final ColorValue lineColor = new ColorValue("Line Color", new Color(255, 192, 203, 255), () -> page.is(Page.Render) && render.get());
    private final BoolValue extrapolation = new BoolValue("Extrapolation", true, () -> page.is(Page.Render));
    private final NumberValue<Integer> extrapolationTicks = new NumberValue<>("Extra Ticks", 0, 0, 20, 1, () -> page.is(Page.Render) && extrapolation.get());
    private final NumberValue<Integer> smooth = new NumberValue<>("Smooth", 1, 1, 10, 1, () -> page.is(Page.Render) && extrapolation.get());
    private final BoolValue renderExtrapolation = new BoolValue("Render Extrapolation", true, () -> page.is(Page.Render) && render.get());
    private final ColorValue extraSideColor = new ColorValue("Extra Side Color", new Color(135, 206, 235, 50), () -> page.is(Page.Render) && renderExtrapolation.get());
    private final ColorValue extraLineColor = new ColorValue("Extra Line Color", new Color(135, 206, 235, 255), () -> page.is(Page.Render) && renderExtrapolation.get());

    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil breakTimer = new TimerUtil();

    private BlockPos renderPos = null;
    private Vec3d currentRenderPos = null;
    private double renderProgress = 0;
    private long lastTime = 0;
    private double renderDamage = 0;
    private boolean isRotating = false;
    private FindItemResult result = null;

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        renderPos = null;
        currentRenderPos = null;
        renderProgress = 0;
        lastTime = 0;
        renderDamage = 0;
        isRotating = false;
        result = null;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        isRotating = false;

        PlayerEntity target = getTarget();

        if (target == null) {
            renderPos = null;
            if (rotate.get()) {
                RotationManager.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
            }
            return;
        }

        if (attack.get() && breakTimer.hasTimeElapsed(getAttackDelay(target))) {
            doBreak(target);
        }

        if (place.get() && placeTimer.hasTimeElapsed(placeDelay.get().longValue())) {
            doPlace(target);
        }

        if (!isRotating && rotate.get()) {
            RotationManager.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!render.get()) return;

        long currentTime = System.currentTimeMillis();
        double delta = (currentTime - lastTime) / 1000.0;
        lastTime = currentTime;

        if (renderPos != null) {
            renderProgress = Math.min(1.0, renderProgress + delta * animationSpeed.get());
            if (currentRenderPos == null || !smoothBox.get()) {
                currentRenderPos = new Vec3d(renderPos.getX(), renderPos.getY(), renderPos.getZ());
            } else {
                currentRenderPos = currentRenderPos.lerp(new Vec3d(renderPos.getX(), renderPos.getY(), renderPos.getZ()), delta * animationSpeed.get());
            }
        } else {
            renderProgress = Math.max(0.0, renderProgress - delta * animationSpeed.get());
        }

        if (renderProgress > 0.001 && currentRenderPos != null) {
            double r = 0.5 - Math.pow(1.0 - renderProgress, animationExponent.get()) / 2.0;

            double down = 0;
            double up = 0;
            double width = 0.5;

            switch (fadeMode.get()) {
                case Up -> {
                    up = 1.0;
                    down = 1.0 - (r * 2);
                }
                case Down -> {
                    up = r * 2;
                    down = 0;
                }
                case Normal -> {
                    up = 0.5 + r;
                    down = 0.5 - r;
                    width = r;
                }
            }

            Color sColor = sideColor.get();
            Color lColor = lineColor.get();

            if (breathing.get()) {
                float breatheFactor = (float) (Math.sin(System.currentTimeMillis() / 1000.0 * 2.0) + 1.0) / 2.0f;
                float alphaFactor = 0.4f + (breatheFactor * 0.6f);
                float scale = 1.0f - (breatheFactor * 0.15f);

                sColor = new Color(sColor.getRed(), sColor.getGreen(), sColor.getBlue(), Math.max(0, Math.min(255, (int) (sColor.getAlpha() * alphaFactor))));
                lColor = new Color(lColor.getRed(), lColor.getGreen(), lColor.getBlue(), Math.max(0, Math.min(255, (int) (lColor.getAlpha() * alphaFactor))));

                width *= scale;
                double height = up - down;
                double newHeight = height * scale;
                double heightDiff = height - newHeight;

                up -= heightDiff / 2.0;
                down += heightDiff / 2.0;
            }

            Box box = new Box(
                    currentRenderPos.getX() + 0.5 - width, currentRenderPos.getY() + down, currentRenderPos.getZ() + 0.5 - width,
                    currentRenderPos.getX() + 0.5 + width, currentRenderPos.getY() + up, currentRenderPos.getZ() + 0.5 + width
            );

            final Color finalSColor = sColor;
            final Color finalLColor = lColor;

            Render3DUtil.drawFullBox(event.getMatrices(), box, finalSColor, finalLColor);

            if (renderDamageText.get()) {
                Vec3d center = new Vec3d(
                        box.minX + (box.maxX - box.minX) * 0.5,
                        box.minY + (box.maxY - box.minY) * 0.5,
                        box.minZ + (box.maxZ - box.minZ) * 0.5
                );
                Render3DUtil.drawText(String.format("%.1f", renderDamage), center, 0, 0, 0, Color.WHITE);
            }
        }

        if (renderExtrapolation.get()) {
            PlayerEntity target = getTarget();
            if (target != null) {
                Box predicted = Managers.EXTRAPOLATION.extrapolate((AbstractClientPlayerEntity) target, extrapolationTicks.get(), smooth.get());
                if (predicted != null) {
                    Render3DUtil.drawFullBox(event.getMatrices(), predicted, extraSideColor.get(), extraLineColor.get());
                }
            }
        }
    }

    private void doBreak(PlayerEntity target) {
        EndCrystalEntity bestCrystal = null;
        float bestDamage = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (mc.player.distanceTo(crystal) > breakRange.get()) continue;
            if (!crystal.isAlive()) continue;

            float damage = calculateDamage(target, crystal.getPos());
            float selfDamage = calculateDamage(mc.player, crystal.getPos());

            if (selfDamage > maxSelfDamage.get()) continue;
            if (damage < getMinDamage(target)) continue;

            if (damage > bestDamage) {
                bestDamage = damage;
                bestCrystal = crystal;
            }
        }

        if (bestCrystal != null) {
            if (rotate.get()) {
                Vector2f rotation = RotationUtil.calculate(bestCrystal.getPos());
                RotationManager.setRotations(rotation, rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
                isRotating = true;
            }

            mc.interactionManager.attackEntity(mc.player, bestCrystal);

            Hand hand = result == null || result.getHand() == null ? Hand.MAIN_HAND : result.getHand();
            if (attackSwing.get()) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

            breakTimer.reset();
        }
    }

    private void doPlace(PlayerEntity target) {
        BlockPos bestPos = null;
        float bestDamage = 0;
        EndCrystalEntity blockingCrystal = null;

        for (BlockPos pos : getSphere(placeRange.get())) {
            if (!isValidBaseBlock(pos)) continue;

            EndCrystalEntity crystal = getCrystalAt(pos);
            if (crystal == null) {
                if (EntityUtil.intersectsWithEntity(new Box(pos.up()).stretch(0, 1, 0), entity -> true)) continue;
            } else {
                if (EntityUtil.intersectsWithEntity(new Box(pos.up()).stretch(0, 1, 0), entity -> !entity.equals(crystal)))
                    continue;
            }

            Vec3d crystalPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            float damage = calculateDamage(target, crystalPos);
            float selfDamage = calculateDamage(mc.player, crystalPos);

            if (selfDamage > maxSelfDamage.get()) continue;
            if (damage < getMinDamage(target)) continue;

            if (damage > bestDamage) {
                bestDamage = damage;
                bestPos = pos;
                blockingCrystal = crystal;
            }
        }

        if (bestPos != null) {
            if (blockingCrystal != null) {
                if (breakTimer.hasTimeElapsed(getAttackDelay(target))) {
                    if (rotate.get()) {
                        Vector2f rotation = RotationUtil.calculate(blockingCrystal.getPos());
                        RotationManager.setRotations(rotation, rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
                        isRotating = true;
                    }

                    mc.interactionManager.attackEntity(mc.player, blockingCrystal);

                    Hand hand = result == null || result.getHand() == null ? Hand.MAIN_HAND : result.getHand();
                    if (attackSwing.get()) mc.player.swingHand(hand);
                    else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));

                    breakTimer.reset();
                }
                return;
            }

            result = InvUtil.findInHotbar(Items.END_CRYSTAL);
            if (!result.found()) return;

            int slot = result.slot();
            boolean switched = false;

            if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
                if (autoSwitch.is(SwitchMode.None)) return;

                switch (autoSwitch.get()) {
                    case Silent -> {
                        InvUtil.swap(slot, true);
                        switched = true;
                    }
                    case InvSilent -> switched = InvUtil.invSwitch(slot);
                    default -> InvUtil.swap(slot, false);
                }
            }

            if (rotate.get()) {
                Vector2f rotation = RotationUtil.calculate(bestPos.toCenterPos().add(0, 0.5, 0));
                RotationManager.setRotations(rotation, rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
                isRotating = true;
            }

            BlockHitResult hitResult = new BlockHitResult(bestPos.toCenterPos().add(0, 1, 0), Direction.UP, bestPos, false);
            Hand hand = result.getHand() == null ? Hand.MAIN_HAND : result.getHand();
            ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, hand, hitResult);

            if (actionResult.isAccepted()) {
                if (swingHand.get()) mc.player.swingHand(hand, true);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
            }

            renderPos = bestPos;
            renderDamage = bestDamage;
            placeTimer.reset();

            if (switched) {
                switch (autoSwitch.get()) {
                    case Silent -> InvUtil.swapBack();
                    case InvSilent -> InvUtil.invSwapBack();
                }
            }
        }
    }

    private boolean isValidBaseBlock(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN && mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
            return false;
        }

        BlockPos up = pos.up();
        if (!mc.world.isAir(up)) return false;

        if (houyuepingMode.get()) {
            return mc.world.isAir(up.up());
        }
        return true;
    }

    private EndCrystalEntity getCrystalAt(BlockPos pos) {
        BlockPos up = pos.up();
        Box box = new Box(up);
        List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(EndCrystalEntity.class, box, Entity::isAlive);
        return crystals.isEmpty() ? null : crystals.getFirst();
    }

    private PlayerEntity getTarget() {
        PlayerEntity target = null;
        double distance = targetRange.get();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > distance) continue;

            target = player;
            distance = mc.player.distanceTo(player);
        }
        return target;
    }

    private float calculateDamage(PlayerEntity entity, Vec3d crystalPos) {
        if (extrapolation.get() && entity != mc.player) {
            Box predicted = Managers.EXTRAPOLATION.extrapolate((AbstractClientPlayerEntity) entity, extrapolationTicks.get(), smooth.get());
            if (predicted != null) {
                Box oldBox = entity.getBoundingBox();
                Vec3d oldPos = entity.getPos();

                entity.setBoundingBox(predicted);
                entity.setPos(predicted.getCenter().x, predicted.minY, predicted.getCenter().z);

                float damage = DamageUtil.calculateCrystalDamage(entity, crystalPos);

                entity.setBoundingBox(oldBox);
                entity.setPos(oldPos.x, oldPos.y, oldPos.z);

                return damage;
            }
        }
        return DamageUtil.calculateCrystalDamage(entity, crystalPos);
    }

    private long getAttackDelay(PlayerEntity target) {
        if (target != null && (target.getHealth() + target.getAbsorptionAmount() <= forcePlaceHealth.get())) {
            return forcePlaceDelay.get().longValue();
        }
        return attackDelay.get().longValue();
    }

    private double getMinDamage(PlayerEntity target) {
        if (target.getHealth() + target.getAbsorptionAmount() <= forcePlaceHealth.get()) {
            return 1.0;
        }
        return minDamage.get();
    }

    private List<BlockPos> getSphere(double range) {
        List<BlockPos> list = new ArrayList<>();
        BlockPos center = mc.player.getBlockPos();
        int r = (int) Math.ceil(range);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.player.squaredDistanceTo(pos.toCenterPos()) <= range * range) {
                        list.add(pos);
                    }
                }
            }
        }
        return list;
    }
}