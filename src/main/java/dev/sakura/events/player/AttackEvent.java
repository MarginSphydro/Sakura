package dev.sakura.events.player;

import dev.sakura.events.Cancellable;
import dev.sakura.events.type.EventType;
import net.minecraft.entity.Entity;

public class AttackEvent extends Cancellable {
    public Entity target;
    public static EventType type;

    public final EventType stage;

    public AttackEvent(Entity target, EventType stage) {
        this.target = target;
        this.stage = stage;
    }

    public Entity getTarget() {
        return target;
    }

    public static EventType getType() {
        return type;
    }

    public EventType getStage() {
        return stage;
    }
}
