package dev.sakura.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.events.client.TickEvent;
import dev.sakura.events.render.Render3DEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.Value;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.random;

public class EnvParticles extends Module {
    public EnvParticles() {
        super("EnvParticles", Category.Render);
    }

    private final Value<Boolean> fireflies = new BoolValue("Fireflies", true);
    private final Value<Integer> fireflyCount = new NumberValue<>("FireflyCount", 30, 20, 200, 5, fireflies::get);
    private final Value<Double> fireflySize = new NumberValue<>("FireflySize", 1.0, 0.1, 2.0, 0.1, fireflies::get);
    private final EnumValue<Mode> mode = new EnumValue<>("Mode", Mode.SNOWFLAKE);

/*    private final EnumValue<PrecipitationType> precipitationSetting = new EnumValue<>("Precipitation", PrecipitationType.Snow, () -> mode.get() == Mode.VANILLA);
    private final Value<Integer> height = new NumberValue<>("Height", -64, -64, 320, 5, () -> mode.is(Mode.VANILLA));
    private final Value<Double> strength = new NumberValue<>("Strength", 0.8, 0.1, 2.0, 0.1, () -> mode.get() == Mode.VANILLA);
    private final Value<Color> weatherColor = new ColorValue("Color", Color.WHITE, () -> mode.get() == Mode.VANILLA);
    private final Value<Double> snowFallingSpeedMultiplier = new NumberValue<>("FallingSpeed", 1.0, 0.0, 10.0, 0.1, () -> mode.is(Mode.VANILLA));
    private final Value<Integer> expandSize = new NumberValue<>("ExpandSize", 5, 1, 10, 1);*/

    Value<Integer> particleCountConfig = new NumberValue<>("Count", 100, 20, 800, 10, () -> !mode.is(Mode.VANILLA));
    Value<Double> particleSizeConfig = new NumberValue<>("Size", 1.0, 0.1, 6.0, 0.1, () -> !mode.is(Mode.VANILLA));
    EnumValue<ColorMode> colorModeConfig = new EnumValue<>("ColorMode", ColorMode.RAINBOW, () -> !mode.is(Mode.VANILLA));
    Value<Integer> rainbowSpeedConfig = new NumberValue<>("RainbowSpeed", 4, 1, 10, 1, () -> colorModeConfig.get() == ColorMode.RAINBOW && !mode.is(Mode.VANILLA));
    Value<Integer> saturationConfig = new NumberValue<>("Saturation", 130, 1, 255, 5, () -> colorModeConfig.get() == ColorMode.RAINBOW && !mode.is(Mode.VANILLA));
    Value<Double> brightnessConfig = new NumberValue<>("Brightness", 1.0, 0.01, 1.0, 0.01, () -> colorModeConfig.get() == ColorMode.RAINBOW && !mode.is(Mode.VANILLA));
    Value<Color> colorConfig = new ColorValue("Color", new Color(3649978), false, () -> colorModeConfig.get() == ColorMode.CUSTOM && !mode.is(Mode.VANILLA));
    EnumValue<Physics> physicsConfig = new EnumValue<>("Physics", Physics.FLY, () -> !mode.is(Mode.OFF) && !mode.is(Mode.VANILLA));

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
        CUSTOM,
        RAINBOW
    }

    public enum Mode {
        OFF,
        SNOWFLAKE,
        STARS,
        HEARTS,
        DOLLARS,
        BLOOM,
        VANILLA
    }

    public enum Physics {
        DROP,
        FLY
    }

    private final java.util.List<FireFly> fireFlies = new ArrayList<>();
    private final java.util.List<ParticleBase> particles = new ArrayList<>();
    private final Random random = Random.create();

    private static final Identifier DOLLAR = Identifier.of("sakura", "textures/envparticles/dollar.png");
    private static final Identifier FIREFLY = Identifier.of("sakura", "textures/envparticles/firefly.png");
    private static final Identifier HEART = Identifier.of("sakura", "textures/envparticles/heart.png");
    private static final Identifier SNOWFLAKE = Identifier.of("sakura", "textures/envparticles/snowflake.png");
    private static final Identifier STAR = Identifier.of("sakura", "textures/envparticles/star.png");
    private static final Identifier RAIN = Identifier.ofVanilla("textures/environment/rain.png");
    private static final Identifier SNOW = Identifier.ofVanilla("textures/environment/snow.png");

/*    private int ticks = 0;
    private static final float[] weatherXCoords = new float[1024];
    private static final float[] weatherYCoords = new float[1024];

    static {
        for (int xRange = 0; xRange < 32; ++xRange) {
            for (int zRange = 0; zRange < 32; ++zRange) {
                float x = (float) (zRange - 16);
                float z = (float) (xRange - 16);
                float length = MathHelper.sqrt(x * x + z * z);
                weatherXCoords[xRange << 5 | zRange] = -z / length;
                weatherYCoords[xRange << 5 | zRange] = x / length;
            }
        }
    }*/

    @Override
    protected void onDisable() {
        fireFlies.clear();
        particles.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {

        //TODO: SNOW RENDER
        //if (mode.is(Mode.VANILLA)) ++ticks;
        if (mc.player == null || mc.world == null) {
            return;
        }

        fireFlies.removeIf(FireFly::tick);
        particles.removeIf(ParticleBase::tick);
        if (fireflies.get()) {
            while (fireFlies.size() < fireflyCount.get()) {
                fireFlies.add(new FireFly(
                        randomAroundPlayer(25f),
                        randomAroundPlayerY(2f, 15f),
                        randomAroundPlayer(25f),
                        randomVelocity(0.2f),
                        randomVelocity(0.1f),
                        randomVelocity(0.2f)
                ));
            }
        }

        if (!mode.is(Mode.OFF)) {
            boolean drop = physicsConfig.get() == Physics.DROP;
            while (particles.size() < particleCountConfig.get()) {
                particles.add(new ParticleBase(
                        randomAroundPlayer(48f),
                        randomAroundPlayerY(2f, 48f),
                        randomAroundPlayer(48f),
                        drop ? 0.0f : randomVelocity(0.4f),
                        drop ? MathHelper.nextFloat(random, -0.2f, -0.05f) : randomVelocity(0.1f),
                        drop ? 0.0f : randomVelocity(0.4f)
                ));
            }
        }
    }

    /*
    @EventHandler
    public void onWeatherRender(WeatherRenderEvent event) {
        if (modeConfig.getValue() != Mode.VANILLA) return;

        if (precipitationSetting.get().equals(PrecipitationType.Both)) {
            render(event, PrecipitationType.Rain);
            render(event, PrecipitationType.Snow);
            event.cancel();
            return;
        }

        render(event, precipitationSetting.get());

        event.cancel();
    }

    private void render(WeatherRenderEvent event, PrecipitationType precipitationType) {
        float cameraX = (float) event.getCameraX();
        float cameraY = (float) event.getCameraY();
        float cameraZ = (float) event.getCameraZ();

        float f = strength.getValue();
        float red = weatherColor.getValue().getRed() / 255f;
        float green = weatherColor.getValue().getGreen() / 255f;
        float blue = weatherColor.getValue().getBlue() / 255f;

        event.getLightmapTextureManager().enable();
        int cameraIntX = MathHelper.floor(cameraX);
        int cameraIntY = MathHelper.floor(cameraY);
        int cameraIntZ = MathHelper.floor(cameraZ);

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
        RenderSystem.setShader(ShaderProgramKeys.PARTICLE);

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int expand = expandSize.getValue();
        int tessPosition = -1;
        float fallingValue = (float) this.ticks + event.getTickDelta();

        BufferBuilder bufferBuilder = null;

        for (int zRange = cameraIntZ - expand; zRange <= cameraIntZ + expand; ++zRange) {
            for (int xRange = cameraIntX - expand; xRange <= cameraIntX + expand; ++xRange) {
                int coordPos = (zRange - cameraIntZ + 16) * 32 + xRange - cameraIntX + 16;

                if (coordPos < 0 || coordPos > 1023) continue;

                float xCoord = weatherXCoords[coordPos] * 0.5f;
                float zCoord = weatherYCoords[coordPos] * 0.5f;
                mutable.set(xRange, cameraY, zRange);

                int maxHeight = height.getValue();
                int minIntY = cameraIntY - expand;
                int expandedCameraY = cameraIntY + expand;
                if (minIntY < maxHeight) {
                    minIntY = maxHeight;
                }

                if (expandedCameraY < maxHeight) {
                    expandedCameraY = maxHeight;
                }

                int maxRenderY = Math.max(maxHeight, cameraIntY);

                if (minIntY != expandedCameraY) {
                    Random random = Random.create((long) xRange * xRange * 3121 + xRange * 45238971L ^ (long) zRange * zRange * 418711 + zRange * 13761L);
                    mutable.set(xRange, minIntY, zRange);
                    float texTextureV;
                    float weatherAlpha;
                    Biome.Precipitation precipitation = precipitationType.toMC();

                    if (precipitation == Biome.Precipitation.RAIN) {
                        if (tessPosition != 0) {
                            if (tessPosition >= 0) {
                                draw(bufferBuilder);
                            }

                            tessPosition = 0;
                            RenderSystem.setShaderTexture(0, RAIN);
                            bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
                        }

                        int randomSeed = this.ticks + xRange * xRange * 3121 + xRange * 45238971 + zRange * zRange * 418711 + zRange * 13761 & 31;
                        texTextureV = -((float) randomSeed + event.getTickDelta()) / 32.0F * (3.0F + random.nextFloat());
                        double xOffset = (double) xRange + 0.5 - cameraX;
                        double yOffset = (double) zRange + 0.5 - cameraZ;
                        float dLength = (float) Math.sqrt(xOffset * xOffset + yOffset * yOffset) / (float) expand;
                        weatherAlpha = ((1.0F - dLength * dLength) * 0.5F + 0.5F) * f;
                        mutable.set(xRange, maxRenderY, zRange);
                        int lightmapCoord = WorldRenderer.getLightmapCoordinates(mc.world, mutable);

                        bufferBuilder.vertex(xRange - cameraX - xCoord + 0.5F, expandedCameraY - cameraY, zRange - cameraZ - zCoord + 0.5F)
                                .texture(0.0F, (float) minIntY * 0.25F + texTextureV)
                                .color(red, green, blue, weatherAlpha)
                                .light(lightmapCoord);
                        bufferBuilder.vertex(xRange - cameraX + xCoord + 0.5F, expandedCameraY - cameraY, zRange - cameraZ + zCoord + 0.5F)
                                .texture(1.0F, (float) minIntY * 0.25F + texTextureV)
                                .color(red, green, blue, weatherAlpha)
                                .light(lightmapCoord);
                        bufferBuilder.vertex(xRange - cameraX + xCoord + 0.5F, minIntY - cameraY, zRange - cameraZ + zCoord + 0.5F)
                                .texture(1.0F, (float) expandedCameraY * 0.25F + texTextureV)
                                .color(red, green, blue, weatherAlpha)
                                .light(lightmapCoord);
                        bufferBuilder.vertex(xRange - cameraX - xCoord + 0.5F, minIntY - cameraY, zRange - cameraZ - zCoord + 0.5F)
                                .texture(0.0F, (float) expandedCameraY * 0.25F + texTextureV)
                                .color(red, green, blue, weatherAlpha)
                                .light(lightmapCoord);

                    } else if (precipitation == Biome.Precipitation.SNOW) {
                        if (tessPosition != 1) {
                            if (tessPosition == 0) {
                                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
                            }

                            tessPosition = 1;
                            RenderSystem.setShaderTexture(0, SNOW);
                            bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
                        }

                        float snowSmooth = -((float) (this.ticks & 511) + event.getTickDelta()) / 512.0F;
                        texTextureV = (float) (random.nextDouble() + (double) fallingValue * 0.01 * (double) ((float) random.nextGaussian()));
                        float fallingSpeed = (float) ((float) (random.nextDouble() + (double) (fallingValue * (float) random.nextGaussian()) * 0.001) * snowFallingSpeedMultiplier.getValue());
                        double xOffset = (double) xRange + 0.5 - cameraX;
                        double yOffset = (double) zRange + 0.5 - cameraZ;
                        weatherAlpha = (float) Math.sqrt(xOffset * xOffset + yOffset * yOffset) / (float) expand;
                        float snowAlpha = ((1.0F - weatherAlpha * weatherAlpha) * 0.3F + 0.5F) * f;
                        mutable.set(xRange, maxRenderY, zRange);
                        int lightMapCoord = WorldRenderer.getLightmapCoordinates(mc.world, mutable);
                        int lightmapCalcV = lightMapCoord >> 16 & '\uffff';
                        int lightmapCalcU = lightMapCoord & '\uffff';
                        int lightmapV = (lightmapCalcV * 3 + 240) / 4;
                        int lightmapU = (lightmapCalcU * 3 + 240) / 4;

                        bufferBuilder.vertex(xRange - cameraX - xCoord + 0.5F, expandedCameraY - cameraY, zRange - cameraZ - zCoord + 0.5F)
                                .texture(0.0F + texTextureV, (float) minIntY * 0.25F + snowSmooth + fallingSpeed)
                                .color(red, green, blue, snowAlpha)
                                .light(lightmapU, lightmapV);
                        bufferBuilder.vertex(xRange - cameraX + xCoord + 0.5F, expandedCameraY - cameraY, zRange - cameraZ + zCoord + 0.5F)
                                .texture(1.0F + texTextureV, (float) minIntY * 0.25F + snowSmooth + fallingSpeed)
                                .color(red, green, blue, snowAlpha)
                                .light(lightmapU, lightmapV);
                        bufferBuilder.vertex(xRange - cameraX + xCoord + 0.5F, minIntY - cameraY, zRange - cameraZ + zCoord + 0.5F)
                                .texture(1.0F + texTextureV, (float) expandedCameraY * 0.25F + snowSmooth + fallingSpeed)
                                .color(red, green, blue, snowAlpha)
                                .light(lightmapU, lightmapV);
                        bufferBuilder.vertex(xRange - cameraX - xCoord + 0.5F, minIntY - cameraY, zRange - cameraZ - zCoord + 0.5F)
                                .texture(0.0F + texTextureV, (float) expandedCameraY * 0.25F + snowSmooth + fallingSpeed)
                                .color(red, green, blue, snowAlpha)
                                .light(lightmapU, lightmapV);
                    }
                }
            }
        }

        if (bufferBuilder != null) {
            draw(bufferBuilder);
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        event.getLightmapTextureManager().disable();
    }*/

    @EventHandler
    public void onRenderWorld(Render3DEvent event) {
        if (mode.is(Mode.VANILLA)) return;

        renderFireflies(event.getMatrices());
        renderAmbient(event.getMatrices());
    }

    private void renderFireflies(MatrixStack matrices) {
        if (!fireflies.get() || fireFlies.isEmpty()) return;

        matrices.push();
        RenderSystem.setShaderTexture(0, FIREFLY);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (FireFly fireFly : fireFlies) fireFly.render(builder);

        draw(builder);
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void renderAmbient(MatrixStack matrices) {
        if (mode.is(Mode.OFF) || particles.isEmpty()) return;

        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (ParticleBase particle : particles) particle.render(builder);

        draw(builder);
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void draw(BufferBuilder builder) {
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private float randomAroundPlayer(float radius) {
        return (float) (mc.player.getX() + MathHelper.nextDouble(random, -radius, radius));
    }

    private float randomAroundPlayerY(float min, float max) {
        return (float) (mc.player.getY() + MathHelper.nextDouble(random, min, max));
    }

    private float randomVelocity(float amount) {
        return MathHelper.nextFloat(random, -amount, amount);
    }

    private Color resolveColor(int age, int maxAge) {
        if (colorModeConfig.get() == ColorMode.RAINBOW) {
            return rainbow(
                    rainbowSpeedConfig.get(),
                    age * 2,
                    saturationConfig.get() / 255.0f,
                    brightnessConfig.get().floatValue(),
                    1.0f
            );
        }
        return colorConfig.get();
    }

    private static Color withAlpha(Color color, float alpha) {
        int value = MathHelper.clamp((int) (alpha * 255.0f), 0, 255);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), value);
    }

    private static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / Math.max(speed, 1L) + index) % 360);
        float hue = angle / 360.0f;
        Color base = Color.getHSBColor(hue, MathHelper.clamp(saturation, 0.0f, 1.0f), MathHelper.clamp(brightness, 0.0f, 1.0f));
        int alpha = MathHelper.clamp((int) (opacity * 255.0f), 0, 255);
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    private Vec3d interpolatePos(float prevX, float prevY, float prevZ, float x, float y, float z) {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        double ix = prevX + (x - prevX) * tickDelta - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double iy = prevY + (y - prevY) * tickDelta - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double iz = prevZ + (z - prevZ) * tickDelta - mc.getEntityRenderDispatcher().camera.getPos().getZ();
        return new Vec3d(ix, iy, iz);
    }

    private class FireFly extends ParticleBase {
        private final List<Trail> trails = new ArrayList<>();

        public FireFly(float x, float y, float z, float motionX, float motionY, float motionZ) {
            super(x, y, z, motionX, motionY, motionZ);
        }

        @Override
        public boolean tick() {
            if (mc.player == null || mc.world == null) {
                return true;
            }
            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 100) {
                age -= 4;
            } else if (!mc.world.getBlockState(BlockPos.ofFloored(posX, posY, posZ)).isAir()) {
                age -= 8;
            } else {
                age--;
            }
            if (age < 0) {
                return true;
            }
            trails.removeIf(Trail::update);
            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            Color color = colorModeConfig.get() == ColorMode.RAINBOW
                    ? rainbow(rainbowSpeedConfig.get(), age * 10, saturationConfig.get() / 255.0f, brightnessConfig.get().floatValue(), 1.0f)
                    : colorConfig.get();
            trails.add(new Trail(new Vec3d(prevposX, prevposY, prevposZ), new Vec3d(posX, posY, posZ), color));
            motionX *= 0.99f;
            motionY *= 0.99f;
            motionZ *= 0.99f;
            return false;
        }

        @Override
        public void render(BufferBuilder builder) {
            if (trails.isEmpty()) return;

            Camera camera = mc.gameRenderer.getCamera();
            float size = fireflySize.get().floatValue();
            for (Trail trail : trails) {
                Vec3d pos = trail.interpolate(1.0f);
                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
                matrices.translate(pos.x, pos.y, pos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                float alpha = (float) age / (float) maxAge;
                float animation = trail.animation(mc.getRenderTickCounter().getTickDelta(true));
                int rgb = withAlpha(trail.color(), MathHelper.clamp(alpha * animation, 0.0f, 1.0f)).getRGB();
                builder.vertex(matrix, 0.0f, -size, 0.0f).texture(0.0f, 1.0f).color(rgb);
                builder.vertex(matrix, -size, -size, 0.0f).texture(1.0f, 1.0f).color(rgb);
                builder.vertex(matrix, -size, 0.0f, 0.0f).texture(1.0f, 0.0f).color(rgb);
                builder.vertex(matrix, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(rgb);
            }
        }
    }

    private class ParticleBase {
        protected float prevposX;
        protected float prevposY;
        protected float prevposZ;
        protected float posX;
        protected float posY;
        protected float posZ;
        protected float motionX;
        protected float motionY;
        protected float motionZ;
        protected int age;
        protected final int maxAge;

        public ParticleBase(float x, float y, float z, float motionX, float motionY, float motionZ) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.prevposX = x;
            this.prevposY = y;
            this.prevposZ = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.age = random.nextInt(201) + 100;
            this.maxAge = age;
        }

        public boolean tick() {
            if (mc.player == null) {
                return true;
            }
            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 4096) {
                age -= 8;
            } else {
                age--;
            }
            if (age < 0) {
                return true;
            }
            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
            motionX *= 0.9f;
            if (physicsConfig.get() == Physics.FLY) {
                motionY *= 0.9f;
            }
            motionZ *= 0.9f;
            motionY -= 0.001f;
            return false;
        }

        public void render(BufferBuilder builder) {
            Identifier texture = switch (mode.get()) {
                case BLOOM -> FIREFLY;
                case SNOWFLAKE -> SNOWFLAKE;
                case DOLLARS -> DOLLAR;
                case HEARTS -> HEART;
                case STARS -> STAR;
                default -> null;
            };
            if (texture == null) {
                return;
            }
            RenderSystem.setShaderTexture(0, texture);
            Camera camera = mc.gameRenderer.getCamera();
            Color base = resolveColor(age, maxAge);
            Vec3d pos = interpolatePos(prevposX, prevposY, prevposZ, posX, posY, posZ);
            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float size = particleSizeConfig.get().floatValue();
            float alpha = MathHelper.clamp((float) age / (float) maxAge, 0.0f, 1.0f);
            int rgb = withAlpha(base, alpha).getRGB();
            builder.vertex(matrix, 0.0f, -size, 0.0f).texture(0.0f, 1.0f).color(rgb);
            builder.vertex(matrix, -size, -size, 0.0f).texture(1.0f, 1.0f).color(rgb);
            builder.vertex(matrix, -size, 0.0f, 0.0f).texture(1.0f, 0.0f).color(rgb);
            builder.vertex(matrix, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(rgb);
        }
    }

    private static class Trail {
        private final Vec3d from;
        private final Vec3d to;
        private final Color color;
        private int ticks = 10;
        private int prevTicks;

        public Trail(Vec3d from, Vec3d to, Color color) {
            this.from = from;
            this.to = to;
            this.color = color;
        }

        public Vec3d interpolate(float pt) {
            MinecraftClient mc = MinecraftClient.getInstance();
            double x = from.x + (to.x - from.x) * pt - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double y = from.y + (to.y - from.y) * pt - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double z = from.z + (to.z - from.z) * pt - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            return new Vec3d(x, y, z);
        }

        public float animation(float pt) {
            return MathHelper.clamp((float) (prevTicks + (ticks - prevTicks) * pt) / 10.0f, 0.0f, 1.0f);
        }

        public boolean update() {
            prevTicks = ticks;
            return ticks-- <= 0;
        }

        public Color color() {
            return color;
        }
    }
}
