package dev.sakura.module.impl.render;

import dev.sakura.events.render.Render3DEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.animations.AnimationUtil;
import dev.sakura.values.Value;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class CameraClip extends Module {
    public CameraClip() {
        super("CameraClip", Category.Render);
    }

    private enum Mode {
        NORMAL,
        ACTION
    }

    private final Value<Boolean> disableFirstPers = new BoolValue("NoFirst", true);
    private final EnumValue<Mode> mode = new EnumValue<>("Mode", Mode.NORMAL);
    private final Value<Double> distance = new NumberValue<>("Distance", 3.5, 1.0, 20.0, 0.5, () -> mode.is(Mode.NORMAL));
    private final Value<Double> speed = new NumberValue<>("Speed", 10.0, 1.0, 50.0, 0.5, () -> mode.is(Mode.NORMAL));
    private final Value<Double> actionDistance = new NumberValue<>("ActionDistance", 4.0, 0.5, 20.0, 0.5, () -> mode.is(Mode.ACTION));
    private final Value<Double> smoothness = new NumberValue<>("Smoothness", 0.3, 0.1, 0.95, 0.01, () -> mode.is(Mode.ACTION));
    private final Value<Double> maxDistance = new NumberValue<>("MaxDistance", 20.0, 5.0, 50.0, 0.5, () -> mode.is(Mode.ACTION));
    private final Value<Double> rotationSmoothness = new NumberValue<>("RotationSmoothness", 0.15, 0.01, 0.5, 0.01, () -> mode.is(Mode.ACTION));
    private final Value<Double> rotationOffset = new NumberValue<>("RotationOffset", 2.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.ACTION));

    private Vec3d cameraPos;
    private int key = GLFW.GLFW_KEY_F6;
    private float animation;
    private Perspective lastPerspective = null;
    private float smoothYaw = 0f;
    private float smoothPitch = 0f;
    private float lastYaw = 0f;
    private float lastPitch = 0f;

    @Override
    public void onEnable() {
        if (mc.player != null) {
            cameraPos = mc.player.getPos();
            smoothYaw = mc.player.getYaw();
            smoothPitch = mc.player.getPitch();
            lastYaw = smoothYaw;
            lastPitch = smoothPitch;
        }
    }

    @Override
    public void onDisable() {
        cameraPos = null;
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        Perspective currentPerspective = mc.options.getPerspective();

        if (lastPerspective != null && lastPerspective != currentPerspective) {
            animation = (currentPerspective == Perspective.FIRST_PERSON) ? 1f : 0f;
        }

        lastPerspective = currentPerspective;

        if (mode.is(Mode.NORMAL)) {
            float normalSpeed = speed.getValue().floatValue();
            if (currentPerspective == Perspective.FIRST_PERSON)
                animation = AnimationUtil.fast(animation, 0f, normalSpeed);
            else animation = AnimationUtil.fast(animation, 1f, normalSpeed);
        } else if (mode.is(Mode.ACTION)) {
            if (currentPerspective == Perspective.FIRST_PERSON) animation = AnimationUtil.fast(animation, 0f, 10);
            else animation = AnimationUtil.fast(animation, 1f, 10);
        }
    }

    public float getDistance() {
        return 1f + ((distance.getValue().floatValue() - 1f) * animation);
    }

    public float getActionDistance() {
        return 1f + ((actionDistance.getValue().floatValue() - 1f) * animation);
    }

    public boolean isNormal() {
        return isEnabled() && mode.is(Mode.NORMAL);
    }

    public boolean isAction() {
        return isEnabled() && mode.is(Mode.ACTION);
    }

    private boolean firstPerson() {
        return mc.options != null && mc.options.getPerspective() == Perspective.FIRST_PERSON;
    }

    public Vec3d getCameraPos() {
        if (cameraPos == null) cameraPos = mc.player.getPos();

        if (firstPerson() && mc.player != null) {
            return new Vec3d(
                    mc.player.getX(),
                    mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                    mc.player.getZ()
            );
        }
        return cameraPos;
    }

    public void update(Vec3d playerPos) {
        if (cameraPos == null) {
            cameraPos = playerPos;
            smoothYaw = mc.player.getYaw();
            smoothPitch = mc.player.getPitch();
            lastYaw = smoothYaw;
            lastPitch = smoothPitch;
            return;
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float rotSmooth = rotationSmoothness.getValue().floatValue();
        smoothYaw += (currentYaw - smoothYaw) * rotSmooth;
        smoothPitch += (currentPitch - smoothPitch) * rotSmooth;

        float yawDelta = currentYaw - smoothYaw;
        float pitchDelta = currentPitch - smoothPitch;

        float offsetMultiplier = rotationOffset.getValue().floatValue();
        double yawRad = Math.toRadians(smoothYaw);

        double rotOffsetX = Math.sin(yawRad) * yawDelta * offsetMultiplier * 0.02;
        double rotOffsetY = pitchDelta * offsetMultiplier * 0.02;
        double rotOffsetZ = -Math.cos(yawRad) * yawDelta * offsetMultiplier * 0.02;

        double distance = cameraPos.distanceTo(playerPos);
        float maxDist = maxDistance.getValue().floatValue();

        if (distance > maxDist) {
            cameraPos = playerPos;
            return;
        }

        float smoothFactor = smoothness.getValue().floatValue();
        double dynamicFactor = smoothFactor * (1.0 - Math.exp(-distance / maxDist));

        double dx = playerPos.x - cameraPos.x + rotOffsetX;
        double dy = playerPos.y + mc.player.getEyeHeight(mc.player.getPose()) - cameraPos.y + rotOffsetY;
        double dz = playerPos.z - cameraPos.z + rotOffsetZ;

        cameraPos = new Vec3d(
                cameraPos.x + dx * dynamicFactor,
                cameraPos.y + dy * dynamicFactor,
                cameraPos.z + dz * dynamicFactor
        );

        lastYaw = currentYaw;
        lastPitch = currentPitch;
    }

    public boolean shouldModifyCamera() {
        return isEnabled() && mode.is(Mode.ACTION) && (!disableFirstPers.getValue() || !firstPerson());
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean getDisableFirstPers() {
        return disableFirstPers.getValue();
    }
}
