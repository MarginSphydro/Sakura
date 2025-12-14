package dev.sakura.module.impl.combat;

import dev.sakura.events.client.GameJoinEvent;
import dev.sakura.events.client.TickEvent;
import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.client.ChatUtils;
import dev.sakura.values.impl.BoolValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.item.Items;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Velocity module that reduces knockback by intercepting velocity packets
 * and implementing advanced anti-velocity techniques.
 */
public class Velocity extends Module {

    public static Stage stage = Stage.IDLE;
    public static int grimTick = -1;
    public static int debugTick = 10;
    // Task queue for delayed packet processing
    private final List<Runnable> skipTasks = new CopyOnWriteArrayList<>();
    private final BoolValue loggingConfig = new BoolValue("Logging", false);
    LinkedBlockingDeque<Packet<ClientPlayPacketListener>> inBound = new LinkedBlockingDeque<>();
    Packet<?> velocityPacket;
    BlockPosWithFacing pos;
    private BlockHitResult result = null;

    public Velocity() {
        super("Velocity", Category.Combat);
    }

    @Override
    protected void onEnable() {
        reset();
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private boolean shouldAvoidInteraction(Block block) {
        return block instanceof ChestBlock
                || block instanceof CraftingTableBlock
                || block instanceof FurnaceBlock
                || block instanceof EnderChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof AnvilBlock
                || block instanceof EnchantingTableBlock
                || block instanceof BrewingStandBlock
                || block instanceof BeaconBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof LecternBlock
                || block instanceof CartographyTableBlock
                || block instanceof FletchingTableBlock
                || block instanceof SmithingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof LoomBlock
                || block instanceof GrindstoneBlock
                || block instanceof ComposterBlock
                || block instanceof CauldronBlock
                || block instanceof BedBlock
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof NoteBlock;
    }

    public void reset() {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            stage = Stage.IDLE;
            grimTick = -1;
            debugTick = 0;
            processPackets();
            skipTasks.clear();
        }
    }

    public void processPackets() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            inBound.clear();
        } else {
            Packet<ClientPlayPacketListener> packet;
            while ((packet = inBound.poll()) != null) {
                try {
                    packet.apply(mc.getNetworkHandler());
                } catch (Exception e) {
                    e.printStackTrace();
                    inBound.clear();
                    break;
                }
            }
        }
    }

    public Direction checkBlock(Vec3d baseVec, BlockPos bp) {
        if (!(mc.world.getBlockState(bp).getBlock() instanceof AirBlock)) {
            return null;
        } else {
            Vec3d center = new Vec3d((double) bp.getX() + 0.5, (float) bp.getY() + 0.5F, (double) bp.getZ() + 0.5);
            Direction sbface = Direction.DOWN;
            Vec3d hit = center.add(
                    new Vec3d((double) sbface.getOffsetX() * 0.5, (double) sbface.getOffsetY() * 0.5, (double) sbface.getOffsetZ() * 0.5)
            );
            BlockPos baseBlock = bp.offset(sbface);
            BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
            if (!mc.world.getBlockState(po).isSideSolidFullSquare(mc.world, po, sbface)) {
                return null;
            } else {
                Vec3d relevant = hit.subtract(baseVec);
                if (relevant.lengthSquared() <= 20.25 && relevant.normalize().dotProduct(new Vec3d(sbface.getOffsetX(), sbface.getOffsetY(), sbface.getOffsetZ()).normalize()) >= 0.0) {
                    pos = new BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
                    return sbface.getOpposite();
                } else {
                    return null;
                }
            }
        }
    }

    private void log(String message) {
        if (loggingConfig.get()) {
            ChatUtils.addChatMessage("[Velocity] " + message);
        }
    }

    @EventHandler
    public void onGameJoin(GameJoinEvent event) {
        reset();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player != null && mc.getNetworkHandler() != null && mc.interactionManager != null) {
            
            // Execute one skip task per tick if available
            if (!skipTasks.isEmpty()) {
                skipTasks.remove(0).run();
            }

            if (mc.player.isDead()
                    || !mc.player.isAlive()
                    || mc.player.getHealth() <= 0.0F
                    || mc.currentScreen instanceof ProgressScreen
                    || mc.currentScreen instanceof DeathScreen) {
                reset();
            }

            if (debugTick > 0) {
                debugTick--;
                if (debugTick == 0) {
                    processPackets();
                    stage = Stage.IDLE;
                }
            } else {
                stage = Stage.IDLE;
            }

            if (grimTick > 0) {
                grimTick--;
            }

            // Advanced velocity handling logic with rotation
            float yaw = mc.player.getYaw(); // Fallback to client yaw as we don't have RotationManager
            float pitch = 89.79F;

            // Custom raycast with rotation
            BlockHitResult blockRayTraceResult = customRaycast(3.7F, yaw, pitch);

            if (stage == Stage.TRANSACTION
                    && grimTick == 0
                    && blockRayTraceResult != null
                    && !mc.world.isAir(blockRayTraceResult.getBlockPos())
                    && mc.player.getBoundingBox().intersects(new Box(blockRayTraceResult.getBlockPos().up()))) {

                Block targetBlock = mc.world.getBlockState(blockRayTraceResult.getBlockPos()).getBlock();
                if (shouldAvoidInteraction(targetBlock)) {
                    return;
                }

                result = new BlockHitResult(blockRayTraceResult.getPos(), blockRayTraceResult.getSide(), blockRayTraceResult.getBlockPos(), false);

                processPackets();
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);

                // Add skip task placeholder
                skipTasks.add(() -> {
                });

                // Queue rotation packets for next 100 ticks
                for (int i = 2; i <= 100; i++) {
                    skipTasks.add(() -> {
                        if (mc.player != null && mc.getNetworkHandler() != null) {
                            float currentYaw = mc.player.getYaw();
                            float currentPitch = mc.player.getPitch();
                            if (currentYaw != yaw || currentPitch != pitch) {
                                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(currentYaw, currentPitch, mc.player.isOnGround(), mc.player.horizontalCollision));
                            }
                        }
                    });
                }

                debugTick = 20;
                stage = Stage.BLOCK;
                grimTick = 0;
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.RECEIVE) return;

        if (mc.player != null && mc.getNetworkHandler() != null && mc.interactionManager != null && !mc.player.isUsingItem()) {
            
            if (mc.player.age < 20) {
                reset();
            } else if (!mc.player.isDead()
                    && mc.player.isAlive()
                    && !(mc.player.getHealth() <= 0.0F)
                    && !(mc.currentScreen instanceof ProgressScreen)
                    && !(mc.currentScreen instanceof DeathScreen)) {
                Packet<?> packet = event.getPacket();
                if (packet instanceof GameJoinS2CPacket) {
                    reset();
                } else {
                    if (debugTick > 0 && mc.player.age > 20) {
                        if (stage == Stage.BLOCK
                                && packet instanceof BlockUpdateS2CPacket cbu
                                && result != null
                                && result.getBlockPos().equals(cbu.getPos())) {
                            processPackets();
                            skipTasks.clear();
                            debugTick = 0;
                            result = null;
                            return;
                        }

                        if (!(packet instanceof GameMessageS2CPacket) && !(packet instanceof WorldTimeUpdateS2CPacket)) {
                            event.setCancelled(true);
                            // Unchecked cast warning suppression
                            try {
                                inBound.add((Packet<ClientPlayPacketListener>) packet);
                            } catch (ClassCastException ignored) {}
                            return;
                        }
                    }

                    if (packet instanceof EntityVelocityUpdateS2CPacket packetEntityVelocity) {
                        if (packetEntityVelocity.getEntityId() != mc.player.getId()) {
                            return;
                        }

                        if (packetEntityVelocity.getVelocityY() < 0 || mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                            event.setCancelled(true);
                            return;
                        }

                        grimTick = 2;
                        debugTick = 100;
                        stage = Stage.TRANSACTION;
                        event.setCancelled(true);
                    }
                }
            } else {
                reset();
            }
        }
    }

    /**
     * Custom raycast with specific yaw and pitch
     */
    private BlockHitResult customRaycast(float distance, float yaw, float pitch) {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        Vec3d eyePos = mc.player.getEyePos();
        
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = (float)Math.cos(g);
        float i = (float)Math.sin(g);
        float j = (float)Math.cos(f);
        float k = (float)Math.sin(f);
        Vec3d rotationVec = new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
        
        Vec3d endPos = eyePos.add(rotationVec.multiply(distance));

        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                eyePos,
                endPos,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    public enum Stage {
        TRANSACTION,
        ROTATION,
        BLOCK,
        IDLE
    }

    public static class BlockPosWithFacing {
        public final BlockPos pos;
        public final Direction facing;

        public BlockPosWithFacing(BlockPos pos, Direction facing) {
            this.pos = pos;
            this.facing = facing;
        }
    }
}
