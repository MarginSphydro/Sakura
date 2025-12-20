package dev.sakura.module.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.render.Render2DEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.nanovg.NanoVGRenderer;
import dev.sakura.nanovg.font.FontLoader;
import dev.sakura.nanovg.util.NanoVGHelper;
import dev.sakura.shaders.Shader2DUtils;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NameTag extends Module {
    public static NameTag INSTANCE;

    private final BoolValue armor = new BoolValue("Armor", true);
    private final BoolValue enchants = new BoolValue("Enchants", true, armor::get);
    private final BoolValue durability = new BoolValue("Durability", true, armor::get);
    private final BoolValue itemName = new BoolValue("ItemName", true);
    private final BoolValue ping = new BoolValue("Ping", true);
    private final BoolValue health = new BoolValue("Health", true);
    private final BoolValue pops = new BoolValue("Pops", true);
    private final BoolValue blur = new BoolValue("Blur", true);
    private final NumberValue<Double> blurStrength = new NumberValue<>("BlurStrength", 10.0, 1.0, 25.0, 0.5, blur::get);
    private final BoolValue self = new BoolValue("Self", false);

    private static final float BASE_SCALE = 1.0f;
    private static final float MAX_SCALE = 1.5f;

    private final Map<UUID, Integer> popCounts = new HashMap<>();

    public NameTag() {
        super("NameTag", Category.Render);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        popCounts.clear();
    }

    @Override
    protected void onDisable() {
        popCounts.clear();
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getType() != EventType.RECEIVE) return;

        if (event.getPacket() instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 35) {
                var entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity player) {
                    popCounts.put(player.getUuid(), popCounts.getOrDefault(player.getUuid(), 0) + 1);
                }
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !self.get()) continue;
            if (!player.isAlive()) continue;

            double x = player.prevX + (player.getX() - player.prevX) * mc.getRenderTickCounter().getTickDelta(true);
            double y = player.prevY + (player.getY() - player.prevY) * mc.getRenderTickCounter().getTickDelta(true);
            double z = player.prevZ + (player.getZ() - player.prevZ) * mc.getRenderTickCounter().getTickDelta(true);

            Vec3d pos = new Vec3d(x, y + player.getBoundingBox().getLengthY() + 0.5, z);
            Vec3d screenPos = worldToScreen(pos);

            if (screenPos != null && screenPos.z > 0 && screenPos.z < 1) {
                renderNameTag(event.getContext(), player, (float) screenPos.x, (float) screenPos.y);
            }
        }
    }

    private void renderNameTag(DrawContext context, PlayerEntity player, float posX, float posY) {
        final String name = player.getName().getString();
        final float hp = player.getHealth() + player.getAbsorptionAmount();
        final int playerPops = popCounts.getOrDefault(player.getUuid(), 0);
        final int playerPing = getPlayerPing(player);
        final float finalScale = Math.min(BASE_SCALE, MAX_SCALE);

        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(player.getMainHandStack());
        stacks.add(player.getInventory().armor.get(3));
        stacks.add(player.getInventory().armor.get(2));
        stacks.add(player.getInventory().armor.get(1));
        stacks.add(player.getInventory().armor.get(0));
        stacks.add(player.getOffHandStack());

        final String mainHandName = (itemName.get() && !player.getMainHandStack().isEmpty())
                ? getEnglishItemName(player.getMainHandStack()) : "";

        final float headSize = 28;
        final float itemSize = 16;
        final float itemSpacing = 2;
        final float padding = 8;
        final float headerHeight = 18;
        final float radius = 6;
        final float enchantFontSize = 7;
        final float durFontSize = 8;
        final float infoFontSize = 11;

        int maxEnchants = 0;
        if (enchants.get()) {
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) {
                    var ench = stack.get(DataComponentTypes.ENCHANTMENTS);
                    if (ench != null) {
                        int count = 0;
                        for (var e : ench.getEnchantments()) {
                            String s = getEnchantShortName(e);
                            if (!s.isEmpty()) count++;
                        }
                        maxEnchants = Math.max(maxEnchants, count);
                    }
                }
            }
        }

        float enchantHeight = maxEnchants * (enchantFontSize + 1);
        float durHeight = durability.get() ? durFontSize + 4 : 0;
        float nameHeight = (itemName.get() && !mainHandName.isEmpty()) ? 14 : 0;
        float itemsWidth = 6 * itemSize + 5 * itemSpacing;

        float contentHeight = enchantHeight + itemSize + durHeight + nameHeight + padding;
        float contentWidth = headSize + padding + itemsWidth + padding;

        float totalWidth = contentWidth + padding * 2;
        float totalHeight = headerHeight + contentHeight + padding;

        float boxX = posX - totalWidth / 2;
        float boxY = posY - totalHeight;

        context.getMatrices().push();
        context.getMatrices().translate(posX, posY, 0);
        context.getMatrices().scale(finalScale, finalScale, 1f);
        context.getMatrices().translate(-posX, -posY, 0);

        if (blur.get()) {
            Shader2DUtils.drawRoundedBlur(context.getMatrices(),
                    boxX, boxY, totalWidth, totalHeight, radius,
                    new Color(0, 0, 0, 0), blurStrength.get().floatValue(), 1.0f);
        }

        NanoVGRenderer.INSTANCE.draw(vg -> {
            NanoVGHelper.drawRoundRect(boxX, boxY, totalWidth, totalHeight, radius, new Color(158, 158, 158, 48));

            NanoVGHelper.drawRoundRect(boxX + 2, boxY + 2, totalWidth - 4, headerHeight - 2, radius - 2, new Color(171, 171, 172, 48));

            float headerY = boxY + headerHeight - 5;

            NanoVGHelper.drawString(name, boxX + padding, headerY, FontLoader.bold(11), infoFontSize, Color.WHITE);

            if (health.get()) {
                String healthStr = String.format("%.1f", hp);
                Color healthColor = getHealthColor(hp);
                float healthWidth = NanoVGHelper.getTextWidth(healthStr, FontLoader.regular(11), infoFontSize);
                float healthX = boxX + totalWidth / 2 - healthWidth / 2;
                NanoVGHelper.drawString(healthStr, healthX, headerY, FontLoader.bold(11), infoFontSize, healthColor);
            }

            if (ping.get()) {
                String pingStr = playerPing + "ms";
                Color pingColor = getPingColor(playerPing);
                float pingWidth = NanoVGHelper.getTextWidth(pingStr, FontLoader.regular(10), infoFontSize - 1);
                float pingX = boxX + totalWidth - padding - pingWidth;
                float signalX = pingX - 18;

                drawSignalIcon(signalX, boxY + headerHeight / 2 - 5, 12, playerPing, pingColor);
                NanoVGHelper.drawString(pingStr, pingX, headerY, FontLoader.regular(10), infoFontSize - 1, pingColor);
            }

            if (pops.get() && playerPops > 0) {
                String popStr = "-" + playerPops;
                float healthWidth = health.get() ? NanoVGHelper.getTextWidth(String.format("%.1f", hp), FontLoader.regular(11), infoFontSize) : 0;
                float popX = boxX + totalWidth / 2 + healthWidth / 2 + 8;
                NanoVGHelper.drawString(popStr, popX, headerY, FontLoader.bold(11), infoFontSize, new Color(255, 80, 80));
            }
        });

        float headX = boxX + padding;
        float headY = boxY + headerHeight + padding;

        PlayerListEntry entry = mc.getNetworkHandler() != null
                ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;

        if (entry != null && entry.getSkinTextures() != null) {
            NanoVGRenderer.INSTANCE.draw(vg -> {
                NanoVGHelper.drawRoundRect(headX - 1, headY - 1, headSize + 2, headSize + 2, 4, new Color(172, 172, 174, 47));
            });

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                    entry.getSkinTextures().texture(),
                    (int) headX, (int) headY,
                    8, 8,
                    (int) headSize, (int) headSize,
                    8, 8,
                    64, 64);
            context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured,
                    entry.getSkinTextures().texture(),
                    (int) headX, (int) headY,
                    40, 8,
                    (int) headSize, (int) headSize,
                    8, 8,
                    64, 64);
            RenderSystem.disableBlend();
        } else {
            NanoVGRenderer.INSTANCE.draw(vg -> {
                NanoVGHelper.drawRoundRect(headX, headY, headSize, headSize, 4, new Color(168, 168, 170, 52));
            });
        }

        if (armor.get()) {
            float itemAreaX = headX + headSize + padding;
            float itemY = headY + enchantHeight;

            float currentItemX = itemAreaX;

            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                final float itemX = currentItemX;

                if (!stack.isEmpty()) {
                    DiffuseLighting.disableGuiDepthLighting();
                    context.drawItem(stack, (int) itemX, (int) itemY);

                    if (stack.getCount() > 1) {
                        NanoVGRenderer.INSTANCE.draw(vg -> {
                            String countStr = String.valueOf(stack.getCount());
                            NanoVGHelper.drawString(countStr, itemX + 9, itemY + 12, FontLoader.bold(7), 7, Color.WHITE);
                        });
                    }

                    if (durability.get() && stack.getMaxDamage() > 0) {
                        float durabilityVal = stack.getMaxDamage() - stack.getDamage();
                        int percent = (int) ((durabilityVal / (float) stack.getMaxDamage()) * 100F);
                        Color durColor = getDurabilityColor(percent);

                        NanoVGRenderer.INSTANCE.draw(vg -> {
                            String percentStr = String.valueOf(percent);
                            float strWidth = NanoVGHelper.getTextWidth(percentStr, FontLoader.regular(8), durFontSize);
                            NanoVGHelper.drawString(percentStr, itemX + 8 - strWidth / 2, itemY + itemSize + durFontSize + 2, FontLoader.regular(8), durFontSize, durColor);
                        });
                    }

                    if (enchants.get()) {
                        var enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
                        if (enchantments != null && !enchantments.isEmpty()) {
                            NanoVGRenderer.INSTANCE.draw(vg -> {
                                float enchantY = itemY - 2;
                                for (var enchEntry : enchantments.getEnchantments()) {
                                    String shortName = getEnchantShortName(enchEntry);
                                    int level = enchantments.getLevel(enchEntry);
                                    if (shortName.isEmpty()) continue;

                                    String levelStr = level > 1 ? String.valueOf(level) : "";
                                    float shortWidth = NanoVGHelper.getTextWidth(shortName, FontLoader.regular(7), enchantFontSize);
                                    NanoVGHelper.drawString(shortName, itemX + 8 - shortWidth / 2, enchantY, FontLoader.regular(7), enchantFontSize, new Color(187, 187, 191, 52));
                                    if (!levelStr.isEmpty()) {
                                        NanoVGHelper.drawString(levelStr, itemX + 8 + shortWidth / 2, enchantY, FontLoader.regular(7), enchantFontSize, new Color(255, 100, 100));
                                    }
                                    enchantY -= (enchantFontSize + 1);
                                }
                            });
                        }
                    }
                }

                currentItemX += itemSize + itemSpacing;
            }

            if (itemName.get() && !mainHandName.isEmpty()) {
                float nameY = itemY + itemSize + durHeight + 6;

                NanoVGRenderer.INSTANCE.draw(vg -> {
                    float nameWidth = NanoVGHelper.getTextWidth(mainHandName, FontLoader.regular(10), 10);
                    float nameX = itemAreaX + itemsWidth / 2 - nameWidth / 2;
                    NanoVGHelper.drawString(mainHandName, nameX, nameY + 8, FontLoader.regular(10), 10, new Color(184, 184, 186, 184));
                });
            }
        }

        context.getMatrices().pop();
    }

    private String getEnglishItemName(ItemStack stack) {
        String key = stack.getItem().getTranslationKey();
        String name = key.substring(key.lastIndexOf('.') + 1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                result.append(' ');
            } else if (i == 0 || name.charAt(i - 1) == '_') {
                result.append(Character.toUpperCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private void drawSignalIcon(float x, float y, float size, int ping, Color color) {
        int bars;
        if (ping <= 50) {
            bars = 4;
        } else if (ping <= 100) {
            bars = 3;
        } else if (ping <= 200) {
            bars = 2;
        } else if (ping <= 400) {
            bars = 1;
        } else {
            bars = 0;
        }

        float barWidth = size / 5.5f;
        float spacing = size / 12f;
        float maxHeight = size;

        for (int i = 0; i < 4; i++) {
            float barHeight = maxHeight * (0.25f + 0.22f * i);
            float barX = x + i * (barWidth + spacing);
            float barY = y + maxHeight - barHeight;

            Color barColor;
            if (i < bars) {
                barColor = color;
            } else {
                barColor = new Color(163, 162, 162, 60);
            }

            NanoVGHelper.drawRoundRect(barX, barY, barWidth, barHeight, 1, barColor);
        }
    }

    private String getEnchantShortName(RegistryEntry<Enchantment> enchant) {
        String id = enchant.getIdAsString();
        if (id.contains("blast_protection")) return "Bla";
        if (id.contains("fire_protection")) return "Fir";
        if (id.contains("projectile_protection")) return "Pro";
        if (id.contains("protection")) return "Pro";
        if (id.contains("thorns")) return "Tho";
        if (id.contains("sharpness")) return "Sha";
        if (id.contains("efficiency")) return "Eff";
        if (id.contains("unbreaking")) return "Unb";
        if (id.contains("power")) return "Pow";
        if (id.contains("mending")) return "Men";
        if (id.contains("feather_falling")) return "Fea";
        if (id.contains("depth_strider")) return "Dep";
        if (id.contains("frost_walker")) return "Fro";
        if (id.contains("soul_speed")) return "Sou";
        if (id.contains("swift_sneak")) return "Swi";
        if (id.contains("respiration")) return "Res";
        if (id.contains("aqua_affinity")) return "Aqu";
        if (id.contains("fire_aspect")) return "Fir";
        if (id.contains("looting")) return "Loo";
        if (id.contains("knockback")) return "Kno";
        if (id.contains("smite")) return "Smi";
        if (id.contains("bane")) return "Ban";
        if (id.contains("sweeping")) return "Swe";
        if (id.contains("fortune")) return "For";
        if (id.contains("silk_touch")) return "Sil";
        if (id.contains("vanishing")) return "Van";
        if (id.contains("binding")) return "Bin";
        if (id.contains("loyalty")) return "Loy";
        if (id.contains("riptide")) return "Rip";
        if (id.contains("channeling")) return "Cha";
        if (id.contains("impaling")) return "Imp";
        if (id.contains("multishot")) return "Mul";
        if (id.contains("quick_charge")) return "Qui";
        if (id.contains("piercing")) return "Pie";
        return "";
    }

    private int getPlayerPing(PlayerEntity player) {
        if (mc.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private Color getHealthColor(float health) {
        if (health >= 15) return new Color(100, 255, 100);
        if (health >= 5) return new Color(255, 255, 100);
        return new Color(255, 100, 100);
    }

    private Color getPingColor(int ping) {
        if (ping <= 100) return new Color(100, 255, 100);
        if (ping <= 200) return new Color(255, 255, 100);
        return new Color(255, 100, 100);
    }

    private Color getDurabilityColor(int percent) {
        if (percent >= 70) return new Color(100, 255, 100);
        if (percent >= 30) return new Color(255, 255, 100);
        return new Color(255, 100, 100);
    }

    private Vec3d worldToScreen(Vec3d pos) {
        var camera = mc.gameRenderer.getCamera();
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        Vec3d camPos = camera.getPos();
        Vector3f camLook = camera.getHorizontalPlane();
        Vector3f camUp = camera.getVerticalPlane();
        Vector3f camLeft = new Vector3f();
        camLook.cross(camUp, camLeft);
        camLeft.normalize();

        float dx = (float) (pos.x - camPos.x);
        float dy = (float) (pos.y - camPos.y);
        float dz = (float) (pos.z - camPos.z);

        Vector3f toPos = new Vector3f(dx, dy, dz);

        float dotLook = toPos.dot(camLook);
        if (dotLook <= 0.01f) return null;

        float dotUp = toPos.dot(camUp);
        float dotLeft = toPos.dot(camLeft);

        float fov = mc.options.getFov().getValue().floatValue();
        float aspectRatio = (float) width / height;
        float tanHalfFov = (float) Math.tan(Math.toRadians(fov / 2.0));

        float screenX = width / 2f + (dotLeft / dotLook) / (tanHalfFov * aspectRatio) * (width / 2f);
        float screenY = height / 2f - (dotUp / dotLook) / tanHalfFov * (height / 2f);

        return new Vec3d(screenX, screenY, 1.0 / dotLook);
    }
}