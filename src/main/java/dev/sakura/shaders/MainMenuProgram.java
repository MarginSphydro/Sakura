package dev.sakura.shaders;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.shaders.satin.api.ManagedCoreShader;
import dev.sakura.shaders.satin.api.ShaderEffectManager;
import dev.sakura.shaders.satin.api.uniform.Uniform1f;
import dev.sakura.shaders.satin.api.uniform.Uniform2f;
import dev.sakura.shaders.satin.api.uniform.Uniform4f;
import dev.sakura.utils.animations.AnimationUtil;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

import static dev.sakura.Sakura.mc;

public class MainMenuProgram {
    private Uniform1f Time;
    private Uniform2f uSize;
    private Uniform4f color;
    public static float time_ = 10000f;

    public static final ManagedCoreShader MAIN_MENU = ShaderEffectManager.getInstance()
            .manageCoreShader(Identifier.of("sakura", "core/mainmenu"), VertexFormats.POSITION);

    public MainMenuProgram() {
        setup();
    }

    public void setParameters(float x, float y, float width, float height) {
        float i = (float) mc.getWindow().getScaleFactor();
        this.uSize.set(width * i, height * i);
        time_ += (float) (0.55 * AnimationUtil.deltaTime());
        this.Time.set(time_);
    }

    public void use() {
        RenderSystem.setShader(MAIN_MENU.getProgram());
    }

    protected void setup() {
        uSize = MAIN_MENU.findUniform2f("uSize");
        Time = MAIN_MENU.findUniform1f("Time");
        color = MAIN_MENU.findUniform4f("color");
    }
}