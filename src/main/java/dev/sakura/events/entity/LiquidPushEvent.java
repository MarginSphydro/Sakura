package dev.sakura.events.entity;

import dev.sakura.events.Cancellable;
import net.minecraft.entity.Entity;

public class LiquidPushEvent extends Cancellable {
    private final Entity entity;

    public LiquidPushEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
