package dev.sakura.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;

import static dev.sakura.Sakura.mc;

public class Render3DUtil {
    public static void drawFullBox(MatrixStack stack, BlockPos blockPos, Color sideColor, Color lineColor) {
        drawFullBox(stack, blockPos, sideColor, lineColor, 2f);
    }

    public static void drawFullBox(MatrixStack stack, Box box, Color sideColor, Color lineColor) {
        drawFullBox(stack, box, sideColor, lineColor, 2f);
    }

    public static void drawFullBox(MatrixStack stack, BlockPos blockPos, Color sideColor, Color lineColor, float lineWidth) {
        drawFullBox(stack, new Box(blockPos), sideColor, lineColor, lineWidth);
    }

    public static void drawFullBox(MatrixStack stack, Box box, Color sideColor, Color lineColor, float lineWidth) {
        drawFullBox(stack, box, sideColor.getRGB(), lineColor.getRGB(), lineWidth);
    }

    public static void drawFullBox(MatrixStack stack, Box box, int sideColor, int lineColor, float thickness) {
        drawFilledBox(stack, box, sideColor);
        drawBoxOutline(stack, box, lineColor, thickness);
    }

    public static void drawFilledBox(MatrixStack stack, Box box, int c) {
        drawFilledFadeBox(stack, box, c, c);
    }

    public static void drawFilledFadeBox(MatrixStack stack, Box box, int c, int c1) {
        setup3D();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Vec3d camPos = mc.getEntityRenderDispatcher().camera.getPos();
        float minX = (float) (box.minX - camPos.getX());
        float minY = (float) (box.minY - camPos.getY());
        float minZ = (float) (box.minZ - camPos.getZ());
        float maxX = (float) (box.maxX - camPos.getX());
        float maxY = (float) (box.maxY - camPos.getY());
        float maxZ = (float) (box.maxZ - camPos.getZ());

        Matrix4f matrix = stack.peek().getPositionMatrix();

        vertex(bufferBuilder, matrix, minX, minY, minZ, c);
        vertex(bufferBuilder, matrix, minX, minY, maxZ, c);
        vertex(bufferBuilder, matrix, maxX, minY, maxZ, c);
        vertex(bufferBuilder, matrix, maxX, minY, minZ, c);

        vertex(bufferBuilder, matrix, minX, maxY, minZ, c1);
        vertex(bufferBuilder, matrix, maxX, maxY, minZ, c1);
        vertex(bufferBuilder, matrix, maxX, maxY, maxZ, c1);
        vertex(bufferBuilder, matrix, minX, maxY, maxZ, c);

        vertex(bufferBuilder, matrix, minX, minY, minZ, c);
        vertex(bufferBuilder, matrix, minX, maxY, minZ, c1);
        vertex(bufferBuilder, matrix, maxX, maxY, minZ, c1);
        vertex(bufferBuilder, matrix, maxX, minY, minZ, c);

        vertex(bufferBuilder, matrix, maxX, minY, minZ, c);
        vertex(bufferBuilder, matrix, maxX, maxY, minZ, c1);
        vertex(bufferBuilder, matrix, maxX, maxY, maxZ, c1);
        vertex(bufferBuilder, matrix, maxX, minY, maxZ, c);

        vertex(bufferBuilder, matrix, minX, minY, maxZ, c);
        vertex(bufferBuilder, matrix, maxX, minY, maxZ, c);
        vertex(bufferBuilder, matrix, maxX, maxY, maxZ, c1);
        vertex(bufferBuilder, matrix, minX, maxY, maxZ, c1);

        vertex(bufferBuilder, matrix, minX, minY, minZ, c);
        vertex(bufferBuilder, matrix, minX, minY, maxZ, c);
        vertex(bufferBuilder, matrix, minX, maxY, maxZ, c1);
        vertex(bufferBuilder, matrix, minX, maxY, minZ, c1);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        cleanup3D();
    }

    public static void drawBoxOutline(MatrixStack stack, Box box, int color, float thickness) {
        setup3D();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        RenderSystem.lineWidth(thickness);

        Vec3d camPos = mc.getEntityRenderDispatcher().camera.getPos();
        float minX = (float) (box.minX - camPos.getX());
        float minY = (float) (box.minY - camPos.getY());
        float minZ = (float) (box.minZ - camPos.getZ());
        float maxX = (float) (box.maxX - camPos.getX());
        float maxY = (float) (box.maxY - camPos.getY());
        float maxZ = (float) (box.maxZ - camPos.getZ());

        Matrix4f matrix = stack.peek().getPositionMatrix();
        MatrixStack.Entry entry = stack.peek();

        vertexLine(buffer, matrix, entry, minX, minY, minZ, maxX, minY, minZ, color);
        vertexLine(buffer, matrix, entry, maxX, minY, minZ, maxX, minY, maxZ, color);
        vertexLine(buffer, matrix, entry, maxX, minY, maxZ, minX, minY, maxZ, color);
        vertexLine(buffer, matrix, entry, minX, minY, maxZ, minX, minY, minZ, color);

        vertexLine(buffer, matrix, entry, minX, maxY, minZ, maxX, maxY, minZ, color);
        vertexLine(buffer, matrix, entry, maxX, maxY, minZ, maxX, maxY, maxZ, color);
        vertexLine(buffer, matrix, entry, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        vertexLine(buffer, matrix, entry, minX, maxY, maxZ, minX, maxY, minZ, color);

        vertexLine(buffer, matrix, entry, minX, minY, minZ, minX, maxY, minZ, color);
        vertexLine(buffer, matrix, entry, maxX, minY, minZ, maxX, maxY, minZ, color);
        vertexLine(buffer, matrix, entry, maxX, minY, maxZ, maxX, maxY, maxZ, color);
        vertexLine(buffer, matrix, entry, minX, minY, maxZ, minX, maxY, maxZ, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        cleanup3D();
    }

    public static void drawBottomOutline(MatrixStack stack, Box box, int color) {
        setup3D();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        Vec3d camPos = mc.getEntityRenderDispatcher().camera.getPos();
        float minX = (float) (box.minX - camPos.getX());
        float minY = (float) (box.minY - camPos.getY());
        float minZ = (float) (box.minZ - camPos.getZ());
        float maxX = (float) (box.maxX - camPos.getX());
        float maxZ = (float) (box.maxZ - camPos.getZ());

        Matrix4f matrix = stack.peek().getPositionMatrix();
        MatrixStack.Entry entry = stack.peek();

        vertexLine(buffer, matrix, entry, minX, minY, minZ, maxX, minY, minZ, color);
        vertexLine(buffer, matrix, entry, maxX, minY, minZ, maxX, minY, maxZ, color);
        vertexLine(buffer, matrix, entry, maxX, minY, maxZ, minX, minY, maxZ, color);
        vertexLine(buffer, matrix, entry, minX, minY, maxZ, minX, minY, minZ, color);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        cleanup3D();
    }

    public static void setup3D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    public static void cleanup3D() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z).color(color);
    }

    private static void vertexLine(BufferBuilder buffer, Matrix4f matrix, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        Vector3f normal = getNormal(x1, y1, z1, x2, y2, z2);
        buffer.vertex(matrix, x1, y1, z1).color(color).normal(entry, normal.x, normal.y, normal.z);
        buffer.vertex(matrix, x2, y2, z2).color(color).normal(entry, normal.x, normal.y, normal.z);
    }

    private static Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }
}
