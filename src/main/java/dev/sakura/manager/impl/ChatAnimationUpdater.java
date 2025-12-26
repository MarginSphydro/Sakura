package dev.sakura.manager.impl;

import dev.sakura.Sakura;
import dev.sakura.events.client.TickEvent;
import dev.sakura.utils.animations.ChatAnimationManager;
import meteordevelopment.orbit.EventHandler;

public class ChatAnimationUpdater {
    public ChatAnimationUpdater() {
        Sakura.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ChatAnimationManager.getInstance().update();
    }
}