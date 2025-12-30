package dev.sakura.module.impl.player;

import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.ColorValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;

import java.awt.*;

public class PacketMine extends Module {
    public PacketMine() {
        super("PacketMine", "发包挖掘", Category.Player);
    }

    public enum RemineMode {
        Instant,
        Normal,
        Fast
    }

    public enum Swap {
        Normal,
        Silent,
        SilentAlt,
        Off
    }

    private final BoolValue autoConfig = new BoolValue("Auto", "自动", false);
    private final BoolValue avoidSelfConfig = new BoolValue("AvoidSelf", "避开自身", false, autoConfig::get);
    private final BoolValue strictDirectionConfig = new BoolValue("StrictDirection", "严格方向", false, autoConfig::get);
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
}
