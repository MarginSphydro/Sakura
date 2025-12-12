package dev.sakura.nanovg;

import dev.sakura.nanovg.util.state.States;
import net.minecraft.client.MinecraftClient;

import java.util.function.Consumer;

import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVGGL3.*;

public class NanoVGRenderer {
    public static final NanoVGRenderer INSTANCE = new NanoVGRenderer();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private long vg = 0L;
    private boolean initialized = false;

    private NanoVGRenderer() {
    }

    public void initNanoVG() {
        if (!initialized) {
            vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
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

    public void draw(Consumer<Long> drawingLogic) {
        if (!initialized) {
            initNanoVG();
        }

        States.INSTANCE.push();

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        // 开始NanoVG帧
        nvgBeginFrame(vg, width, height, 1.0f);

        drawingLogic.accept(vg);

        // 结束NanoVG帧
        nvgEndFrame(vg);

        States.INSTANCE.pop();
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
