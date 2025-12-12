package dev.sakura.events.input;

import dev.sakura.events.Cancellable;

public class MovementInputEvent extends Cancellable {

    boolean pressingForward;
    boolean pressingBack;
    boolean pressingLeft;
    boolean pressingRight;

    boolean jumping;
    boolean sneaking;

    float movementForward;
    float movementSideways;

    public MovementInputEvent(boolean pressingForward, boolean pressingBack, boolean pressingLeft, boolean pressingRight, boolean jumping, boolean sneaking, float movementForward, float movementSideways) {
        this.pressingForward = pressingForward;
        this.pressingBack = pressingBack;
        this.pressingLeft = pressingLeft;
        this.pressingRight = pressingRight;
        this.jumping = jumping;
        this.sneaking = sneaking;

        this.movementForward = movementForward;
        this.movementSideways = movementSideways;
    }

    public boolean isPressingForward() {
        return pressingForward;
    }

    public void setPressingForward(boolean pressingForward) {
        this.pressingForward = pressingForward;
    }

    public boolean isPressingBack() {
        return pressingBack;
    }

    public void setPressingBack(boolean pressingBack) {
        this.pressingBack = pressingBack;
    }

    public boolean isPressingLeft() {
        return pressingLeft;
    }

    public void setPressingLeft(boolean pressingLeft) {
        this.pressingLeft = pressingLeft;
    }

    public boolean isPressingRight() {
        return pressingRight;
    }

    public void setPressingRight(boolean pressingRight) {
        this.pressingRight = pressingRight;
    }

    public boolean isJumping() {
        return jumping;
    }

    public void setJumping(boolean jumping) {
        this.jumping = jumping;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public float getMovementForward() {
        return movementForward;
    }

    public void setMovementForward(float movementForward) {
        this.movementForward = movementForward;
    }

    public float getMovementSideways() {
        return movementSideways;
    }

    public void setMovementSideways(float movementSideways) {
        this.movementSideways = movementSideways;
    }
}
