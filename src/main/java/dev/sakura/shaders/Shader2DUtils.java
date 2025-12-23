package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;

import java.awt.*;

public class Shader2DUtils {
    public static MainMenuProgram MAIN_MENU_PROGRAM;
    public static ArcShader ARC_PROGRAM;
    public static BlurProgram BLUR_PROGRAM;

    private static VertexBuffer vertexBuffer;

    public static void init() {
        MAIN_MENU_PROGRAM = new MainMenuProgram();
        ARC_PROGRAM = new ArcShader();
        BLUR_PROGRAM = new BlurProgram();
        vertexBuffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);
    }

    public static void drawQuadBlur(MatrixStack matrices, float x, float y, float width, float height, float blurStrength, float blurOpacity) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vector4f posStart = matrix.transform(new Vector4f(x, y, 0, 1.0F));
        Vector4f posEnd = matrix.transform(new Vector4f(x + width, y + height, 0, 1.0F));

        float absX = posStart.x;
        float absY = posStart.y;
        float absWidth = Math.abs(posEnd.x - posStart.x);
        float absHeight = Math.abs(posEnd.y - posStart.y);

        BufferBuilder bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20);
        BLUR_PROGRAM.setParameters(absX, absY, absWidth, absHeight, 0f, blurStrength, blurOpacity);
        BLUR_PROGRAM.use();
        BuiltBuffer builtBuffer = bb.end();
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);
        vertexBuffer.draw(new Matrix4f(), RenderSystem.getProjectionMatrix(), BlurProgram.BLUR.getProgram());
        VertexBuffer.unbind();
        endRender();
    }

    public static void drawRoundedBlur(MatrixStack matrices, float x, float y, float width, float height, float radius, Color c1, float blurStrenth, float blurOpacity) {
        blurOpacity = Math.max(0f, Math.min(1f, blurOpacity));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vector4f posStart = matrix.transform(new Vector4f(x, y, 0, 1.0F));
        Vector4f posEnd = matrix.transform(new Vector4f(x + width, y + height, 0, 1.0F));

        float absX = posStart.x;
        float absY = posStart.y;
        float absWidth = posEnd.x - posStart.x;
        float absHeight = posEnd.y - posStart.y;

        float scaleX = absWidth / width;
        float scaleY = absHeight / height;
        float absRadius = radius * (scaleX + scaleY) / 2.0f;

        BufferBuilder bb = preShaderDraw(matrices, x - 10, y - 10, width + 20, height + 20);
        BLUR_PROGRAM.setParameters(absX, absY, absWidth, absHeight, absRadius, c1, blurStrenth, blurOpacity);
        BLUR_PROGRAM.use();
        BuiltBuffer builtBuffer = bb.end();
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);
        vertexBuffer.draw(new Matrix4f(), RenderSystem.getProjectionMatrix(), BlurProgram.BLUR.getProgram());
        VertexBuffer.unbind();
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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
    }

    public static void endRender() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
