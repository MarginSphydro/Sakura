package dev.sakura.client.mixin.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.client.shaders.satin.impl.SamplerAccess;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramDefinition;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ShaderProgram.class)
public abstract class MixinShaderProgram implements SamplerAccess {
    @Shadow
    @Final
    private List<ShaderProgramDefinition.Sampler> samplers;

    @SuppressWarnings("all")
    @Override
    public boolean hasSampler(String name) {
        for (ShaderProgramDefinition.Sampler sampler : this.samplers) {
            if (sampler.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sakura$addSamplerTexture(String name, int textureId) {
        if (this.samplers != null) {
            for (int i = 0; i < this.samplers.size(); ++i) {
                ShaderProgramDefinition.Sampler sampler = this.samplers.get(i);
                if (sampler.name().equals(name)) {
                    RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                    RenderSystem.bindTexture(textureId);
                    int location = this.getSamplerLocations().getInt(i);
                    if (location != -1) {
                        GL20.glUniform1i(location, i);
                    }
                    return;
                }
            }
        }
    }

    @Override
    @Accessor("samplers")
    public abstract List<ShaderProgramDefinition.Sampler> getSamplers();

    @Override
    @Accessor("samplerLocations")
    public abstract IntList getSamplerLocations();
}
