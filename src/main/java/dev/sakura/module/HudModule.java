package dev.sakura.module;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import dev.sakura.Sakura;
import dev.sakura.module.impl.client.HudEditor;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.util.NanoVGHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.nanovg.NanoVG;

import java.awt.*;

/**
 * 子类在onRender中直接使用x, y, width, height进行绘制
 */
public abstract class HudModule extends Module {
    @Expose
    @SerializedName("hudX")
    protected float x;

    @Expose
    @SerializedName("hudY")
    protected float y;

    protected float width = 50;
    protected float height = 20;

    protected boolean dragging = false;
    protected float dragX, dragY;
    protected DrawContext currentContext;

    @Expose
    @SerializedName("relativeX")
    protected float relativeX = 0f;

    @Expose
    @SerializedName("relativeY")
    protected float relativeY = 0f;

    protected final MinecraftClient mc;

    public HudModule(String name, float defaultX, float defaultY) {
        super(name, Category.HUD);
        this.x = defaultX;
        this.y = defaultY;
        this.relativeX = 0;
        this.relativeY = 0;
        this.mc = MinecraftClient.getInstance();
    }

    /**
     * 子类实现此方法进行绘制，已自动处理NanoVG缩放
     * 直接使用x, y, width, height等逻辑坐标
     */
    public abstract void onRenderContent();

    private void onRender(DrawContext context) {
        this.currentContext = context;
        NanoVGRenderer.INSTANCE.draw(vg -> {
            applyScale();
            onRenderContent();
            resetScale();
        });
    }

    public void renderInEditor(DrawContext context, float mouseX, float mouseY) {
        if (dragging) {
            int gameWidth = mc.getWindow().getScaledWidth();
            int gameHeight = mc.getWindow().getScaledHeight();

            x = Math.max(0, Math.min(mouseX - dragX, gameWidth - width));
            y = Math.max(0, Math.min(mouseY - dragY, gameHeight - height));

            relativeX = x / gameWidth;
            relativeY = y / gameHeight;
        }

        onRender(context);

        // 绘制编辑器中的背景框
        float scale = (float) mc.getWindow().getScaleFactor();
        NanoVGRenderer.INSTANCE.draw(canvas -> {
            Color boxColor = dragging ? new Color(100, 100, 255, 80) : new Color(0, 0, 0, 50);
            NanoVGHelper.drawRect(x * scale, y * scale, width * scale, height * scale, boxColor);
        });
    }

    /**
     * 正常游戏中渲染
     */
    public void renderInGame(DrawContext context) {
        HudEditor hudEditor = Sakura.MODULE.getModule(HudEditor.class);
        if (hudEditor != null && hudEditor.isEnabled()) {
            return;
        }
        onRender(context);
    }

    protected float getMCScale() {
        return (float) mc.getWindow().getScaleFactor();
    }

    /**
     * 补偿applyScale的影响
     * 子类可以用此值乘以自定义缩放值
     */
    protected float getScale() {
        return 1.0f / getMCScale();
    }

    protected void applyScale() {
        NanoVG.nvgSave(NanoVGRenderer.INSTANCE.getContext());
        NanoVG.nvgScale(NanoVGRenderer.INSTANCE.getContext(), getMCScale(), getMCScale());
    }

    protected void resetScale() {
        NanoVG.nvgRestore(NanoVGRenderer.INSTANCE.getContext());
    }

    protected void withRawCoords(Runnable drawer) {
        resetScale();
        drawer.run();
        applyScale();
    }

    protected DrawContext getContext() {
        return currentContext;
    }

    protected MatrixStack getMatrix() {
        return currentContext.getMatrices();
    }

    /**
     * 在分辨率变化时调整位置
     */
    public void onResolutionChanged() {
        int gameWidth = mc.getWindow().getScaledWidth();
        int gameHeight = mc.getWindow().getScaledHeight();

        x = Math.max(0, Math.min(gameWidth * relativeX, gameWidth - width));
        y = Math.max(0, Math.min(gameHeight * relativeY, gameHeight - height));
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (isHovering(mouseX, mouseY) && button == 0) {
            dragging = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(float mouseX, float mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean isHovering(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isDragging() {
        return dragging;
    }
}
