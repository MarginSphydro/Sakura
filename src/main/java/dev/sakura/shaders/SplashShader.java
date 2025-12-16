package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.utils.animations.AnimationUtil;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static dev.sakura.Sakura.mc;

public class SplashShader {
    private static SplashShader INSTANCE;

    private int programId;
    private int timeUniform;
    private int resolutionUniform;
    private int progressUniform;
    private int fadeOutUniform;
    private int zoomUniform;
    private float accumulatedTime;
    private float currentProgress = 0f;
    private VertexBuffer vertexBuffer;
    private boolean initialized = false;

    private boolean transitionStarted = false;
    private long transitionStartTime = 0L;
    private static final long TRANSITION_DURATION = 2000L; // 2秒过渡

    public static SplashShader getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SplashShader();
        }
        return INSTANCE;
    }

    private SplashShader() {
        this.accumulatedTime = 0f;
    }

    public void init() {
        if (initialized) return;
        try {
            this.programId = createShaderProgram();
            this.setupVertexBuffer();
            this.initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupVertexBuffer() {
        this.vertexBuffer = new VertexBuffer(GlUsage.STATIC_WRITE);

        MatrixStack identityStack = new MatrixStack();
        Matrix4f matrix = identityStack.peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, -1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(matrix, -1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(matrix, 1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(matrix, 1.0f, -1.0f, 0.0f);

        BuiltBuffer builtBuffer = bufferBuilder.end();
        this.vertexBuffer.bind();
        this.vertexBuffer.upload(builtBuffer);
        VertexBuffer.unbind();
    }

    private int createShaderProgram() throws IOException {
        int program = GL20.glCreateProgram();

        int vertexShader = createShader("/assets/sakura/shaders/mainmenu_passthrough.vsh", GL20.GL_VERTEX_SHADER);
        int fragmentShader = createShader("/assets/sakura/shaders/splash_sakura.fsh", GL20.GL_FRAGMENT_SHADER);

        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        int linked = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        if (linked == 0) {
            throw new IllegalStateException("Shader failed to link");
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        GL20.glUseProgram(program);
        this.timeUniform = GL20.glGetUniformLocation(program, "time");
        this.resolutionUniform = GL20.glGetUniformLocation(program, "resolution");
        this.progressUniform = GL20.glGetUniformLocation(program, "progress");
        this.fadeOutUniform = GL20.glGetUniformLocation(program, "fadeOut");
        this.zoomUniform = GL20.glGetUniformLocation(program, "zoom");
        GL20.glUseProgram(0);

        return program;
    }

    private int createShader(String path, int shaderType) throws IOException {
        InputStream stream = SplashShader.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Shader file not found: " + path);
        }

        String source = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

        int shader = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        int compiled = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            String log = GL20.glGetShaderInfoLog(shader);
            throw new IllegalStateException("Failed to compile shader: " + path + "\n" + log);
        }

        return shader;
    }

    public void render(int width, int height) {
        render(width, height, this.currentProgress, 0f, 1.0f);
    }

    public void render(int width, int height, float progress) {
        render(width, height, progress, 0f, 1.0f);
    }

    public void render(int width, int height, float progress, float fadeOut, float zoom) {
        if (!initialized) {
            init();
        }
        if (this.programId == 0 || this.vertexBuffer == null) return;

        this.currentProgress = progress;

        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        GL20.glUseProgram(this.programId);

        float scaleFactor = (float) mc.getWindow().getScaleFactor();
        GL20.glUniform2f(this.resolutionUniform, width * scaleFactor, height * scaleFactor);

        accumulatedTime += (float) (1.0 * AnimationUtil.deltaTime());
        GL20.glUniform1f(this.timeUniform, accumulatedTime);
        GL20.glUniform1f(this.progressUniform, progress);
        GL20.glUniform1f(this.fadeOutUniform, fadeOut);
        GL20.glUniform1f(this.zoomUniform, zoom);

        this.vertexBuffer.bind();
        this.vertexBuffer.draw();
        VertexBuffer.unbind();

        GL20.glUseProgram(0);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    public void startTransition() {
        this.transitionStarted = true;
        this.transitionStartTime = System.currentTimeMillis();
    }

    public boolean isTransitionStarted() {
        return transitionStarted;
    }

    public float getTransitionProgress() {
        if (!transitionStarted) return 0f;
        long elapsed = System.currentTimeMillis() - transitionStartTime;
        return Math.min(1f, (float) elapsed / TRANSITION_DURATION);
    }

    public boolean isTransitionComplete() {
        return transitionStarted && getTransitionProgress() >= 1f;
    }

    public float getAccumulatedTime() {
        return accumulatedTime;
    }

    public void reset() {
        this.transitionStarted = false;
        this.accumulatedTime = 0f;
    }

    public void cleanup() {
        if (this.programId != 0) {
            GL20.glDeleteProgram(this.programId);
            this.programId = 0;
        }
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
        this.initialized = false;
        INSTANCE = null;
    }
}
