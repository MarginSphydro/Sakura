package dev.sakura.events.player;

import dev.sakura.events.Cancellable;
import net.minecraft.util.math.Vec3d;

public class LookAtEvent extends Cancellable {
    private Vec3d target;
    private float yaw;
    private float pitch;
    private boolean rotation;
    private float speed;

    public Vec3d getTarget() {
        return target;
    }

    public void setTarget(Vec3d target, float speed) {
        this.target = target;
        this.speed = speed;
        this.rotation = false;
    }

    public void setRotation(float yaw, float pitch, float speed) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.speed = speed;
        this.rotation = true;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean getRotation() {
        return rotation;
    }

    public float getSpeed() {
        return speed;
    }
}
