package dev.sakura.module.impl.movement;

import dev.sakura.events.client.TickEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import net.minecraft.screen.PlayerScreenHandler;

import dev.sakura.utils.player.FindItemResult;
import dev.sakura.utils.player.InvUtil;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class ElytraBoost extends Module {
    private final NumberValue<Integer> delay = new NumberValue<>("Delay", "延迟", 15, 1, 100, 1);
    private final BoolValue onlyInAir = new BoolValue("OnlyInAir", "仅在空中", true);

    private final TimerUtil timer = new TimerUtil();

    public ElytraBoost() {
        super("ElytraBoost", "护甲飞行", Category.Movement);
    }

    private boolean wasOnGround;

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) return;

        if (onlyInAir.get() && mc.player.isOnGround()) {
            wasOnGround = true;
            timer.setTime(0);
            return;
        }

        if (wasOnGround) {
            timer.setTime(0);
            wasOnGround = false;
        }

        FindItemResult elytraResult = InvUtil.find(Items.ELYTRA);
        if (!elytraResult.found()) return;

        InvUtil.moveItem(elytraResult.slot(), 6);

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

        if (timer.delay(delay.get())) {
            FindItemResult fireworkResult = InvUtil.find(Items.FIREWORK_ROCKET);
            
            if (fireworkResult.found()) {
                boolean switchedFirework = InvUtil.invSwap(fireworkResult.slot());
                if (switchedFirework) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    InvUtil.invSwapBack();
                }
                timer.reset();
            }
        }

        InvUtil.moveItem(elytraResult.slot(), 6);
    }
}
