package dev.sakura.module.impl.movement.velocity;

import dev.sakura.events.client.TickEvent;
import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;

public class Velocity extends Module {

    private final EnumValue<VelocityMode> mode = new EnumValue<>("Mode", VelocityMode.Cancel);

    public final NumberValue<Integer> motionX = new NumberValue<>("MotionX", 100, 0, 100, 1, () -> mode.is(VelocityMode.Normal));
    public final NumberValue<Integer> motionY = new NumberValue<>("MotionY", 100, 0, 100, 1, () -> mode.is(VelocityMode.Normal));
    public final NumberValue<Integer> motionZ = new NumberValue<>("MotionZ", 100, 0, 100, 1, () -> mode.is(VelocityMode.Normal));

    public final NumberValue<Integer> attackCount = new NumberValue<>("AttackCounts", 3, 1, 5, 1, () -> mode.is(VelocityMode.HeypixelReduce));
    public boolean velocityInput;
    public double velocityX;
    public double velocityY;
    public double velocityZ;

    public Velocity() {
        super("Velocity", Category.Movement);
    }

    private enum VelocityMode {
        Normal,
        Cancel,
        GrimFull,
        HeypixelReduce
    }

    @Override
    protected void onEnable() {
        velocityInput = false;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null || event.getType() != EventType.RECEIVE) return;

        switch (mode.get()) {
            case VelocityMode.Cancel -> VelocityCancel.onPacket(this, event);
            case VelocityMode.Normal -> VelocityNormal.onPacket(this, event);
            case VelocityMode.HeypixelReduce -> VelocityHeypixelReduce.onPacket(this, event);
        }
    }

    @EventHandler
    public void onPreTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        switch (mode.get()) {
            case VelocityMode.HeypixelReduce -> VelocityHeypixelReduce.onPreTick(this);
        }
    }
}
