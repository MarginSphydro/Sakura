package dev.sakura.client.module.impl.player.mine;

import dev.sakura.client.events.client.TickEvent;
import dev.sakura.client.events.player.BlockEvent;
import dev.sakura.client.events.render.Render3DEvent;
import dev.sakura.client.module.Category;
import dev.sakura.client.module.Module;
import dev.sakura.client.utils.client.ChatUtil;
import dev.sakura.client.utils.player.InvUtil;
import dev.sakura.client.utils.render.Render3DUtil;
import dev.sakura.client.values.impl.BoolValue;
import dev.sakura.client.values.impl.ColorValue;
import dev.sakura.client.values.impl.EnumValue;
import dev.sakura.client.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.List;

public class PacketMine extends Module {
    private final BoolValue autoConfig = new BoolValue("Auto", "自动", false);
    private final BoolValue avoidSelfConfig = new BoolValue("AvoidSelf", "避开自身", false, autoConfig::get);
    private final BoolValue strictDirectionConfig = new BoolValue("StrictDirection", "严格方向", false, autoConfig::get);
    private final BoolValue eatingPause = new BoolValue("EatingPause", "吃东西时暂停", false, autoConfig::get);
    private final NumberValue<Double> enemyRangeConfig = new NumberValue<>("EnemyRange", "敌人范围", 5.0, 1.0, 10.0, 0.1, autoConfig::get);
    private final BoolValue antiCrawlConfig = new BoolValue("AntiCrawl", "反爬行", false);
    private final BoolValue headConfig = new BoolValue("TargetBody", "目标身体", false, autoConfig::get);
    private final BoolValue aboveHeadConfig = new BoolValue("TargetHead", "目标头部", false, autoConfig::get);
    private final BoolValue doubleBreakConfig = new BoolValue("DoubleBreak", "双重破坏", false);
    private final NumberValue<Integer> mineTicksConfig = new NumberValue<>("MiningTicks", "挖掘刻数", 20, 5, 60, 1, doubleBreakConfig::get);
    private final EnumValue<RemineMode> remineConfig = new EnumValue<>("Remine", "重挖", RemineMode.Normal);
    private final BoolValue packetInstantConfig = new BoolValue("Fast", "快速", false, () -> remineConfig.is(RemineMode.Instant));
    private final NumberValue<Double> rangeConfig = new NumberValue<>("Range", "范围", 4.0, 0.1, 6.0, 0.1);
    private final NumberValue<Double> speedConfig = new NumberValue<>("Speed", "速度", 1.0, 0.1, 1.0, 0.1);
    private final EnumValue<Swap> swapConfig = new EnumValue<>("AutoSwap", "自动切换", Swap.Silent);
    private final BoolValue swapBeforeConfig = new BoolValue("SwapBefore", "挖前切换", false, () -> !swapConfig.is(Swap.Off));
    private final BoolValue rotateConfig = new BoolValue("Rotate", "旋转", true);
    private final BoolValue switchResetConfig = new BoolValue("SwitchReset", "切换重置", false);
    private final BoolValue grimConfig = new BoolValue("Grim", "Grim", false);
    private final BoolValue grimNewConfig = new BoolValue("GrimV3", "GrimV3", false, grimConfig::get);
    private final ColorValue sideColor = new ColorValue("MineSide", "挖掘面颜色", new Color(255, 0, 0, 31));
    private final ColorValue lineColor = new ColorValue("MineLine", "挖掘线颜色", new Color(255, 0, 0, 233));
    private final ColorValue doneSideColor = new ColorValue("DoneSide", "完成面颜色", new Color(0, 255, 0, 23));
    private final ColorValue doneLineColor = new ColorValue("DoneLine", "完成线颜色", new Color(0, 255, 0, 233));
    private final NumberValue<Integer> fadeTimeConfig = new NumberValue<>("Fade-Time", "淡出时间", 250, 0, 1000, 1, () -> false);
    private final BoolValue smoothColorConfig = new BoolValue("SmoothColor", "平滑颜色", false, () -> false);
    private final BoolValue debugConfig = new BoolValue("Debug", "调试", false);
    private final ColorValue packetSide = new ColorValue("PacketSide", "发包面颜色", new Color(0, 0, 255, 31), debugConfig::get);
    private final ColorValue packetLine = new ColorValue("PacketLine", "发包线颜色", new Color(0, 0, 255, 233), debugConfig::get);
    private final ColorValue instantSide = new ColorValue("InstantSide", "瞬挖面颜色", new Color(255, 0, 255, 31), debugConfig::get);
    private final ColorValue instantLine = new ColorValue("InstantLine", "瞬挖线颜色", new Color(255, 0, 255, 233), debugConfig::get);
    private final BoolValue debugTicksConfig = new BoolValue("Debug-Ticks", "调试刻数", false, debugConfig::get);
    private BlockData blockData = null;

    public PacketMine() {
        super("PacketMine", "发包挖掘", Category.Player);
    }

    // 获取点击方块事件
    @EventHandler
    public void onClickBlock(BlockEvent event) {
        if (eatingCheck()) return;
        if (blockData == null || event.getBlockPos() != blockData.getCurrentPos()) {
            if (canBreak(event.getBlockPos())) {
                if (debugConfig.get()) ChatUtil.sendMessage("[PacketMine] Setting fucking blockData.");
                blockData = new BlockData(
                        event.getBlockPos(),
                        event.getDirection(),
                        System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (blockData == null) return;
        if (mc.player.squaredDistanceTo(blockData.getCurrentPos().toCenterPos()) > (rangeConfig.get() + 4) * (rangeConfig.get() + 4)) {
            if (debugConfig.get()) ChatUtil.sendMessage("[PacketMine] set blockData = null.");
            blockData = null;
            return;
        }
        if (eatingCheck()) return;
        if (mc.world.isAir(blockData.getCurrentPos())) return;
        if (System.currentTimeMillis() - blockData.getStartTime() > calcBreakTime(blockData.getCurrentPos(), swapConfig.get() == Swap.SilentAlt)) {
            if (debugConfig.get()) ChatUtil.sendMessage("[PacketMine] PASS.");
            Integer slot = InvUtil.findFastestTool(mc.world.getBlockState(blockData.getCurrentPos()), swapConfig.get() == Swap.SilentAlt).slot();
            switch (swapConfig.get()) {
                case Silent:
                    InvUtil.swap(slot, true);
                    break;
                case SilentAlt:
                    InvUtil.invSwap(slot);
                    break;
                case Normal:
                    InvUtil.swap(slot, false);
                    break;
                case Off:
                    break;
            }
            stopMiningInternal(blockData);
            switch (swapConfig.get()) {
                case Silent:
                    InvUtil.swapBack();
                    break;
                case SilentAlt:
                    InvUtil.invSwapBack();
                    break;
                default:
                    break;
            }
            if (remineConfig.get() == RemineMode.Normal)
                blockData = new BlockData(blockData.getCurrentPos(), blockData.getDirection(), System.currentTimeMillis());
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (blockData == null) return;
        Box box = new Box(blockData.getCurrentPos());
        Render3DUtil.drawFilledBox(event.getMatrices(), box, sideColor.get());
        Render3DUtil.drawBoxOutline(event.getMatrices(), box, lineColor.get().getRGB(), 1f);
        Vec3d center = new Vec3d(
                box.minX + (box.maxX - box.minX) * 0.5,
                box.minY + (box.maxY - box.minY) * 0.5,
                box.minZ + (box.maxZ - box.minZ) * 0.5
        );
        double progress = (System.currentTimeMillis() - blockData.getStartTime()) / calcBreakTime(blockData.getCurrentPos(), swapConfig.get() == Swap.SilentAlt);
        Render3DUtil.drawText((progress > 1) ? "Completed" : String.format("%.1f", progress * 100.0) + "%", center, 0, 0, 0, Color.WHITE);
    }

    public boolean canBreak(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        List<Block> blackList = List.of(
                Blocks.AIR,
                Blocks.BEDROCK,
                Blocks.END_PORTAL_FRAME,
                Blocks.END_PORTAL,
                Blocks.WATER,
                Blocks.WATER_CAULDRON,
                Blocks.LAVA,
                Blocks.LAVA_CAULDRON,
                Blocks.FIRE
        );
        return !blackList.contains(state.getBlock());
    }

    private void stopMiningInternal(BlockData data) {
        if (debugConfig.get()) ChatUtil.sendMessage("[PacketMine] SENDING MINE PACKET.");
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getCurrentPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getCurrentPos(), data.getDirection()));
    }

    public float calcBreakTime(BlockPos pos, boolean inventory) {
        BlockState blockState = mc.world.getBlockState(pos);

        float hardness = blockState.getHardness(mc.world, pos);

        float breakSpeed = +getBreakSpeed(blockState, inventory);

        if (breakSpeed == -1.0f) {
            return -1.0f;
        }

        float relativeDamage = breakSpeed / hardness / 30.0f;

        int ticks = MathHelper.ceil(0.7f / relativeDamage);

        return (float) ticks * 50.0f;
    }

    public float getBreakSpeed(BlockState blockState, boolean inventory) {
        float maxSpeed = 1.0f;

        int limit = inventory ? mc.player.getInventory().size() : 9;

        for (int i = 0; i < limit; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            float speed = stack.getMiningSpeedMultiplier(blockState);

            if (speed > 1.0f) {
                var enchantmentRegistry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                RegistryEntry<Enchantment> efficiencyEntry = enchantmentRegistry.getOrThrow(Enchantments.EFFICIENCY);

                int efficiencyLevel = EnchantmentHelper.getLevel(efficiencyEntry, stack);

                if (efficiencyLevel > 0) {
                    speed += (float) (efficiencyLevel * efficiencyLevel + 1);
                }

                if (speed > maxSpeed) {
                    maxSpeed = speed;
                }
            }
        }

        return maxSpeed;
    }

    private boolean eatingCheck() {
        return eatingPause.get() && onEating();
    }

    private boolean onEating() {
        return mc.player.isUsingItem() && mc.player.getMainHandStack().contains(DataComponentTypes.FOOD);
    }

    public enum RemineMode {
        Instant,
        Normal
    }

    public enum Swap {
        Normal,
        Silent,
        SilentAlt,
        Off
    }
}
