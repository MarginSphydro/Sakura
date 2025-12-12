package dev.sakura.events.player;

public class StrafeEvent {
    private float yaw;

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public StrafeEvent(float yaw) {
        this.yaw = yaw;
    }
}
