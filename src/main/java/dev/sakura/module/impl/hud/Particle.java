package dev.sakura.module.impl.hud;

import dev.sakura.module.impl.client.ClickGui;

import java.awt.*;

public class Particle {
    public float x, y;
    public float velocityX, velocityY;
    public float size;
    public Color color;
    public float alpha;
    public float life;
    public float maxLife;

    public Particle(float x, float y) {
        this.x = x;
        this.y = y;
        this.size = 1.0f + (float) (Math.random() * 2.0f);
        this.color = ClickGui.mainColor.get();
        this.alpha = 1.0f;
        this.maxLife = 100f + (float) (Math.random() * 100f);
        this.life = maxLife;
        float speed = 0.5f + (float) (Math.random() * 1.5f);
        double angle = Math.random() * Math.PI * 2;
        this.velocityX = (float) (Math.cos(angle) * speed);
        this.velocityY = (float) (Math.sin(angle) * speed);
    }

    public void update() {
        x += velocityX;
        y += velocityY;
        life -= 1.0f;
        alpha = life / maxLife;
    }

    public boolean isAlive() {
        return life > 0;
    }
}