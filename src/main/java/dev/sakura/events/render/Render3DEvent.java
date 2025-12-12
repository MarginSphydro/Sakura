package dev.sakura.events.render;

import net.minecraft.client.util.math.MatrixStack;

public class Render3DEvent {
    private final MatrixStack matrices;
    private final float tickDelta;

    public Render3DEvent(MatrixStack matrices, float tickDelta) {
        this.matrices = matrices;
        this.tickDelta = tickDelta;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    public static class Game extends Render3DEvent {
        public Game(MatrixStack matrices, float tickDelta) {
            super(matrices, tickDelta);
        }
    }

    public static class Hand extends Render3DEvent {
        public Hand(MatrixStack matrices, float tickDelta) {
            super(matrices, tickDelta);
        }
    }
}
