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

public class MainMenuShader {
    // 共享的SAKURA着色器实例（用于 SplashOverlay和TitleScreen之间的过渡）
    private static MainMenuShader sharedInstance;

    public static MainMenuShader getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new MainMenuShader(MainMenuShaderType.SAKURA);
        }
        return sharedInstance;
    }

    public static void cleanupSharedInstance() {
        if (sharedInstance != null) {
            sharedInstance.cleanup();
            sharedInstance = null;
        }
    }

    private int programId;
    private int timeUniform;
    private int resolutionUniform;
    private int transitionUniform;
    private float accumulatedTime;
    private float transitionValue = 1.0f; // 1.0 = 正常显示
    private VertexBuffer vertexBuffer;
    private MainMenuShaderType currentShaderType;
    private boolean useAlternativeUniforms;
    private VideoBackgroundRenderer videoRenderer;

    public MainMenuShader(MainMenuShaderType shaderType) {
        this.accumulatedTime = (shaderType == MainMenuShaderType.MAIN_MENU) ? 10000f : 0f;
        this.currentShaderType = shaderType;

/*        if (shaderType == MainMenuShaderType.VIDEO) {
            this.videoRenderer = new VideoBackgroundRenderer();
        } else {
            try {
                this.programId = createShaderProgram(shaderType);
                this.setupVertexBuffer();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load main menu shader: " + shaderType.getDisplayName(), e);
            }
        }*/

        try {
            this.programId = createShaderProgram(shaderType);
            this.setupVertexBuffer();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load main menu shader: " + shaderType.getDisplayName(), e);
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

    private int createShaderProgram(MainMenuShaderType shaderType) throws IOException {
        int program = GL20.glCreateProgram();

        int vertexShader = createShader(shaderType.getVertexShaderPath(), GL20.GL_VERTEX_SHADER);
        int fragmentShader = createShader(shaderType.getFragmentShaderPath(), GL20.GL_FRAGMENT_SHADER);

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
        if (shaderType == MainMenuShaderType.MAIN_MENU) {
            this.timeUniform = GL20.glGetUniformLocation(program, "Time");
            this.resolutionUniform = GL20.glGetUniformLocation(program, "uSize");
            this.transitionUniform = -1; // 不支持
            this.useAlternativeUniforms = true;
        } else {
            this.timeUniform = GL20.glGetUniformLocation(program, "time");
            this.resolutionUniform = GL20.glGetUniformLocation(program, "resolution");
            this.transitionUniform = GL20.glGetUniformLocation(program, "transition");
            this.useAlternativeUniforms = false;
        }
        GL20.glUseProgram(0);

        return program;
    }

    private int createShader(String path, int shaderType) throws IOException {
        InputStream stream = MainMenuShader.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Shader file not found: " + path);
        }

        String source = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

        int shader = GL20.glCreateShader(shaderType);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        int compiled = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        if (compiled == 0) {
            throw new IllegalStateException("Failed to compile shader: " + path);
        }

        return shader;
    }

    public void render(int width, int height) {
        render(width, height, this.transitionValue);
    }

    public void render(int width, int height, float transition) {
        if (this.programId == 0 || this.vertexBuffer == null) return;

        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        GL20.glUseProgram(this.programId);

        float scaleFactor = (float) mc.getWindow().getScaleFactor();
        GL20.glUniform2f(this.resolutionUniform, width * scaleFactor, height * scaleFactor);

        if (useAlternativeUniforms) {
            accumulatedTime += (float) (0.55 * AnimationUtil.deltaTime());
            GL20.glUniform1f(this.timeUniform, accumulatedTime);
        } else {
            accumulatedTime += (float) (1.0 * AnimationUtil.deltaTime());
            GL20.glUniform1f(this.timeUniform, accumulatedTime);
        }

        // 设置过渡参数
        if (this.transitionUniform >= 0) {
            GL20.glUniform1f(this.transitionUniform, transition);
        }

        this.vertexBuffer.bind();
        this.vertexBuffer.draw();
        VertexBuffer.unbind();

        GL20.glUseProgram(0);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    public void setTransition(float transition) {
        this.transitionValue = transition;
    }

    public float getTransition() {
        return this.transitionValue;
    }

    public void switchShaderType(MainMenuShaderType newType) {
        if (this.currentShaderType == newType) {
            return;
        }

        // 清理旧的资源
        if (this.programId != 0) {
            GL20.glDeleteProgram(this.programId);
            this.programId = 0;
        }

        if (this.videoRenderer != null) {
            this.videoRenderer.cleanup();
            this.videoRenderer = null;
        }

        this.currentShaderType = newType;

        // 初始化新着色器
        /*if (newType == MainMenuShaderType.VIDEO) {
            this.videoRenderer = new VideoBackgroundRenderer();
        } else {
            try {
                this.accumulatedTime = (newType == MainMenuShaderType.MAIN_MENU) ? 10000f : 0f;
                this.programId = createShaderProgram(newType);
                if (this.vertexBuffer == null) {
                    this.setupVertexBuffer();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
        try {
            this.accumulatedTime = (newType == MainMenuShaderType.MAIN_MENU) ? 10000f : 0f;
            this.programId = createShaderProgram(newType);
            if (this.vertexBuffer == null) {
                this.setupVertexBuffer();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MainMenuShaderType getCurrentShaderType() {
        return currentShaderType;
    }

    public void nextShader() {
        MainMenuShaderType next = currentShaderType.next();

        while (next == MainMenuShaderType.MAIN_MENU || next == MainMenuShaderType.BSW) {
            next = next.next();
        }
        switchShaderType(next);
    }

    public void previousShader() {
        MainMenuShaderType prev = currentShaderType.previous();

        while (prev == MainMenuShaderType.MAIN_MENU || prev == MainMenuShaderType.BSW) {
            prev = prev.previous();
        }
        switchShaderType(prev);
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
        if (this.videoRenderer != null) {
            this.videoRenderer.cleanup();
            this.videoRenderer = null;
        }
    }

    public enum MainMenuShaderType {
        MAIN_MENU("mainmenu.fsh", "主菜单"),
        //SAKURA1("mainmenu_sakura1.fsh", "樱花效果 1"),
        SAKURA("mainmenu_sakura2.fsh", "樱花效果"),
        //SAKURA3("mainmenu_sakura3.fsh", "樱花效果 3"),
        SEA("mainmenu_sea.fsh", "海洋效果"),
        TOKYO("mainmenu_tokyo.fsh", "东京夜景"),
        BSW("mainmenubsw.fsh", "流光波纹"),
        MOON("mainmenu_moon.fsh", "月球飞行");
        //JOURNEY("mainmenu_journey.fsh", "旅程效果"),
        //DRIVE_HOME("mainmenu_drivehome.fsh", "驾车回家"),
        //HEARTFELT("mainmenu_heartfelt.fsh", "爱心雨滴"), TODO:爱心雨滴依赖背景不然很丑

        private final String fragmentShaderPath;
        private final String displayName;

        MainMenuShaderType(String fragmentShader, String displayName) {
            this.fragmentShaderPath = "/assets/sakura/shaders/" + fragmentShader;
            this.displayName = displayName;
        }

        public String getFragmentShaderPath() {
            return fragmentShaderPath;
        }

        public String getVertexShaderPath() {
            return "/assets/sakura/shaders/mainmenu_passthrough.vsh";
        }

        public String getDisplayName() {
            return displayName;
        }

        public MainMenuShaderType next() {
            MainMenuShaderType[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public MainMenuShaderType previous() {
            MainMenuShaderType[] values = values();
            return values[(this.ordinal() - 1 + values.length) % values.length];
        }

        public static MainMenuShaderType fromName(String name) {
            for (MainMenuShaderType type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
