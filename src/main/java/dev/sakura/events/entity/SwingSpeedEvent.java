package dev.sakura.events.entity;

import dev.sakura.events.Cancellable;

public final class SwingSpeedEvent extends Cancellable {
    int swingSpeed;
    boolean selfOnly;

    public void setSwingSpeed(int swingSpeed) {
        this.swingSpeed = swingSpeed;
    }

    public int getSwingSpeed() {
        return swingSpeed;
    }

    public void setSelfOnly(boolean selfOnly) {
        this.selfOnly = selfOnly;
    }

    public boolean getSelfOnly() {
        return selfOnly;
    }
}
