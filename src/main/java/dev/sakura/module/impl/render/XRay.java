package dev.sakura.module.impl.render;

import dev.sakura.mixin.accessor.IMinecraftClient;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.BoolValue;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class XRay extends Module {
    public XRay() {
        super("XRay", "透视", Category.Render);
        initOreMap();
    }

    private final BoolValue coal = new BoolValue("Coal", false);
    private final BoolValue iron = new BoolValue("Iron", true);
    private final BoolValue gold = new BoolValue("Gold", true);
    private final BoolValue redstone = new BoolValue("Redstone", true);
    private final BoolValue lapis = new BoolValue("Lapis", true);
    private final BoolValue diamond = new BoolValue("Diamond", true);
    private final BoolValue emerald = new BoolValue("Emerald", true);
    private final BoolValue copper = new BoolValue("Copper", true);
    private final BoolValue netherite = new BoolValue("Netherite", true);
    private final BoolValue netherGold = new BoolValue("NetherGold", true);
    private final BoolValue netherQuartz = new BoolValue("NetherQuartz", false);

    private final Map<Block, BoolValue> oreMap = new HashMap<>();

    private void initOreMap() {
        Set.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COAL_BLOCK).forEach(b -> oreMap.put(b, coal));
        Set.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.IRON_BLOCK).forEach(b -> oreMap.put(b, iron));
        Set.of(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.RAW_GOLD_BLOCK, Blocks.GOLD_BLOCK).forEach(b -> oreMap.put(b, gold));
        Set.of(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.REDSTONE_BLOCK).forEach(b -> oreMap.put(b, redstone));
        Set.of(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.LAPIS_BLOCK).forEach(b -> oreMap.put(b, lapis));
        Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_BLOCK).forEach(b -> oreMap.put(b, diamond));
        Set.of(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_BLOCK).forEach(b -> oreMap.put(b, emerald));
        Set.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK, Blocks.COPPER_BLOCK).forEach(b -> oreMap.put(b, copper));
        Set.of(Blocks.ANCIENT_DEBRIS, Blocks.NETHERITE_BLOCK).forEach(b -> oreMap.put(b, netherite));
        Set.of(Blocks.NETHER_GOLD_ORE, Blocks.GILDED_BLACKSTONE).forEach(b -> oreMap.put(b, netherGold));
        Set.of(Blocks.NETHER_QUARTZ_ORE, Blocks.QUARTZ_BLOCK).forEach(b -> oreMap.put(b, netherQuartz));
    }

    @Override
    protected void onEnable() {
        ((IMinecraftClient) mc).setChunkCullingEnabled(false);
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    @Override
    protected void onDisable() {
        ((IMinecraftClient) mc).setChunkCullingEnabled(true);
        if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
        }
    }

    public boolean shouldRender(Block block) {
        if (!isEnabled()) return true;
        BoolValue setting = oreMap.get(block);
        return setting != null && setting.get();
    }

    public boolean isOre(Block block) {
        return oreMap.containsKey(block);
    }
}