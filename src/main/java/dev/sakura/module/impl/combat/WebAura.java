package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.manager.Managers;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.CombatUtil;
import dev.sakura.utils.player.FindItemResult;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.rotation.RotationUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.utils.world.BlockUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class WebAura extends Module {
    public WebAura() {
        super("WebAura", "蜘蛛网光环", Category.Combat);
    }

    public enum SwitchMode {
        Normal,
        Silent
    }

    private final NumberValue<Double> targetRange = new NumberValue<>("Target Range", 8.0, 1.0, 12.0, 1.0);
    private final NumberValue<Integer> placeDelay = new NumberValue<>("Place Delay", 1, 0, 20, 1);
    private final BoolValue face = new BoolValue("Place on Face", true);
    private final BoolValue feet = new BoolValue("Place on Feet", true);
    private final BoolValue down = new BoolValue("Place on Down", false);
    private final NumberValue<Double> offset = new NumberValue<>("Offset", 0.25, 0.0, 0.3, 0.01);
    private final EnumValue<SwitchMode> autoSwitch = new EnumValue<>("Switch", SwitchMode.Normal);
    private final BoolValue swingHand = new BoolValue("Swing", true);
    private final BoolValue rotate = new BoolValue("Rotate", true);
    private final NumberValue<Integer> rotationSpeed = new NumberValue<>("Rotation Speed", 10, 0, 10, 1, rotate::get);
    private final NumberValue<Integer> rotationBackSpeed = new NumberValue<>("Back Speed", 10, 0, 10, 1, rotate::get);

    private final TimerUtil placeTimer = new TimerUtil();
    private boolean isRotating = false;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) return;

        isRotating = false;
        for (PlayerEntity target : CombatUtil.getEnemies(targetRange.get())) {
            if (target == null) {
                return;
            }
            if (placeTimer.delay(placeDelay.get().floatValue())) {
                for (double x : new double[]{0, offset.get(), -offset.get()}) {
                    for (double z : new double[]{0, offset.get(), -offset.get()}) {
                        BlockPos pos = BlockPos.ofFloored(target.getX() + x, target.getY(), target.getZ() + z);
                        if (face.get()) {
                            place(pos.up());
                        }
                        if (feet.get()) {
                            place(pos);
                        }
                        if (down.get()) {
                            place(pos.down());
                        }
                    }
                }
            }
        }
        if (!isRotating && rotate.get()) {
            Managers.ROTATION.setRotations(new Vector2f(mc.player.getYaw(), mc.player.getPitch()), rotationBackSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
        }
    }

    private void place(BlockPos pos) {
        if (BlockUtil.solid(pos)) {
            return;
        }
        FindItemResult result = InvUtil.findInHotbar(Items.COBWEB);
        if (!result.found()) return;
        int slot = result.slot();
        boolean switched = false;
        if (mc.player.getMainHandStack().getItem() != Items.COBWEB) {
            if (autoSwitch.is(SwitchMode.Silent)) {
                InvUtil.swap(slot, true);
                switched = true;
            } else {
                InvUtil.swap(slot, false);
            }
        }
        if (rotate.get()) {
            Managers.ROTATION.setRotations(RotationUtil.calculate(pos.toCenterPos()), rotationSpeed.get(), MovementFix.NORMAL, RotationManager.Priority.Medium);
            isRotating = true;
        }
        BlockHitResult hitResult = new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false);
        ActionResult actionResult = mc.interactionManager.interactBlock(mc.player, result.getHand(), hitResult);
        if (actionResult.isAccepted()) {
            if (swingHand.get()) mc.player.swingHand(result.getHand(), true);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(result.getHand()));
        }
        if (switched && autoSwitch.get() == SwitchMode.Silent) {
            InvUtil.swapBack();
        }
        placeTimer.reset();
    }
}
