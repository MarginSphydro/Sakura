package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.shaders.satin.api.ManagedCoreShader;
import dev.sakura.shaders.satin.api.ShaderEffectManager;
import dev.sakura.shaders.satin.api.uniform.SamplerUniform;
import dev.sakura.shaders.satin.api.uniform.Uniform1f;
import dev.sakura.shaders.satin.api.uniform.Uniform2f;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL30;

import static dev.sakura.Sakura.mc;

public class LiquidGlassProgram {
    private Uniform2f uSize;
    private Uniform2f uLocation;
    private Uniform1f radius;
    private Uniform2f inputResolution;
    private Uniform1f blurStrength;
    private Uniform1f refractionStrength;
    private Uniform1f edgeWidth;
    private Uniform1f brightness;
    private Uniform1f saturation;
    private SamplerUniform sampler;

    private Framebuffer input;

    public static final ManagedCoreShader LIQUID_GLASS = ShaderEffectManager.getInstance()
            .manageCoreShader(Identifier.of("sakura", "core/liquid_glass"), VertexFormats.POSITION);

    public LiquidGlassProgram() {
        this.inputResolution = LIQUID_GLASS.findUniform2f("InputResolution");
        this.uSize = LIQUID_GLASS.findUniform2f("uSize");
        this.uLocation = LIQUID_GLASS.findUniform2f("uLocation");
        this.radius = LIQUID_GLASS.findUniform1f("radius");
        this.blurStrength = LIQUID_GLASS.findUniform1f("BlurStrength");
        this.refractionStrength = LIQUID_GLASS.findUniform1f("RefractionStrength");
        this.edgeWidth = LIQUID_GLASS.findUniform1f("EdgeWidth");
        this.brightness = LIQUID_GLASS.findUniform1f("Brightness");
        this.saturation = LIQUID_GLASS.findUniform1f("Saturation");
        this.sampler = LIQUID_GLASS.findSampler("InputSampler");

        WindowResizeCallback.EVENT.register((client, window) -> {
            if (input != null) {
                input.resize(window.getFramebufferWidth(), window.getFramebufferHeight());
            }
        });
    }

    public void setParameters(float x, float y, float width, float height, float r,
                              float blur, float refraction, float edge, float bright, float sat) {
        if (input == null) {
            input = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), false);
        }

        float i = (float) mc.getWindow().getScaleFactor();
        radius.set(r * i);
        uLocation.set(x * i, -y * i + mc.getWindow().getScaledHeight() * i - height * i);
        uSize.set(width * i, height * i);
        blurStrength.set(blur);
        refractionStrength.set(refraction);
        edgeWidth.set(edge * i);
        brightness.set(bright);
        saturation.set(sat);
        sampler.set(input.getColorAttachment());
    }

    public void use() {
        if (input != null && (input.textureWidth != mc.getWindow().getFramebufferWidth() || input.textureHeight != mc.getWindow().getFramebufferHeight()))
            input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());

        var buffer = mc.getFramebuffer();

        input.beginWrite(false);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, buffer.fbo);
        GL30.glBlitFramebuffer(0, 0, buffer.textureWidth, buffer.textureHeight, 0, 0, buffer.textureWidth, buffer.textureHeight, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
        buffer.beginWrite(false);

        inputResolution.set((float) buffer.textureWidth, (float) buffer.textureHeight);
        sampler.set(input.getColorAttachment());

        RenderSystem.setShader(LIQUID_GLASS.getProgram());
    }
}