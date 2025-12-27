package dev.sakura.module.impl.movement;

import dev.sakura.events.client.TickEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.player.FindItemResult;
import dev.sakura.utils.player.InvUtil;
import dev.sakura.utils.player.MovementUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraBoost extends Module {
    private final NumberValue<Integer> delay = new NumberValue<>("Delay", "延迟", 15, 1, 100, 1);
    private final BoolValue onlyInAir = new BoolValue("OnlyInAir", "仅在空中", true);
    private final NumberValue<Double> speed = new NumberValue<>("Speed", "速度", 1.5, 0.1, 5.0, 0.1);

    private final TimerUtil timer = new TimerUtil();
    private Vec3d velocity = Vec3d.ZERO;

    public ElytraBoost() {
        super("ElytraBoost", "护甲飞行", Category.Movement);
    }

    @Override
    protected void onEnable() {
        velocity = Vec3d.ZERO;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) return;

        if (onlyInAir.get() && mc.player.isOnGround()) {
            velocity = Vec3d.ZERO;
            return;
        }

        FindItemResult elytra = InvUtil.find(Items.ELYTRA);
        if (!elytra.found()) return;

        InvUtil.moveItem(elytra.slot(), 6);
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.setFlag(7, true);

        boolean input = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0 || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
        velocity = input ? lerp(velocity, getTargetVelocity(), 0.4) : decay(velocity, 0.6);
        mc.player.setVelocity(velocity);

        if (timer.delay(delay.get())) {
            if (useFirework()) {
                timer.reset();
            }
        }

        InvUtil.moveItem(elytra.slot(), 6);
    }

    private Vec3d getTargetVelocity() {
        double[] dir = MovementUtil.getMotion(speed.get());
        double y = 0;
        
        if (mc.options.jumpKey.isPressed()) {
            y = speed.get();
        } else if (mc.options.sneakKey.isPressed()) {
            y = -speed.get();
        }
        
        return new Vec3d(dir[0], y, dir[1]);
    }

    private Vec3d lerp(Vec3d current, Vec3d target, double factor) {
        return current.add(target.subtract(current).multiply(factor));
    }

    private Vec3d decay(Vec3d vec, double factor) {
        Vec3d result = vec.multiply(factor);
        return result.lengthSquared() < 1e-6 ? Vec3d.ZERO : result;
    }

    private boolean useFirework() {
        FindItemResult firework = InvUtil.find(Items.FIREWORK_ROCKET);
        if (!firework.found()) return false;
        if (!InvUtil.invSwap(firework.slot())) return false;
        
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        mc.player.swingHand(Hand.MAIN_HAND);
        
        InvUtil.invSwapBack();
        return true;
    }
}
