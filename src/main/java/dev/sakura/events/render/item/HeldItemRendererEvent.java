package dev.sakura.events.render.item;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;

public class HeldItemRendererEvent {
    private final MatrixStack matrices;
    private final Hand hand;

    public HeldItemRendererEvent(MatrixStack matrices, Hand hand) {
        this.matrices = matrices;
        this.hand = hand;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public Hand getHand() {
        return hand;
    }
}