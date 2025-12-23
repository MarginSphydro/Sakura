package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.shaders.satin.api.ManagedCoreShader;
import dev.sakura.shaders.satin.api.ShaderEffectManager;
import dev.sakura.shaders.satin.api.uniform.SamplerUniform;
import dev.sakura.shaders.satin.api.uniform.Uniform1f;
import dev.sakura.shaders.satin.api.uniform.Uniform2f;
import dev.sakura.shaders.satin.api.uniform.Uniform4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL30;

import java.awt.*;

import static dev.sakura.Sakura.mc;

public class BlurProgram {
    private Uniform2f uSize;
    private Uniform2f uLocation;
    private Uniform1f radius;
    private Uniform2f inputResolution;
    private Uniform1f brightness;
    private Uniform1f quality;
    private Uniform4f color1;
    private SamplerUniform sampler;

    private Framebuffer input;
    private int currentProgramId = -1;

    public static final ManagedCoreShader BLUR = ShaderEffectManager.getInstance().manageCoreShader(Identifier.of("sakura", "core/blur"), VertexFormats.POSITION);

    public BlurProgram() {
        setup();
        WindowResizeCallback.EVENT.register((client, window) -> {
            if (input != null)
                input.resize(window.getFramebufferWidth(), window.getFramebufferHeight());
        });
    }

    public void setParameters(float x, float y, float width, float height, float r, float blurStrenth, float blurOpacity) {
        setParameters(x, y, width, height, r, 0f, 0f, 0f, 0f, blurStrenth, blurOpacity);
    }

    public void setParameters(float x, float y, float width, float height, float r, Color c1, float blurStrenth, float blurOpacity) {
        setParameters(x, y, width, height, r, c1.getRed() / 255f, c1.getGreen() / 255f, c1.getBlue() / 255f, c1.getAlpha() / 255f, blurStrenth, blurOpacity);
    }

    public void setParameters(float x, float y, float width, float height, float r, float red, float green, float blue, float alpha, float blurStrenth, float blurOpacity) {
        if (input == null)
            input = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);

        float i = (float) mc.getWindow().getScaleFactor();

        checkProgram();

        radius.set(r * i);
        float calculatedY = mc.getWindow().getFramebufferHeight() - (y * i) - (height * i);
        uLocation.set(x * i, calculatedY);
        uSize.set(width * i, height * i);
        
        // Debug output to verify coordinates
        // System.out.println("Blur Param: x=" + (x*i) + ", y=" + calculatedY + ", w=" + (width*i) + ", h=" + (height*i));
        
        brightness.set(blurOpacity);
        quality.set(blurStrenth);
        color1.set(red, green, blue, alpha);
        sampler.set(input.getColorAttachment());
    }

    public void use() {
        if (input != null && (input.textureWidth != mc.getWindow().getFramebufferWidth() || input.textureHeight != mc.getWindow().getFramebufferHeight()))
            input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());

        var buffer = MinecraftClient.getInstance().getFramebuffer();
        
        // Backup current state
        int prevVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        GL30.glBindVertexArray(0); // Unbind VAO for framebuffer operations to avoid conflicts
        
        input.beginWrite(false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.fbo);
        GL30.glBlitFramebuffer(0, 0, buffer.textureWidth, buffer.textureHeight, 0, 0, buffer.textureWidth, buffer.textureHeight, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
        buffer.beginWrite(false);

        checkProgram();

        inputResolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
        sampler.set(input.getColorAttachment());

        if (BLUR.getProgram() != null) {
            RenderSystem.setShader(BLUR.getProgram());
        } else {
            System.err.println("[BlurProgram] Shader not loaded!");
        }
        
        // Restore state
        GL30.glBindVertexArray(prevVAO);
    }

    private void checkProgram() {
        ShaderProgram program = BLUR.getProgram();
        if (program != null) {
            int newId = program.getGlRef();
            if (newId != currentProgramId) {
                setup();
                currentProgramId = newId;
            }
        }
    }

    private void setup() {
        this.inputResolution = BLUR.findUniform2f("InputResolution");
        this.brightness = BLUR.findUniform1f("Brightness");
        this.quality = BLUR.findUniform1f("Quality");
        this.color1 = BLUR.findUniform4f("color1");
        this.uSize = BLUR.findUniform2f("uSize");
        this.uLocation = BLUR.findUniform2f("uLocation");
        this.radius = BLUR.findUniform1f("radius");
        sampler = BLUR.findSampler("InputSampler");

        ShaderProgram program = BLUR.getProgram();
        if (program != null) {
            currentProgramId = program.getGlRef();
        }
    }
}
