package dev.sakura.client.module.impl.movement.velocity;

import dev.sakura.client.events.client.TickEvent;
import dev.sakura.client.events.packet.PacketEvent;
import dev.sakura.client.events.type.EventType;
import dev.sakura.client.module.Category;
import dev.sakura.client.module.Module;
import dev.sakura.client.values.impl.EnumValue;
import dev.sakura.client.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;

public class Velocity extends Module {

    private final EnumValue<VelocityMode> mode = new EnumValue<>("Mode", "模式", VelocityMode.Cancel);

    public final NumberValue<Integer> motionX = new NumberValue<>("MotionX", "水平X", 100, 0, 100, 1, () -> mode.is(VelocityMode.Normal));
    public final NumberValue<Integer> motionY = new NumberValue<>("MotionY", "垂直Y", 100, 0, 100, 1, () -> mode.is(VelocityMode.Normal));
    public final NumberValue<Integer> motionZ = new NumberValue<>("MotionZ", "水平Z", 100, 0, 100, 1, () -> mode.is(VelocityMode.Normal));

    public final NumberValue<Integer> attackCount = new NumberValue<>("AttackCounts", "攻击次数", 3, 1, 5, 1, () -> mode.is(VelocityMode.HeypixelReduce));
    public boolean velocityInput;
    public double velocityX;
    public double velocityY;
    public double velocityZ;

    public Velocity() {
        super("Velocity", "反击退", Category.Movement);
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

    @Override
    public String getSuffix() {
        return mode.get().name();
    }
}
