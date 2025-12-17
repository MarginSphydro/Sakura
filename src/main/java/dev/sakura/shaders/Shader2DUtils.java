package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.gui.clickgui.ClickGuiScreen;
import dev.sakura.gui.hud.HudEditorScreen;
import dev.sakura.gui.mainmenu.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;

import static dev.sakura.Sakura.mc;

public class Shader2DUtils {
    public static MainMenuProgram MAIN_MENU_PROGRAM;
    public static ArcShader ARC_PROGRAM;
    public static BlurProgram BLUR_PROGRAM;

    private static boolean shouldSkipBlur() {
        Screen screen = mc.currentScreen;
        return !(screen instanceof ClickGuiScreen || screen instanceof HudEditorScreen || screen instanceof MainMenuScreen);
    }

    public static void init() {
        MAIN_MENU_PROGRAM = new MainMenuProgram();
        ARC_PROGRAM = new ArcShader();
        BLUR_PROGRAM = new BlurProgram();
    }

    public static void drawQuadBlur(MatrixStack matrices, float x, float y, float width, float height, float blurStrength, float blurOpacity) {
        if (shouldSkipBlur()) return;
        BufferBuilder bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20);
        BLUR_PROGRAM.setParameters(x, y, width, height, 0f, blurStrength, blurOpacity);
        BLUR_PROGRAM.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        endRender();
    }

    public static void drawRoundedBlur(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity) {
        if (shouldSkipBlur()) return;
        BufferBuilder bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20);
        BLUR_PROGRAM.setParameters(x, y, width, height, radius, c1, blurStrenth, blurOpacity);
        BLUR_PROGRAM.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        endRender();
    }

    public static BufferBuilder preShaderDraw(MatrixStack matrices, float x, float y, float width, float height) {
        setupRender();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        setRectanglePoints(buffer, matrix, x, y, x + width, y + height);
        return buffer;
    }

    public static void setRectanglePoints(BufferBuilder buffer, Matrix4f matrix, float x, float y, float x1, float y1) {
        buffer.vertex(matrix, x, y, 0);
        buffer.vertex(matrix, x, y1, 0);
        buffer.vertex(matrix, x1, y1, 0);
        buffer.vertex(matrix, x1, y, 0);
    }

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void endRender() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}