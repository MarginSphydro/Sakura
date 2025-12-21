package dev.sakura.module.impl.movement;

import dev.sakura.Sakura;
import dev.sakura.events.client.TickEvent;
import dev.sakura.events.entity.BlockPushEvent;
import dev.sakura.events.entity.EntityPushEvent;
import dev.sakura.events.entity.LiquidPushEvent;
import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.manager.impl.RotationManager;
import dev.sakura.mixin.accessor.IAccessorBundlePacket;
import dev.sakura.mixin.accessor.IEntityVelocityUpdateS2CPacket;
import dev.sakura.mixin.accessor.IExplosionS2CPacket;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.rotation.MovementFix;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.utils.vector.Vector2f;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Velocity extends Module {
    public enum Mode {
        Vanilla, Walls, Grim
    }

    private final EnumValue<Mode> mode = new EnumValue<>("Mode", Mode.Walls);
    private final NumberValue<Double> horizontalPercent = new NumberValue<>("Horizontal", 0.0, 0.0, 100.0, 1.0);
    private final NumberValue<Double> verticalPercent = new NumberValue<>("Vertical", 0.0, 0.0, 100.0, 1.0);
    private final BoolValue handleKnockback = new BoolValue("Knockback", true);
    private final BoolValue handleExplosions = new BoolValue("Explosion", true);
    private final BoolValue concealMotion = new BoolValue("Conceal", false);
    private final BoolValue requireGround = new BoolValue("GroundOnly", false);
    private final BoolValue cancelEntityPush = new BoolValue("EntityPush", true);
    private final BoolValue cancelBlockPush = new BoolValue("BlockPush", true);
    private final BoolValue cancelLiquidPush = new BoolValue("LiquidPush", true);
    private final BoolValue cancelFishHook = new BoolValue("RodPush", false);

    private boolean pendingConcealment = false;
    private boolean pendingVelocity = false;
    private final TimerUtil lagTimer = new TimerUtil();

    public Velocity() {
        super("Velocity", Category.Movement);
    }

    @Override
    protected void onEnable() {
        pendingVelocity = false;
        Sakura.EVENT_BUS.subscribe(this);
    }

    @Override
    protected void onDisable() {
        flushPendingVelocity();
        pendingConcealment = false;
        Sakura.EVENT_BUS.unsubscribe(this);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreTick(TickEvent.Pre event) {
        flushPendingVelocity();
        pendingConcealment = false;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE || mc.player == null || mc.world == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof PlayerPositionLookS2CPacket) {
            lagTimer.reset();
            if (concealMotion.get()) {
                pendingConcealment = true;
            }
        }

        if (packet instanceof EntityVelocityUpdateS2CPacket vel && handleKnockback.get()) {
            handleVelocityPacket(event, vel);
        } else if (packet instanceof ExplosionS2CPacket explosion && handleExplosions.get()) {
            handleExplosionPacket(event, explosion);
        } else if (packet instanceof BundleS2CPacket bundle) {
            handleBundlePacket(event, bundle);
        } else if (packet instanceof EntityStatusS2CPacket status
                && status.getStatus() == EntityStatuses.PULL_HOOKED_ENTITY
                && cancelFishHook.get()) {
            handleFishHookPacket(event, status);
        }
    }

    @EventHandler
    public void onEntityPush(EntityPushEvent event) {
        if (cancelEntityPush.get() && event.getEntity().equals(mc.player)) {
            event.cancel();
        }
    }

    @EventHandler
    public void onBlockPush(BlockPushEvent event) {
        if (cancelBlockPush.get()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onFluidPush(LiquidPushEvent event) {
        if (cancelLiquidPush.get()) {
            event.cancel();
        }
    }

    private void handleVelocityPacket(PacketEvent event, EntityVelocityUpdateS2CPacket packet) {
        if (packet.getEntityId() != mc.player.getId()) return;

        if (pendingConcealment && isZeroVelocity(packet)) {
            pendingConcealment = false;
            return;
        }

        switch (mode.get()) {
            case Vanilla -> processVelocityVanilla(event, packet);
            case Walls -> processVelocityWalls(event, packet);
            case Grim -> processVelocityGrim(event);
        }
    }

    private void handleExplosionPacket(PacketEvent event, ExplosionS2CPacket packet) {
        switch (mode.get()) {
            case Vanilla -> processExplosionVanilla(event, packet);
            case Walls -> processExplosionWalls(event, packet);
            case Grim -> processExplosionGrim(event);
        }
    }

    private void handleBundlePacket(PacketEvent event, BundleS2CPacket bundle) {
        List<Packet<?>> filtered = new ArrayList<>();
        boolean modified = false;

        for (Packet<?> packet : bundle.getPackets()) {
            if (packet instanceof ExplosionS2CPacket exp && handleExplosions.get()) {
                if (processBundleExplosion(filtered, exp)) modified = true;
            } else if (packet instanceof EntityVelocityUpdateS2CPacket vel && handleKnockback.get()) {
                if (processBundleVelocity(filtered, vel)) modified = true;
            } else {
                filtered.add(packet);
            }
        }

        if (modified) {
            ((IAccessorBundlePacket) bundle).setIterable(filtered);
        }
    }

    private void handleFishHookPacket(PacketEvent event, EntityStatusS2CPacket status) {
        Entity entity = status.getEntity(mc.world);
        if (entity instanceof FishingBobberEntity hook && hook.getHookedEntity() == mc.player) {
            event.cancel();
        }
    }

    private void processVelocityVanilla(PacketEvent event, EntityVelocityUpdateS2CPacket packet) {
        if (isNoVelocityConfigured()) {
            event.cancel();
        } else {
            scaleVelocityPacket(packet);
        }
    }

    private void processVelocityWalls(PacketEvent event, EntityVelocityUpdateS2CPacket packet) {
        if (!isPhased() || (requireGround.get() && !mc.player.isOnGround())) return;
        processVelocityVanilla(event, packet);
    }

    private void processVelocityGrim(PacketEvent event) {
        if (!lagTimer.hasReached(100)) return;
        event.cancel();
        pendingVelocity = true;
    }

    private void processExplosionVanilla(PacketEvent event, ExplosionS2CPacket packet) {
        if (isNoVelocityConfigured()) {
            event.cancel();
        } else {
            scaleExplosionPacket(packet);
        }
    }

    private void processExplosionWalls(PacketEvent event, ExplosionS2CPacket packet) {
        if (!isPhased()) return;
        processExplosionVanilla(event, packet);
    }

    private void processExplosionGrim(PacketEvent event) {
        if (!lagTimer.hasReached(100)) return;
        event.cancel();
        pendingVelocity = true;
    }

    private boolean processBundleExplosion(List<Packet<?>> filtered, ExplosionS2CPacket packet) {
        switch (mode.get()) {
            case Vanilla -> {
                if (!isNoVelocityConfigured()) scaleExplosionPacket(packet);
                else return true;
            }
            case Walls -> {
                if (!isPhased()) {
                    filtered.add(packet);
                    return false;
                }
                if (!isNoVelocityConfigured()) scaleExplosionPacket(packet);
                else return true;
            }
            case Grim -> {
                if (!lagTimer.hasReached(100)) {
                    filtered.add(packet);
                    return false;
                }
                pendingVelocity = true;
                return true;
            }
        }
        filtered.add(packet);
        return false;
    }

    private boolean processBundleVelocity(List<Packet<?>> filtered, EntityVelocityUpdateS2CPacket packet) {
        if (packet.getEntityId() != mc.player.getId()) {
            filtered.add(packet);
            return false;
        }

        switch (mode.get()) {
            case Vanilla -> {
                if (!isNoVelocityConfigured()) scaleVelocityPacket(packet);
                else return true;
            }
            case Walls -> {
                if (!isPhased() || (requireGround.get() && !mc.player.isOnGround())) {
                    filtered.add(packet);
                    return false;
                }
                if (!isNoVelocityConfigured()) scaleVelocityPacket(packet);
                else return true;
            }
            case Grim -> {
                if (!lagTimer.hasReached(100)) {
                    filtered.add(packet);
                    return false;
                }
                pendingVelocity = true;
                return true;
            }
        }

        filtered.add(packet);
        return false;
    }

    private void flushPendingVelocity() {
        if (!pendingVelocity) return;
        if (mode.get() == Mode.Grim) {
            sendRotationFix();
        }
        pendingVelocity = false;
    }

    private void sendRotationFix() {
        if (RotationManager.lastServerRotations == null) return;
        
        float yaw = RotationManager.lastServerRotations.x;
        float pitch = RotationManager.lastServerRotations.y;

        RotationManager.setRotations(new Vector2f(yaw, pitch), 100, MovementFix.NORMAL);
        
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(),
                Direction.DOWN
        ));
    }

    private boolean isZeroVelocity(EntityVelocityUpdateS2CPacket packet) {
        return packet.getVelocityX() == 0 && packet.getVelocityY() == 0 && packet.getVelocityZ() == 0;
    }

    private boolean isNoVelocityConfigured() {
        return horizontalPercent.get() == 0 && verticalPercent.get() == 0;
    }

    private void scaleVelocityPacket(EntityVelocityUpdateS2CPacket packet) {
        int scaledX = (int) (packet.getVelocityX() * (horizontalPercent.get() / 100.0));
        int scaledY = (int) (packet.getVelocityY() * (verticalPercent.get() / 100.0));
        int scaledZ = (int) (packet.getVelocityZ() * (horizontalPercent.get() / 100.0));

        ((IEntityVelocityUpdateS2CPacket) packet).setVelocityX(scaledX);
        ((IEntityVelocityUpdateS2CPacket) packet).setVelocityY(scaledY);
        ((IEntityVelocityUpdateS2CPacket) packet).setVelocityZ(scaledZ);
    }

    private void scaleExplosionPacket(ExplosionS2CPacket packet) {
        IExplosionS2CPacket accessor = (IExplosionS2CPacket) (Object) packet;
        Optional<Vec3d> knockback = accessor.getPlayerKnockback();
        if (knockback == null || knockback.isEmpty()) return;

        Vec3d original = knockback.get();
        Vec3d scaled = new Vec3d(
                original.x * (horizontalPercent.get() / 100.0),
                original.y * (verticalPercent.get() / 100.0),
                original.z * (horizontalPercent.get() / 100.0)
        );

        accessor.setPlayerKnockback(Optional.of(scaled));
    }

    private boolean isPhased() {
        Box box = mc.player.getBoundingBox();
        for (double x = box.minX; x < box.maxX; x += 0.2) {
            for (double y = box.minY; y < box.maxY; y += 0.2) {
                for (double z = box.minZ; z < box.maxZ; z += 0.2) {
                    if (mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).blocksMovement()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
