package dev.sakura.events.render;


import net.minecraft.client.gui.DrawContext;

public class Render2DEvent {
    private final DrawContext context;

    public Render2DEvent(DrawContext context) {
        this.context = context;
    }

    public DrawContext getContext() {
        return this.context;
    }
}
