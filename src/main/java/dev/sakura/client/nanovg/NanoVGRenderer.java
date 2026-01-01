package dev.sakura.client.nanovg;

import dev.sakura.client.nanovg.util.state.States;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

/**
 * Sakura NanoVG渲染器
 * <p>
 * 坐标系说明：
 * - draw(): 使用MC逻辑坐标（自动缩放），与鼠标坐标、Screen尺寸一致
 * - drawRaw(): 使用像素坐标，用于需要精确像素控制的场景
 */

public class NanoVGRenderer {
    public static final NanoVGRenderer INSTANCE = new NanoVGRenderer();

    private long vg = 0L;
    private boolean initialized = false;
    private boolean inFrame = false;
    private boolean scaled = false;

    private NanoVGRenderer() {
    }

    public void initNanoVG() {
        if (!initialized) {
            // Remove NVG_STENCIL_STROKES as it can cause issues if the framebuffer doesn't have a stencil attachment
            vg = nvgCreate(NVG_ANTIALIAS);
            if (vg == 0L) {
                throw new RuntimeException("无法初始化NanoVG");
            }
            initialized = true;
        }
    }

    public long getContext() {
        if (!initialized) {
            initNanoVG();
        }
        return vg;
    }

    private float getScaleFactor() {
        return (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
    }

    public int getScaledWidth() {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    public int getScaledHeight() {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    /**
     * 使用MC逻辑坐标绘制（自动缩放）
     * 坐标系与mouseX, mouseY, scaledWidth, scaledHeight一致
     */
    public void draw(Consumer<Long> drawingLogic) {
        draw(drawingLogic, true);
    }

    public void draw(Consumer<Long> drawingLogic, boolean applyScale) {
        if (!initialized) initNanoVG();
        if (inFrame) {
            drawingLogic.accept(vg);
            return;
        }

        States.INSTANCE.push();

        MinecraftClient mc = MinecraftClient.getInstance();
        // 获取逻辑坐标的宽高（与鼠标坐标、Screen 尺寸一致）
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();
        // 获取像素比（Retina 为 2.0，普通屏为 1.0）
        float pixelRatio = (float) mc.getWindow().getScaleFactor();

        // 关键修正：传入逻辑宽高和像素比，NanoVG 内部会自动处理高分屏缩放
        nvgBeginFrame(vg, scaledWidth, scaledHeight, pixelRatio);
        inFrame = true;

        try {
            // 现在直接使用逻辑坐标绘图即可，无需额外 nvgScale
            drawingLogic.accept(vg);
        } finally {
            nvgEndFrame(vg);
            inFrame = false;
            States.INSTANCE.pop();
        }
    }

    /**
     * 使用像素坐标绘制（无缩放）
     * 用于需要精确像素控制的场景
     */
    @Deprecated
    public void drawRaw(Consumer<Long> drawingLogic) {
        if (!initialized) {
            initNanoVG();
        }

        States.INSTANCE.push();

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        nvgBeginFrame(vg, width, height, 1.0f);
        inFrame = true;
        scaled = false;

        try {
            drawingLogic.accept(vg);
        } finally {
            nvgEndFrame(vg);
            inFrame = false;
            States.INSTANCE.pop();
        }
    }

    /**
     * 在已开始的帧中临时暂停NanoVG（用于其他渲染器如Shader）
     */
    public void withRawCoords(Runnable drawer) {
        if (!inFrame) {
            throw new IllegalStateException("必须在draw()回调内使用");
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        boolean wasScaled = scaled;

        nvgEndFrame(vg);

        drawer.run();

        nvgBeginFrame(vg, width, height, 1.0f);

        if (wasScaled) {
            float scale = getScaleFactor();
            nvgSave(vg);
            nvgScale(vg, scale, scale);
        }
    }

    public boolean isInFrame() {
        return inFrame;
    }

    public boolean isScaled() {
        return scaled;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (initialized && vg != 0L) {
            nvgDelete(vg);
            vg = 0L;
            initialized = false;
        }
    }
}
