package dev.sakura.module.impl.combat;

import dev.sakura.events.input.MovementInputEvent;
import dev.sakura.events.misc.WorldLoadEvent;
import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.player.MotionEvent;
import dev.sakura.events.player.UpdateEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.packet.OldNaming;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.EnumValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * @Author：Gu-Yuemang
 * @Date：12/7/2025 2:29 PM
 */
public class AntiKnockback extends Module {

    public enum Mode {
        Jump, Reduce, Grim
    }

    public enum Mode2 {
        Jump2, Reduce2, Grim2
    }

    public enum Mode3 {
        Jump2, Reduce2, Grim2
    }

    public AntiKnockback() {
        super("AntiKnockback", Category.Combat);
    }

    private final EnumValue<Mode> modeValue = new EnumValue<>("Mode", Mode.Jump, Mode.class);
    private final BoolValue a = new BoolValue("A", false);
    private final EnumValue<Mode2> modeValue2 = new EnumValue<>("Mode2", Mode2.Jump2, Mode2.class);

    private final BoolValue b = new BoolValue("B", false);
    private final BoolValue c = new BoolValue("C", false);
    private final EnumValue<Mode3> modeValue3 = new EnumValue<>("Mode3", Mode3.Jump2, Mode3.class);

    private final BoolValue d = new BoolValue("D", false);
    public static boolean val;

    private int ccCooldown;
    private boolean stopMove;
    private boolean grimSend;
    private final TimerUtil timer = new TimerUtil();
    private int ticks;
    public boolean sb;
    public boolean s12;

    public int skipTicks;

    @Override
    public void onEnable() {
        timer.reset();
        val = false;
    }

    @Override
    public void onDisable() {
        rest();
    }

    @EventHandler
    private void onMotion(MotionEvent eventMotion) {
        if (mc.player == null) return;
        if (eventMotion.getType() == EventType.POST) return;
        if (modeValue.is(Mode.Jump)) {
            if (val) val = false;
        }
    }

    @EventHandler
    private void onKey(MovementInputEvent eventKeyMoveInput) {
        if (mc.player == null || mc.world == null) return;

        if (modeValue.is(Mode.Jump) || modeValue.is(Mode.Grim)) {
            if (val && mc.player.hasMovementInput()) {
                eventKeyMoveInput.setJumping(true);
            }
        }
    }

    @EventHandler
    private void onWorld(WorldLoadEvent eventWorld) {
        rest();

        ccCooldown = 0;
        ticks = 0;
    }

    @EventHandler
    private void onUpdate(UpdateEvent evenUpdate) {
        switch (modeValue.get()) {
            case Jump -> setSuffix("JumpReset");
            default -> setSuffix(modeValue.get().name());
        }

        if (mc.player == null || mc.world == null) return;

        switch (modeValue.get()) {
            case Jump -> {
                if (mc.currentScreen != null) {
                    return;
                }

                if (mc.player.hurtTime == 0) {
                    ticks = 0;
                    stopMove = false;
                }

                if (stopMove) {
                    ticks++;
                    if (ticks > 2) stopMove = false;
                }

                if (grimSend) {
                    sendPacket();
                    timer.reset();
                    grimSend = false;
                }
            }
            //我不知道我在干什么
            case Reduce -> {
                if (s12) {
                    if (mc.player.isOnGround()) {
                        mc.options.jumpKey.setPressed(true);
                        sb = true;

                        Vec3d velocity = mc.player.getVelocity();
                        mc.player.setVelocity(velocity.x * 0.6, velocity.y, velocity.z * 0.6);
                        s12 = false;
                    }
                }
            }
        }
        if (sb && !mc.player.isOnGround()) {
            mc.options.jumpKey.setPressed(false);
            sb = false;
        }
    }

    public boolean isVelocity(Packet<?> packet) {
        return (packet instanceof EntityS2CPacket velocity
                && velocity.id == mc.player.getId()
                && velocity.getDeltaY() > 0
                && (velocity.getDeltaX() != 0 || velocity.getDeltaZ() != 0));
    }

    @EventHandler
    private void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;
        if (ccCooldown > 0) {
            ccCooldown--;
            return;
        }

        if (isVelocity(packet)) {
            switch (modeValue.get()) {
                case Jump -> {
                    break;
                }
                /*case "Reduce" -> {
                    if (event.getPacket() instanceof EntityPositionS2CPacket packet1) {
                        if (packet1.entityId() == mc.player.getId()) {
                            if (KillAura.target != null) {
                                boolean iscnm = mc.player.isSprinting();

                                if (!iscnm) {
                                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                                }
                                for (int i = 0; i < 3; i++) {
                                    if (!iscnm) {
                                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                                        DebugUtil.sendMessage("AntiKnockback: " + s12 + " " + sb);
                                        s12 = true;
                                    }
                                }
                            }
                        }
                    }
                }*/
            }
        }
    }


    private void sendPacket() {
        skipTicks += 1;

        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, BlockPos.ofFloored(mc.player.getX(), mc.player.getY(), mc.player.getZ()), Direction.getFacing(lookVec.x, lookVec.y, lookVec.z).getOpposite()));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.player.networkHandler.sendPacket(OldNaming.C03PacketPlayer());
    }

    private void rest() {
        val = false;
    }
}
