package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.CombatUtil;
import dev.sakura.utils.combat.DamageUtil;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.rotation.RotationUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.utils.world.BlockUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

//todo: 未完成
public class AnchorAura extends Module {
    private final NumberValue<Double> targetRange = new NumberValue<>("Target Range", 8.0, 1.0, 12.0, 1.0);
    private final NumberValue<Double> placeRange = new NumberValue<>("Place Range", 5.0, 1.0, 6.0, 1.0);
    private final BoolValue usingPause = new BoolValue("Using Pause", true);
    private final BoolValue inventorySwap = new BoolValue("Inventory Swap", true);
    private final BoolValue swingHand = new BoolValue("Swing", true);
    private final NumberValue<Double> minDamage = new NumberValue<>("Min Damage", 8.0, 0.0, 36.0, 0.1);
    private final NumberValue<Double> maxSelfDamage = new NumberValue<>("Max Self Damage", 8.0, 0.0, 36.0, 0.1);
    private final NumberValue<Double> placeDelay = new NumberValue<>("Place Delay", 50.0, 0.0, 1000.0, 1.0);
    private final NumberValue<Double> updateDelay = new NumberValue<>("Update Delay", 50.0, 0.0, 1000.0, 1.0);
    private final BoolValue rotate = new BoolValue("Rotate", true);
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("Rotation Speed", 10, 0, 10, 1, rotate::get);
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("Back Speed", 10, 0, 10, 1, rotate::get);

    public AnchorAura() {
        super("AnchorAura", Category.Combat);
    }

    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil updateDelayTimer = new TimerUtil();
    private BlockPos tempPos = null;
    private boolean isRotating = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;
        isRotating = false;
        if (usingPause.get() && mc.player.isUsingItem()) return;
        if (updateDelayTimer.hasTimeElapsed(updateDelay.get().longValue())) {
            tempPos = null;
            for (PlayerEntity target : CombatUtil.getEnemies(targetRange.get())) {
                for (BlockPos pos : BlockUtil.getSphere(placeRange.get().floatValue())) {
                    double damage = DamageUtil.calculateAnchorDamage(target, pos);
                    double selfDamage = DamageUtil.calculateAnchorDamage(mc.player, pos);
                    if (selfDamage > maxSelfDamage.get()) continue;
                    if (damage < minDamage.get()) continue;
                    tempPos = pos;
                    updateDelayTimer.reset();
                }
            }
        }
        if (tempPos != null) {
            doAnchor(tempPos);
        }
        if (!isRotating && rotate.get()) {
            RotationManager.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
        }
    }

    private void doAnchor(BlockPos pos) {
        int anchor = inventorySwap.get() ? InvUtil.find(Items.RESPAWN_ANCHOR).slot() : InvUtil.findInHotbar(Items.RESPAWN_ANCHOR).slot();
        int glowstone = inventorySwap.get() ? InvUtil.find(Items.GLOWSTONE).slot() : InvUtil.findInHotbar(Items.GLOWSTONE).slot();
        int oldSlot = mc.player.getInventory().selectedSlot;
        if (anchor == -1 || glowstone == -1) return;
        if (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).isReplaceable()) {
            place(pos, anchor);
        } else if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            if (mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0) {
                place(pos, glowstone);
            } else {
                place(pos, anchor);
            }
        }
        if (!inventorySwap.get()) InvUtil.swap(oldSlot, false);
    }

    private void place(BlockPos pos, int slot) {
        if (placeTimer.hasTimeElapsed(placeDelay.get().longValue())) {
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) return;
            boolean switched = false;
            boolean usedInvSwitch = false;
            if (inventorySwap.get()) {
                if (slot < 9) {
                    InvUtil.swap(slot, true);
                    switched = true;
                } else {
                    switched = InvUtil.invSwitch(slot);
                    usedInvSwitch = true;
                }
            } else {
                InvUtil.swap(slot, true);
                switched = true;
            }
            if (rotate.get()) {
                RotationManager.setRotations(RotationUtil.calculate(pos.offset(side)), rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
                isRotating = true;
            }
            BlockHitResult hitResult = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
            ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            if (actionResult.isAccepted()) {
                if (swingHand.get()) mc.player.swingHand(Hand.MAIN_HAND, true);
                else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
            if (switched) {
                if (inventorySwap.get()) {
                    if (usedInvSwitch) InvUtil.invSwapBack();
                    else InvUtil.swapBack();
                } else {
                    InvUtil.swapBack();
                }
            }
            placeTimer.reset();
        }
    }
}
