/*
package dev.sakura.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.events.player.PlayerTickEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.Value;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EnvParticles extends Module {
    public EnvParticles() {
        super("EnvParticles", Category.Render);
    }

    private final Value<Boolean> fireflies = new BoolValue("Fireflies", true);
    private final Value<Integer> fireflyCount = new NumberValue<>("FireflyCount", 30, 20, 200, 5, fireflies::get);
    private final Value<Double> fireflySize = new NumberValue<>("FireflySize", 1.0, 0.1, 2.0, 0.1, fireflies::get);
    private final EnumValue<Mode> mode = new EnumValue<>("Mode", Mode.Snowflake);

*/
/*    private final EnumValue<PrecipitationType> precipitationSetting = new EnumValue<>("Precipitation", PrecipitationType.Snow, () -> mode.get() == Mode.VANILLA);
    private final Value<Integer> height = new NumberValue<>("Height", -64, -64, 320, 5, () -> mode.is(Mode.VANILLA));
    private final Value<Double> strength = new NumberValue<>("Strength", 0.8, 0.1, 2.0, 0.1, () -> mode.get() == Mode.VANILLA);
    private final Value<Color> weatherColor = new ColorValue("Color", Color.WHITE, () -> mode.get() == Mode.VANILLA);
    private final Value<Double> snowFallingSpeedMultiplier = new NumberValue<>("FallingSpeed", 1.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.VANILLA));
    private final Value<Integer> expandSize = new NumberValue<>("ExpandSize", 5, 1, 10, 1);*//*


    Value<Integer> particleCount = new NumberValue<>("Count", 100, 20, 800, 10, () -> !mode.is(Mode.Vanilla));
    Value<Double> particleSize = new NumberValue<>("Size", 1.0, 0.1, 6.0, 0.1, () -> !mode.is(Mode.Vanilla));
    EnumValue<ColorMode> colorModeConfig = new EnumValue<>("ColorMode", ColorMode.Rainbow, () -> !mode.is(Mode.Vanilla));
    Value<Integer> rainbowSpeed = new NumberValue<>("RainbowSpeed", 4, 1, 10, 1, () -> colorModeConfig.get() == ColorMode.Rainbow && !mode.is(Mode.Vanilla));
    Value<Integer> saturation = new NumberValue<>("Saturation", 130, 1, 255, 5, () -> colorModeConfig.get() == ColorMode.Rainbow && !mode.is(Mode.Vanilla));
    Value<Double> brightness = new NumberValue<>("Brightness", 1.0, 0.01, 1.0, 0.01, () -> colorModeConfig.get() == ColorMode.Rainbow && !mode.is(Mode.Vanilla));
    Value<Color> color = new ColorValue("Color", new Color(3649978), false, () -> colorModeConfig.get() == ColorMode.Custom && !mode.is(Mode.Vanilla));
    EnumValue<Physics> physics = new EnumValue<>("Physics", Physics.Fly, () -> !mode.is(Mode.Off) && !mode.is(Mode.Vanilla));

    private enum PrecipitationType {
        Rain,
        Snow,
        Both;

        private Biome.Precipitation toMC() {
            return switch (this) {
                case Both -> Biome.Precipitation.NONE;
                case Rain -> Biome.Precipitation.RAIN;
                case Snow -> Biome.Precipitation.SNOW;
            };
        }
    }

    public enum ColorMode {
        Custom,
        Rainbow
    }

    public enum Mode {
        Off,
        Snowflake,
        Stars,
        Hearts,
        Dollars,
        Bloom,
        Vanilla
    }

    public enum Physics {
        Drop,
        Fly
    }

    private static final Identifier DOLLAR = Identifier.of("sakura", "textures/particles/dollar.png");
    private static final Identifier FIREFLY = Identifier.of("sakura", "textures/particles/firefly.png");
    private static final Identifier HEART = Identifier.of("sakura", "textures/particles/heart.png");
    private static final Identifier SNOWFLAKE = Identifier.of("sakura", "textures/particles/snowflake.png");
    private static final Identifier STAR = Identifier.of("sakura", "textures/particles/star.png");
    private static final Identifier RAIN = Identifier.ofVanilla("textures/environment/rain.png");
    private static final Identifier SNOW = Identifier.ofVanilla("textures/environment/snow.png");

    private final ArrayList<ParticleBase> fireFlies = new ArrayList<>();
    private final ArrayList<ParticleBase> particles = new ArrayList<>();

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        fireFlies.removeIf(ParticleBase::tick);
        particles.removeIf(ParticleBase::tick);

        for (int i = fireFlies.size(); i < fireflyCount.get(); i++) {
            if (fireflies.get())
                fireFlies.add(new FireFly(
                        (float) (mc.player.getX() + MathUtility.random(-25f, 25f)),
                        (float) (mc.player.getY() + MathUtility.random(2f, 15f)),
                        (float) (mc.player.getZ() + MathUtility.random(-25f, 25f)),
                        MathUtility.random(-0.2f, 0.2f),
                        MathUtility.random(-0.1f, 0.1f),
                        MathUtility.random(-0.2f, 0.2f)));
        }

        for (int j = particles.size(); j < particleCount.get(); j++) {
            boolean drop = physics.get() == Physics.Drop;
            if (mode.get() != Mode.Off)
                particles.add(new ParticleBase(
                        (float) (mc.player.getX() + MathUtility.random(-48f, 48f)),
                        (float) (mc.player.getY() + MathUtility.random(2, 48f)),
                        (float) (mc.player.getZ() + MathUtility.random(-48f, 48f)),
                        drop ? 0 : MathUtility.random(-0.4f, 0.4f),
                        drop ? MathUtility.random(-0.2f, -0.05f) : MathUtility.random(-0.1f, 0.1f),
                        drop ? 0 : MathUtility.random(-0.4f, 0.4f)));
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (fireflies.get()) {
            event.getMatrices().push();
            RenderSystem.setShaderTexture(0, FIREFLY);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            fireFlies.forEach(p -> p.render(bufferBuilder));
            endBuilding(bufferBuilder);
            RenderSystem.depthMask(true);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            event.getMatrices().pop();
        }

        if (mode.get() != Mode.Off) {
            event.getMatrices().push();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            particles.forEach(p -> p.render(bufferBuilder));
            endBuilding(bufferBuilder);
            RenderSystem.depthMask(true);
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
            event.getMatrices().pop();
        }
    }

    private void endBuilding(BufferBuilder builder) {
        BuiltBuffer builtBuffer = builder.endNullable();
        if (builtBuffer != null) BufferRenderer.drawWithGlobalProgram(builtBuffer);
    }

    public class FireFly extends ParticleBase {
        private final List<Trails.Trail> trails = new ArrayList<>();

        public FireFly(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            super(posX, posY, posZ, motionX, motionY, motionZ);
        }

        @Override
        public boolean tick() {

            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 100) age -= 4;
            else if (!mc.world.getBlockState(new BlockPos((int) posX, (int) posY, (int) posZ)).isAir()) age -= 8;
            else age--;

            if (age < 0)
                return true;

            trails.removeIf(Trails.Trail::update);

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            trails.add(new Trails.Trail(new Vec3d(prevposX, prevposY, prevposZ), new Vec3d(posX, posY, posZ), lmode.getValue() == ColorMode.Sync ? HudEditor.getColor(age * 10) : color.getValue().getColorObject()));

            motionX *= 0.99f;
            motionY *= 0.99f;
            motionZ *= 0.99f;

            return false;
        }

        @Override
        public void render(BufferBuilder bufferBuilder) {
            RenderSystem.setShaderTexture(0, TextureStorage.firefly);
            if (!trails.isEmpty()) {
                Camera camera = mc.gameRenderer.getCamera();
                for (Trails.Trail ctx : trails) {
                    Vec3d pos = ctx.interpolate(1f);
                    MatrixStack matrices = new MatrixStack();
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                    matrices.translate(pos.x, pos.y, pos.z);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                    Matrix4f matrix = matrices.peek().getPositionMatrix();

                    bufferBuilder.vertex(matrix, 0, -ffsize.getValue(), 0).texture(0f, 1f).color(Render2DEngine.injectAlpha(ctx.color(), (int) (255 * ((float) age / (float) maxAge) * ctx.animation(Render3DEngine.getTickDelta()))).getRGB());
                    bufferBuilder.vertex(matrix, -ffsize.getValue(), -ffsize.getValue(), 0).texture(1f, 1f).color(Render2DEngine.injectAlpha(ctx.color(), (int) (255 * ((float) age / (float) maxAge) * ctx.animation(Render3DEngine.getTickDelta()))).getRGB());
                    bufferBuilder.vertex(matrix, -ffsize.getValue(), 0, 0).texture(1f, 0).color(Render2DEngine.injectAlpha(ctx.color(), (int) (255 * ((float) age / (float) maxAge) * ctx.animation(Render3DEngine.getTickDelta()))).getRGB());
                    bufferBuilder.vertex(matrix, 0, 0, 0).texture(0, 0).color(Render2DEngine.injectAlpha(ctx.color(), (int) (255 * ((float) age / (float) maxAge) * ctx.animation(Render3DEngine.getTickDelta()))).getRGB());
                }
            }
        }
    }

    public class ParticleBase {
        protected float prevposX, prevposY, prevposZ, posX, posY, posZ, motionX, motionY, motionZ;
        protected int age, maxAge;

        public ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            age = (int) MathUtility.random(100, 300);
            maxAge = age;
        }

        public boolean tick() {
            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 4096) age -= 8;
            else age--;

            if (age < 0)
                return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionX *= 0.9f;
            if (physics.getValue() == Physics.Fly)
                motionY *= 0.9f;
            motionZ *= 0.9f;

            motionY -= 0.001f;

            return false;
        }

        public void render(BufferBuilder bufferBuilder) {
            switch (mode.get()) {
                case Bloom -> RenderSystem.setShaderTexture(0, FIREFLY);
                case Snowflake -> RenderSystem.setShaderTexture(0, SNOWFLAKE);
                case Dollars -> RenderSystem.setShaderTexture(0, DOLLAR);
                case Hearts -> RenderSystem.setShaderTexture(0, HEART);
                case Stars -> RenderSystem.setShaderTexture(0, STAR);
            }

            Camera camera = mc.gameRenderer.getCamera();
            Color color1 = lmode.getValue() == ColorMode.Sync ? HudEditor.getColor(age * 2) : color.getValue().getColorObject();
            Vec3d pos = Render3DEngine.interpolatePos(prevposX, prevposY, prevposZ, posX, posY, posZ);

            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            Matrix4f matrix1 = matrices.peek().getPositionMatrix();

            bufferBuilder.vertex(matrix1, 0, -size.getValue(), 0).texture(0f, 1f).color(Render2DEngine.injectAlpha(color1, (int) (255 * ((float) age / (float) maxAge))).getRGB());
            bufferBuilder.vertex(matrix1, -size.getValue(), -size.getValue(), 0).texture(1f, 1f).color(Render2DEngine.injectAlpha(color1, (int) (255 * ((float) age / (float) maxAge))).getRGB());
            bufferBuilder.vertex(matrix1, -size.getValue(), 0, 0).texture(1f, 0).color(Render2DEngine.injectAlpha(color1, (int) (255 * ((float) age / (float) maxAge))).getRGB());
            bufferBuilder.vertex(matrix1, 0, 0, 0).texture(0, 0).color(Render2DEngine.injectAlpha(color1, (int) (255 * ((float) age / (float) maxAge))).getRGB());
        }
    }
}
*/
