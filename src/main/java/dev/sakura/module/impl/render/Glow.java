package dev.sakura.module.impl.render;

import dev.sakura.events.render.Render2DEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.render.Shader2DUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class Glow extends Module {
    public static Glow INSTANCE;

    private final BoolValue players = new BoolValue("Players", true);
    private final BoolValue self = new BoolValue("Self", false);
    private final ColorValue color = new ColorValue("Color", new Color(255, 180, 220, 180));
    private final NumberValue<Double> blurStrength = new NumberValue<>("BlurStrength", 12.0, 4.0, 25.0, 1.0);
    private final NumberValue<Double> colorIntensity = new NumberValue<>("ColorIntensity", 0.4, 0.0, 1.0, 0.05);
    private final BoolValue nativeGlow = new BoolValue("NativeGlow", true);

    private final List<PlayerScreenData> playerScreenPositions = new ArrayList<>();

    public Glow() {
        super("Glow", "发光", Category.Render);
        INSTANCE = this;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        playerScreenPositions.clear();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!shouldGlow(player)) continue;

            float tickDelta = event.getTickDelta();
            double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
            double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
            double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());

            Box box = player.getBoundingBox();
            double height = box.getLengthY();
            double halfWidth = box.getLengthX() / 2;

            Vec3d[] corners = new Vec3d[]{
                    new Vec3d(x - halfWidth, y, z - halfWidth),
                    new Vec3d(x + halfWidth, y, z - halfWidth),
                    new Vec3d(x - halfWidth, y, z + halfWidth),
                    new Vec3d(x + halfWidth, y, z + halfWidth),
                    new Vec3d(x - halfWidth, y + height, z - halfWidth),
                    new Vec3d(x + halfWidth, y + height, z - halfWidth),
                    new Vec3d(x - halfWidth, y + height, z + halfWidth),
                    new Vec3d(x + halfWidth, y + height, z + halfWidth),
            };

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
            boolean anyVisible = false;

            for (Vec3d corner : corners) {
                Vec3d screen = worldToScreen(corner);
                if (screen != null && screen.z > 0 && screen.z < 1) {
                    minX = Math.min(minX, (float) screen.x);
                    maxX = Math.max(maxX, (float) screen.x);
                    minY = Math.min(minY, (float) screen.y);
                    maxY = Math.max(maxY, (float) screen.y);
                    anyVisible = true;
                }
            }

            if (anyVisible && minX < maxX && minY < maxY) {
                float padding = 5;
                playerScreenPositions.add(new PlayerScreenData(
                        minX - padding,
                        minY - padding,
                        maxX - minX + padding * 2,
                        maxY - minY + padding * 2,
                        player
                ));
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (playerScreenPositions.isEmpty()) return;

        MatrixStack matrices = event.getContext().getMatrices();

        for (PlayerScreenData data : playerScreenPositions) {
            Color c = color.get();
            float intensity = colorIntensity.get().floatValue();

            Color tintColor = new Color(
                    c.getRed(),
                    c.getGreen(),
                    c.getBlue(),
                    (int) (c.getAlpha() * intensity)
            );

            Shader2DUtil.drawRoundedBlur(matrices,
                    data.x, data.y, data.width, data.height,
                    Math.min(data.width, data.height) * 0.15f,
                    tintColor,
                    blurStrength.get().floatValue(),
                    0.9f);
        }
    }

    public boolean shouldGlow(Entity entity) {
        if (!isEnabled()) return false;

        if (entity instanceof PlayerEntity) {
            if (entity == mc.player) {
                return self.get();
            }
            return players.get();
        }

        return false;
    }

    public int getGlowColor(Entity entity) {
        Color c = color.get();
        return (255 << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }


    public boolean useNativeGlow() {
        return nativeGlow.get();
    }

    public Color getColor() {
        return color.get();
    }

    public float getColorIntensity() {
        return colorIntensity.get().floatValue();
    }

    private Vec3d worldToScreen(Vec3d pos) {
        Camera camera = mc.gameRenderer.getCamera();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        Vec3d camPos = camera.getPos();
        Vector3f camLook = camera.getHorizontalPlane();
        Vector3f camUp = camera.getVerticalPlane();
        Vector3f camLeft = new Vector3f();
        camLook.cross(camUp, camLeft);
        camLeft.normalize();

        float dx = (float) (pos.x - camPos.x);
        float dy = (float) (pos.y - camPos.y);
        float dz = (float) (pos.z - camPos.z);

        Vector3f toPos = new Vector3f(dx, dy, dz);

        float dotLook = toPos.dot(camLook);
        if (dotLook <= 0.01f) return null;

        float dotUp = toPos.dot(camUp);
        float dotLeft = toPos.dot(camLeft);

        float fov = mc.options.getFov().getValue().floatValue();
        float aspectRatio = (float) width / height;
        float tanHalfFov = (float) Math.tan(Math.toRadians(fov / 2.0));

        float screenX = width / 2f + (dotLeft / dotLook) / (tanHalfFov * aspectRatio) * (width / 2f);
        float screenY = height / 2f - (dotUp / dotLook) / tanHalfFov * (height / 2f);

        return new Vec3d(screenX, screenY, 1.0 / dotLook);
    }

    private static class PlayerScreenData {
        float x, y, width, height;
        PlayerEntity player;

        PlayerScreenData(float x, float y, float width, float height, PlayerEntity player) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.player = player;
        }
    }
}