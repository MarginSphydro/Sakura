package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.manager.Managers;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.CombatUtil;
import dev.sakura.utils.combat.DamageUtil;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.render.Render3DUtil;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.rotation.RotationUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.utils.world.BlockUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.awt.*;

public class AnchorAura extends Module {
    private final EnumValue<Page> page = new EnumValue<>("Page", Page.General);
    private final NumberValue<Double> targetRange = new NumberValue<>("Target Range", 8.0, 1.0, 12.0, 1.0, () -> page.is(Page.General));
    private final NumberValue<Double> placeRange = new NumberValue<>("Place Range", 5.0, 1.0, 6.0, 1.0, () -> page.is(Page.General));
    private final BoolValue usingPause = new BoolValue("Using Pause", true, () -> page.is(Page.General));
    private final BoolValue inventorySwap = new BoolValue("Inventory Swap", true, () -> page.is(Page.General));
    private final BoolValue swingHand = new BoolValue("Swing", true, () -> page.is(Page.General));
    private final NumberValue<Double> placeDelay = new NumberValue<>("Place Delay", 0.0, 0.0, 20.0, 1.0, () -> page.is(Page.General));
    private final NumberValue<Double> updateDelay = new NumberValue<>("Update Delay", 6.0, 0.0, 20.0, 1.0, () -> page.is(Page.General));

    private final NumberValue<Double> minDamage = new NumberValue<>("Min Damage", 4.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));
    private final NumberValue<Double> breakMin = new NumberValue<>("Break Min Damage", 4.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));
    private final NumberValue<Double> minHeadDamage = new NumberValue<>("Min Head Damage", 7.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));
    private final NumberValue<Double> minPrefer = new NumberValue<>("Min Prefer Damage", 7.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));
    private final NumberValue<Double> maxSelfDamage = new NumberValue<>("Max Self Damage", 8.0, 0.0, 36.0, 0.1, () -> page.is(Page.Calc));

    private final BoolValue rotate = new BoolValue("Rotate", true, () -> page.is(Page.Rotate));
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("Rotation Speed", 10, 0, 10, 1, () -> page.is(Page.Rotate) && rotate.get());
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("Back Speed", 10, 0, 10, 1, () -> page.is(Page.Rotate) && rotate.get());

    private final BoolValue render = new BoolValue("Render", true, () -> page.is(Page.Render));
    private final ColorValue boxColor = new ColorValue("BoxColor", new Color(255, 255, 255, 255), () -> page.is(Page.Render) && render.get());
    private final ColorValue fillColor = new ColorValue("FillColor", new Color(255, 255, 255, 50), () -> page.is(Page.Render) && render.get());

    public AnchorAura() {
        super("AnchorAura", Category.Combat);
    }

    public PlayerEntity lastTarget = null;
    public double lastDamage;
    public BlockPos currentPos = null;
    BlockPos tempPos = null;

    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil calcTimer = new TimerUtil();
    private boolean isRotating = false;

    @Override
    public void onDisable() {
        currentPos = null;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (currentPos != null) {
            Render3DUtil.drawFilledBox(event.getMatrices(), new Box(currentPos), fillColor.get());
            Render3DUtil.drawBoxOutline(event.getMatrices(), new Box(currentPos), boxColor.get().getRGB(), 1.5f);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;
        update();
        setSuffix(lastTarget != null && currentPos != null && lastDamage > 0.0 ? lastTarget.getName().getString() + ", " + lastDamage : null);
        isRotating = false;
        if (mc.player.isSneaking()) return;
        if (usingPause.get() && mc.player.isUsingItem()) return;
        if (currentPos != null) {
            doAnchor(currentPos);
        }
        if (!isRotating && rotate.get()) {
            Managers.ROTATION.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
        }
    }

    private void update() {
        if (calcTimer.delay(updateDelay.get().floatValue())) {
            double placeDamage = minDamage.get();
            double breakDamage = breakMin.get();
            boolean anchorFound = false;
            tempPos = null;
            calcTimer.reset();
            for (PlayerEntity target : CombatUtil.getEnemies(targetRange.get())) {
                BlockPos blockPos = target.getBlockPos().up(2);
                if (canPlace(blockPos) || mc.world.getBlockState(blockPos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    double damage;
                    if ((damage = DamageUtil.calculateAnchorDamage(target, blockPos)) > minHeadDamage.get()) {
                        double selfDamage = DamageUtil.calculateAnchorDamage(mc.player, blockPos);
                        if (selfDamage > maxSelfDamage.get()) continue;
                        lastTarget = target;
                        lastDamage = damage;
                        tempPos = blockPos;
                        //break;
                    }
                }
                if (tempPos == null) {
                    for (BlockPos pos : BlockUtil.getSphere(placeRange.get().floatValue())) {
                        if (mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) {
                            if (anchorFound) continue;
                            if (!canPlace(pos)) continue;
                            double damage;
                            if ((damage = DamageUtil.calculateAnchorDamage(target, pos)) >= placeDamage) {
                                double selfDamage = DamageUtil.calculateAnchorDamage(mc.player, pos);
                                if (selfDamage > maxSelfDamage.get()) continue;
                                lastTarget = target;
                                lastDamage = damage;
                                placeDamage = damage;
                                tempPos = pos;
                                //break;
                            }
                        } else {
                            double damage;
                            if ((damage = DamageUtil.calculateAnchorDamage(target, pos)) >= breakDamage) {
                                double selfDamage = DamageUtil.calculateAnchorDamage(mc.player, pos);
                                if (selfDamage > maxSelfDamage.get()) continue;
                                if (damage >= minPrefer.get()) anchorFound = true;
                                if (!anchorFound && damage < placeDamage) {
                                    continue;
                                }
                                lastTarget = target;
                                lastDamage = damage;
                                breakDamage = damage;
                                tempPos = pos;
                                //break;
                            }
                        }
                    }
                }
            }
        }
        currentPos = tempPos;
    }

    private void doAnchor(BlockPos pos) {
        int anchor = inventorySwap.get() ? InvUtil.find(Items.RESPAWN_ANCHOR).slot() : InvUtil.findInHotbar(Items.RESPAWN_ANCHOR).slot();
        int glowstone = inventorySwap.get() ? InvUtil.find(Items.GLOWSTONE).slot() : InvUtil.findInHotbar(Items.GLOWSTONE).slot();
        int unBlock = inventorySwap.get() ? anchor : InvUtil.findInHotbar(itemStack -> !(itemStack.getItem() instanceof BlockItem)).slot();
        int oldSlot = mc.player.getInventory().selectedSlot;
        if (anchor == -1 || glowstone == -1 || unBlock == -1) return;
        if (!canPlace(pos)) return;
        if (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).isReplaceable()) {
            place(pos, anchor);
        } else if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
            if (mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0) {
                place(pos, glowstone);
            } else {
                place(pos, unBlock);
            }
        }
        if (!inventorySwap.get()) InvUtil.swap(oldSlot, false);
    }

    private boolean canPlace(BlockPos pos) {
        Direction side = BlockUtil.getPlaceSide(pos);
        if (side == null) return false;
        Box box = new Box(pos);
        return mc.world.getOtherEntities(null, box, e -> e instanceof PlayerEntity).isEmpty();
    }

    private void place(BlockPos pos, int slot) {
        if (placeTimer.delay(placeDelay.get().floatValue())) {
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side == null) return;
            boolean switched = false;
            boolean usedInvSwitch = false;
            if (inventorySwap.get()) {
                if (slot < 9) {
                    InvUtil.swap(slot, true);
                    switched = true;
                } else {
                    switched = InvUtil.invSwap(slot);
                    usedInvSwitch = true;
                }
            } else {
                InvUtil.swap(slot, true);
                switched = true;
            }
            if (rotate.get()) {
                Managers.ROTATION.setRotations(RotationUtil.calculate(pos.offset(side)), rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
                isRotating = true;
            }
            BlockHitResult hitResult = new BlockHitResult(pos.toCenterPos(), side, pos, false);
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

    public enum Page {
        General,
        Calc,
        Rotate,
        Render
    }
}