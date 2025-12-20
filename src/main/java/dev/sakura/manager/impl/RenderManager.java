package dev.sakura.manager.impl;

import dev.sakura.Sakura;
import dev.sakura.events.render.Render3DEvent;
import meteordevelopment.orbit.EventHandler;

public class RenderManager {
    public RenderManager() {
        Sakura.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        
    }
}
