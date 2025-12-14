package dev.sakura.manager;

import dev.sakura.Sakura;
import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.player.LookAtEvent;
import dev.sakura.events.player.MotionEvent;
import dev.sakura.events.player.RotateEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.utils.math.MathUtils;
import dev.sakura.utils.time.TimerUtil;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

public class RotationManager {

    public static final RotationManager INSTANCE = new RotationManager();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public RotationManager() {
        Sakura.EVENT_BUS.subscribe(this);
    }

    public float nextYaw;
    public float nextPitch;
    public float rotationYaw = 0;
    public float rotationPitch = 0;
    public float lastYaw = 0;
    public float lastPitch = 0;
    public static final TimerUtil ROTATE_TIMER = new TimerUtil();
    public static Vec3d directionVec = null;
    public static boolean lastGround;

    // Placeholders for missing modules/settings
    // AntiCheat settings
    private boolean grimRotation = false; // Default off
    private boolean noSpamRotation = false;
    private float fov = 180f;
    private float steps = 1f; // Instant
    private boolean look = true;
    private float rotateTime = 1f;
    private boolean forceSync = false;

    // MoveFix settings
    private boolean moveFixOn = false;
    public static float fixRotation = 0f;
    public static float fixPitch = 0f;

    public void snapBack() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround(), mc.player.horizontalCollision));
    }

    public void lookAt(Vec3d directionVec) {
        rotationTo(directionVec);
        snapAt(directionVec);
    }

    public void lookAt(BlockPos pos, Direction side) {
        final Vec3d hitVec = pos.toCenterPos().add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5));
        lookAt(hitVec);
    }

    public void snapAt(float yaw, float pitch) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        setRenderRotation(yaw, pitch, true);
        if (grimRotation) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
        } else {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
        }
    }

    public void snapAt(Vec3d directionVec) {
        float[] angle = getRotation(directionVec);
        if (noSpamRotation) {
            if (MathHelper.angleBetween(angle[0], lastYaw) < fov && Math.abs(angle[1] - lastPitch) < fov) {
                return;
            }
        }
        snapAt(angle[0], angle[1]);
    }

    public float[] getRotation(Vec3d eyesPos, Vec3d vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }

    public float[] getRotation(Vec3d vec) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());
        return getRotation(eyesPos, vec);
    }

    public void rotationTo(Vec3d vec3d) {
        ROTATE_TIMER.reset();
        directionVec = vec3d;
    }

    public boolean inFov(Vec3d directionVec, float fov) {
        if (mc.player == null) return false;
        float[] angle = getRotation(new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()), directionVec);
        return inFov(angle[0], angle[1], fov);
    }

    public boolean inFov(float yaw, float pitch, float fov) {
        return MathHelper.angleBetween(yaw, rotationYaw) + Math.abs(pitch - rotationPitch) <= fov;
    }

    @EventHandler
    public void update(MotionEvent event) {
        if (event.getType() == EventType.PRE) {
            // Equivalent to MovementPacketsEvent
            if (moveFixOn) {
                event.setYaw(nextYaw);
                event.setPitch(nextPitch);
            } else {
                RotateEvent event1 = new RotateEvent(event.getYaw(), event.getPitch());
                Sakura.EVENT_BUS.post(event1);
                event.setYaw(event1.getYaw());
                event.setPitch(event1.getPitch());
            }
        } else if (event.getType() == EventType.POST) {
            // Equivalent to UpdateWalkingPlayerEvent (Post)
            // Also onUpdateWalkingPost logic
            setRenderRotation(lastYaw, lastPitch, false);

            if (moveFixOn) {
                updateNext();
            }
        }
    }

    private void updateNext() {
        if (mc.player == null) return;
        RotateEvent rotateEvent = new RotateEvent(mc.player.getYaw(), mc.player.getPitch());
        Sakura.EVENT_BUS.post(rotateEvent);
        if (rotateEvent.isModified()) {
            nextYaw = rotateEvent.getYaw();
            nextPitch = rotateEvent.getPitch();
        } else {
            float[] newAngle = injectStep(new float[]{rotateEvent.getYaw(), rotateEvent.getPitch()}, steps);
            nextYaw = newAngle[0];
            nextPitch = newAngle[1];
        }
        fixRotation = nextYaw;
        fixPitch = nextPitch;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLastRotation(RotateEvent event) {
        LookAtEvent lookAtEvent = new LookAtEvent();
        Sakura.EVENT_BUS.post(lookAtEvent);
        if (lookAtEvent.getRotation()) {
            float[] newAngle = injectStep(new float[]{lookAtEvent.getYaw(), lookAtEvent.getPitch()}, lookAtEvent.getSpeed());
            event.setYaw(newAngle[0]);
            event.setPitch(newAngle[1]);
        } else if (lookAtEvent.getTarget() != null) {
            float[] newAngle = injectStep(lookAtEvent.getTarget(), lookAtEvent.getSpeed());
            event.setYaw(newAngle[0]);
            event.setPitch(newAngle[1]);
        } else if (!event.isModified() && look) {
            if (directionVec != null && !ROTATE_TIMER.hasTimeElapsed((long) (rotateTime * 1000))) {
                float[] newAngle = injectStep(directionVec, steps);
                event.setYaw(newAngle[0]);
                event.setPitch(newAngle[1]);
            }
        }
    }

    public float[] injectStep(Vec3d vec, float steps) {
        if (mc.player == null) return new float[]{0, 0};
        float currentYaw = forceSync ? lastYaw : rotationYaw;
        float currentPitch = forceSync ? lastPitch : rotationPitch;

        float yawDelta = MathHelper.wrapDegrees((float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z - mc.player.getZ(), (vec.x - mc.player.getX()))) - 90) - currentYaw);
        float pitchDelta = ((float) (-Math.toDegrees(Math.atan2(vec.y - (mc.player.getPos().y + mc.player.getEyeHeight(mc.player.getPose())), Math.sqrt(Math.pow((vec.x - mc.player.getX()), 2) + Math.pow(vec.z - mc.player.getZ(), 2))))) - currentPitch);

        float angleToRad = (float) Math.toRadians(27 * (mc.player.age % 30));
        yawDelta = (float) (yawDelta + Math.sin(angleToRad) * 3) + MathUtils.getRandomInRange(-1f, 1f);
        pitchDelta = pitchDelta + MathUtils.getRandomInRange(-0.6f, 0.6f);

        if (yawDelta > 180)
            yawDelta = yawDelta - 180;

        float yawStepVal = 180 * steps;

        float clampedYawDelta = MathHelper.clamp(MathHelper.abs(yawDelta), -yawStepVal, yawStepVal);
        float clampedPitchDelta = MathHelper.clamp(pitchDelta, -45, 45);

        float newYaw = currentYaw + (yawDelta > 0 ? clampedYawDelta : -clampedYawDelta);
        float newPitch = MathHelper.clamp(currentPitch + clampedPitchDelta, -90.0F, 90.0F);

        double gcdFix = (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;

        return new float[]{(float) (newYaw - (newYaw - currentYaw) % gcdFix), (float) (newPitch - (newPitch - currentPitch) % gcdFix)};
    }

    public float[] injectStep(float[] angle, float steps) {
        if (steps < 0.01f) steps = 0.01f;
        if (steps > 1) steps = 1;
        if (steps < 1 && angle != null) {
            float packetYaw = forceSync ? lastYaw : rotationYaw;
            float diff = MathHelper.angleBetween(angle[0], packetYaw);
            if (Math.abs(diff) > 180 * steps) {
                angle[0] = (packetYaw + (diff * ((180 * steps) / Math.abs(diff))));
            }
            float packetPitch = forceSync ? lastPitch : rotationPitch;
            diff = angle[1] - packetPitch;
            if (Math.abs(diff) > 90 * steps) {
                angle[1] = (packetPitch + (diff * ((90 * steps) / Math.abs(diff))));
            }
        }
        return new float[]{angle[0], angle[1]};
    }

    @EventHandler(priority = -999)
    public void onPacketSend(PacketEvent event) {
        if (mc.player == null || event.isCancelled()) return;
        if (event.getPacket() instanceof PlayerMoveC2SPacket packet) {
            if (packet.changesLook()) {
                lastYaw = packet.getYaw(lastYaw);
                lastPitch = packet.getPitch(lastPitch);
                setRenderRotation(lastYaw, lastPitch, false);
            }
            lastGround = packet.isOnGround();
        }
    }

    private static float renderPitch;
    private static float renderYawOffset;
    private static float prevPitch;
    private static float prevRenderYawOffset;
    private static float prevRotationYawHead;
    private static float rotationYawHead;
    private int ticksExisted;

    @EventHandler(priority = EventPriority.HIGH)
    public void onReceivePacket(PacketEvent event) {
        if (mc.player == null) return;
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket packet) {
            Set<?> flags = readPacketFlags(packet);
            float yaw = readPacketYaw(packet);
            float pitch = readPacketPitch(packet);

            if (flags != null && flags.contains(PositionFlag.X_ROT)) {
                lastYaw = lastYaw + yaw;
            } else {
                lastYaw = yaw;
            }

            if (flags != null && flags.contains(PositionFlag.Y_ROT)) {
                lastPitch = lastPitch + pitch;
            } else {
                lastPitch = pitch;
            }
            setRenderRotation(lastYaw, lastPitch, true);
        }
    }

    private static Set<?> readPacketFlags(PlayerPositionLookS2CPacket packet) {
        Object flags = invokeFirst(packet,
                "getFlags",
                "flags",
                "getRelative",
                "relative",
                "getPositionFlags",
                "positionFlags",
                "getRelativeArguments",
                "relativeArguments"
        );
        if (flags instanceof Set<?> set) return set;
        return null;
    }

    private static float readPacketYaw(PlayerPositionLookS2CPacket packet) {
        Object yaw = invokeFirst(packet, "getYaw", "yaw", "getPlayerYaw", "playerYaw");
        if (yaw instanceof Number n) return n.floatValue();
        return 0.0f;
    }

    private static float readPacketPitch(PlayerPositionLookS2CPacket packet) {
        Object pitch = invokeFirst(packet, "getPitch", "pitch", "getPlayerPitch", "playerPitch");
        if (pitch instanceof Number n) return n.floatValue();
        return 0.0f;
    }

    private static Object invokeFirst(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                var m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public void setRenderRotation(float yaw, float pitch, boolean force) {
        if (mc.player == null) return;
        if (mc.player.age == ticksExisted && !force) {
            return;
        }

        ticksExisted = mc.player.age;
        prevPitch = renderPitch;

        prevRenderYawOffset = renderYawOffset;
        renderYawOffset = getRenderYawOffset(yaw, prevRenderYawOffset);

        prevRotationYawHead = rotationYawHead;
        rotationYawHead = yaw;

        renderPitch = pitch;
    }

    public static float getRenderPitch() {
        return renderPitch;
    }

    public static float getRotationYawHead() {
        return rotationYawHead;
    }

    public static float getRenderYawOffset() {
        return renderYawOffset;
    }

    public static float getPrevPitch() {
        return prevPitch;
    }

    public static float getPrevRotationYawHead() {
        return prevRotationYawHead;
    }

    public static float getPrevRenderYawOffset() {
        return prevRenderYawOffset;
    }

    private float getRenderYawOffset(float yaw, float offsetIn) {
        float result = offsetIn;
        float offset;

        double xDif = mc.player.getX() - mc.player.prevX;
        double zDif = mc.player.getZ() - mc.player.prevZ;

        if (xDif * xDif + zDif * zDif > 0.0025000002f) {
            offset = (float) MathHelper.atan2(zDif, xDif) * 57.295776f - 90.0f;
            float wrap = MathHelper.abs(MathHelper.wrapDegrees(yaw) - offset);
            if (95.0F < wrap && wrap < 265.0F) {
                result = offset - 180.0F;
            } else {
                result = offset;
            }
        }

        if (mc.player.handSwingProgress > 0.0F) {
            result = yaw;
        }

        result = offsetIn + MathHelper.wrapDegrees(result - offsetIn) * 0.3f;
        offset = MathHelper.wrapDegrees(yaw - result);

        if (offset < -75.0f) {
            offset = -75.0f;
        } else if (offset >= 75.0f) {
            offset = 75.0f;
        }

        result = yaw - offset;
        if (offset * offset > 2500.0f) {
            result += offset * 0.2f;
        }

        return result;
    }

    // Kept from previous version to maintain compatibility if used elsewhere
    public float getServerYaw() {
        if (mc.player == null) return 0.0f;
        return mc.player.getYaw(); // Or return rotationYaw/lastYaw?
    }

    public float getServerPitch() {
        if (mc.player == null) return 0.0f;
        return mc.player.getPitch();
    }
}
