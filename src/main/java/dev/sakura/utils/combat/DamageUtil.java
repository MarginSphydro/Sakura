package dev.sakura.utils.combat;

import dev.sakura.utils.player.InvUtil;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import static dev.sakura.Sakura.mc;

public class DamageUtil {

    public static float calculateDamage(BlockPos pos, LivingEntity target) {
        return calculateDamage(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, target, target, 6);
    }

    public static float calculateDamage(double posX, double posY, double posZ, LivingEntity entity, Box predictBox, float power) {
        if (entity instanceof PlayerEntity player && player.getAbilities().creativeMode) return 0;
        if (predictBox == null) return 0;

        float doubleExplosionSize = 2 * power;
        Vec3d entityPos = predictBox.getCenter(); // Approximation
        double distancedsize = MathHelper.sqrt((float) entityPos.squaredDistanceTo(posX, posY, posZ)) / (double) doubleExplosionSize;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = getExposure(vec3d, predictBox);
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * (double) doubleExplosionSize + 1.0);
        double finald = getBlastReduction(entity, getDamageMultiplied(damage));
        return (float) finald;
    }

    public static float calculateDamage(double posX, double posY, double posZ, LivingEntity entity, Entity predict, float power) {
        return calculateDamage(posX, posY, posZ, entity, predict != null ? predict.getBoundingBox() : entity.getBoundingBox(), power);
    }

    public static float getDamageAfterAbsorb(float damage, float totalArmor, float toughnessAttribute) {
        float f = 2.0F + toughnessAttribute / 4.0F;
        float f1 = MathHelper.clamp(totalArmor - damage / f, totalArmor * 0.2F, 20.0F);
        return damage * (1.0F - f1 / 25.0F);
    }

    public static float getBlastReduction(LivingEntity entity, float damageI) {
        float damage = damageI;
        if (entity instanceof PlayerEntity player) {
            damage = getDamageAfterAbsorb(damage, (float) player.getArmor(), (float) player.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
            int k = getProtectionAmount(player.getArmorItems());
            float f = MathHelper.clamp((float) k, 0.0f, 20.0f);
            damage *= 1.0f - f / 25.0f;
            if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                damage -= damage / 4.0f;
            }
            return Math.max(damage, 0.0f);
        }
        damage = getDamageAfterAbsorb(damage, (float) entity.getArmor(), (float) entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
        return Math.max(damage, 0.0f);
    }

    public static int getProtectionAmount(Iterable<ItemStack> armorItems) {
        int value = 0;
        for (ItemStack itemStack : armorItems) {
            int level = InvUtil.getEnchantmentLevel(itemStack, Enchantments.PROTECTION);
            if (level == 0) {
                value += InvUtil.getEnchantmentLevel(itemStack, Enchantments.BLAST_PROTECTION) * 2;
            } else {
                value += level;
            }
        }
        return value;
    }

    public static float getDamageMultiplied(float damage) {
        int diff = mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }

    public static float getExposure(Vec3d source, Box box) {
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / e) * e) / 2.0;
        double i = (1.0 - Math.floor(1.0 / f) * f) / 2.0;
        if (!(d < 0.0) && !(e < 0.0) && !(f < 0.0)) {
            int j = 0;
            int k = 0;

            for (float l = 0.0F; l <= 1.0F; l = (float) ((double) l + d)) {
                for (float m = 0.0F; m <= 1.0F; m = (float) ((double) m + e)) {
                    for (float n = 0.0F; n <= 1.0F; n = (float) ((double) n + f)) {
                        double o = MathHelper.lerp((double) l, box.minX, box.maxX);
                        double p = MathHelper.lerp((double) m, box.minY, box.maxY);
                        double q = MathHelper.lerp((double) n, box.minZ, box.maxZ);
                        Vec3d vec3d = new Vec3d(o + g, p + h, q + i);
                        if (raycast(vec3d, source) == HitResult.Type.MISS) {
                            ++j;
                        }

                        ++k;
                    }
                }
            }

            return (float) j / (float) k;
        } else {
            return 0.0F;
        }
    }

    private static HitResult.Type raycast(Vec3d start, Vec3d end) {
        return BlockView.raycast(start, end, null, (context, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock().getBlastResistance() < 600) return null;

            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (context) -> HitResult.Type.MISS);
    }
}
