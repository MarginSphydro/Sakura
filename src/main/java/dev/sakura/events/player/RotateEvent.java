package dev.sakura.events.player;

import dev.sakura.events.Cancellable;

public class RotateEvent extends Cancellable {
    private float yaw;
    private float pitch;
    private boolean modified;

    public RotateEvent(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        this.modified = true;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }
}
