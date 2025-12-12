package dev.sakura.events.player;

public class JumpEvent {
    private float yaw;

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        return this.yaw;
    }

    public JumpEvent(float yaw) {
        this.yaw = yaw;
    }
}
