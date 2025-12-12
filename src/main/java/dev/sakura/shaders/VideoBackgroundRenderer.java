package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.joml.Matrix4f;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import static dev.sakura.Sakura.mc;

public class VideoBackgroundRenderer {
    // 60帧，我的电脑有点卡
    private static final String VIDEO_PATH = "/assets/sakura/videos/video_60.mp4";

    private FFmpegFrameGrabber grabber;
    private Java2DFrameConverter converter;
    private NativeImageBackedTexture texture;
    private Identifier textureId;
    private VertexBuffer vertexBuffer;
    private boolean initialized;
    private long lastFrameTime;
    private double frameDelay;

    private int videoWidth;
    private int videoHeight;

    public VideoBackgroundRenderer() {
        this.converter = new Java2DFrameConverter();
        this.lastFrameTime = System.currentTimeMillis();
        this.initialized = false;
    }

    public void init() {
        if (initialized) return;

        try {
            InputStream stream = VideoBackgroundRenderer.class.getResourceAsStream(VIDEO_PATH);
            if (stream == null) {
                throw new RuntimeException("视频文件不存在: " + VIDEO_PATH);
            }

            grabber = new FFmpegFrameGrabber(stream);
            grabber.setFormat("mp4");
            grabber.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
            grabber.start();

            videoWidth = grabber.getImageWidth();
            videoHeight = grabber.getImageHeight();
            double fps = grabber.getFrameRate();
            frameDelay = 1000.0 / fps;

            textureId = Identifier.of("sakura", "video_background");
            texture = new NativeImageBackedTexture(videoWidth, videoHeight, false);
            mc.getTextureManager().registerTexture(textureId, texture);

            setupVertexBuffer();

            updateFrame();

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
        }
    }

    private void setupVertexBuffer() {
        this.vertexBuffer = new VertexBuffer(GlUsage.STATIC_WRITE);

        MatrixStack identityStack = new MatrixStack();
        Matrix4f matrix = identityStack.peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, -1.0f, -1.0f, 0.0f).texture(0.0f, 1.0f);
        bufferBuilder.vertex(matrix, -1.0f, 1.0f, 0.0f).texture(0.0f, 0.0f);
        bufferBuilder.vertex(matrix, 1.0f, 1.0f, 0.0f).texture(1.0f, 0.0f);
        bufferBuilder.vertex(matrix, 1.0f, -1.0f, 0.0f).texture(1.0f, 1.0f);

        BuiltBuffer builtBuffer = bufferBuilder.end();
        this.vertexBuffer.bind();
        this.vertexBuffer.upload(builtBuffer);
        VertexBuffer.unbind();
    }

    public void render(int screenWidth, int screenHeight) {
        if (!initialized) {
            init();
            if (!initialized) return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFrameTime >= frameDelay) {
                updateFrame();
                lastFrameTime = currentTime;
            }

            renderFrame(screenWidth, screenHeight);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateFrame() throws Exception {
        Frame frame = grabber.grabImage();

        if (frame == null) {
            grabber.stop();
            grabber.release();

            InputStream stream = VideoBackgroundRenderer.class.getResourceAsStream(VIDEO_PATH);
            if (stream != null) {
                grabber = new FFmpegFrameGrabber(stream);
                grabber.setFormat("mp4");
                grabber.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
                grabber.start();
                frame = grabber.grabImage();
            } else {
                return;
            }
        }

        if (frame != null && frame.image != null) {
            BufferedImage bufferedImage = converter.convert(frame);
            if (bufferedImage != null) {
                updateTexture(bufferedImage);
            }
        }
    }

    private void updateTexture(BufferedImage image) {
        NativeImage nativeImage = texture.getImage();
        if (nativeImage == null) return;

        for (int y = 0; y < videoHeight; y++) {
            for (int x = 0; x < videoWidth; x++) {
                int rgb = image.getRGB(x, y);
                int a = 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                nativeImage.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        texture.upload();
    }

    private void renderFrame(int screenWidth, int screenHeight) {
        if (texture == null || vertexBuffer == null) return;

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);

        MatrixStack matrices = new MatrixStack();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix, 0, screenHeight, 0).texture(0.0f, 1.0f);
        bufferBuilder.vertex(matrix, screenWidth, screenHeight, 0).texture(1.0f, 1.0f);
        bufferBuilder.vertex(matrix, screenWidth, 0, 0).texture(1.0f, 0.0f);
        bufferBuilder.vertex(matrix, 0, 0, 0).texture(0.0f, 0.0f);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    public void cleanup() {
        initialized = false;

        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            grabber = null;
        }

        if (converter != null) {
            converter.close();
            converter = null;
        }

        if (texture != null) {
            texture.close();
            texture = null;
        }

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
