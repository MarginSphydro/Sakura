package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class Shader2DUtils {
    public static MainMenuProgram MAIN_MENU_PROGRAM;
    public static ArcShader ARC_PROGRAM;
    public static BlurProgram BLUR_PROGRAM;

    private static boolean prevScissor;
    private static boolean prevStencil;

    public static void init() {
        MAIN_MENU_PROGRAM = new MainMenuProgram();
        ARC_PROGRAM = new ArcShader();
        BLUR_PROGRAM = new BlurProgram();
    }

    public static void drawQuadBlur(MatrixStack matrices, float x, float y, float width, float height, float blurStrength, float blurOpacity) {
        BufferBuilder bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20);
        BLUR_PROGRAM.setParameters(x, y, width, height, 0f, blurStrength, blurOpacity);
        BLUR_PROGRAM.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        endRender();
    }

    public static void drawRoundedBlur(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity) {
        BufferBuilder bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20);
        BLUR_PROGRAM.setParameters(x, y, width, height, radius, c1, blurStrenth, blurOpacity);
        BLUR_PROGRAM.use();
        BufferRenderer.drawWithGlobalProgram(bb.end());
        endRender();
    }

    public static BufferBuilder preShaderDraw(MatrixStack matrices, float x, float y, float width, float height) {
        beginRender();
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

    public static void beginRender() {
        prevScissor = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST);
        prevStencil = GL11.glGetBoolean(GL11.GL_STENCIL_TEST);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void endRender() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (prevScissor) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        else GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (prevStencil) GL11.glEnable(GL11.GL_STENCIL_TEST);
        else GL11.glDisable(GL11.GL_STENCIL_TEST);
    }
}
