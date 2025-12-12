package dev.sakura.events.entity;

import dev.sakura.events.Cancellable;

public final class LimbAnimationEvent extends Cancellable {
    float speed;

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }
}
