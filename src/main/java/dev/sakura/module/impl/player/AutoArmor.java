package dev.sakura.module.impl.player;

import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.NumberValue;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

import java.util.Arrays;
import java.util.Comparator;

import static net.minecraft.entity.EquipmentSlot.*;

public class AutoArmor extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final ArmorPiece[] armorPieces = new ArmorPiece[4];
    private final ArmorPiece helmet = new ArmorPiece(HEAD);
    private final ArmorPiece chestplate = new ArmorPiece(CHEST);
    private final ArmorPiece leggings = new ArmorPiece(LEGS);
    private final ArmorPiece boots = new ArmorPiece(FEET);

    // 新增整理速度选项
    private final NumberValue<Integer> delayTicksValue =
            new NumberValue<>("delayTicks", "延迟", 8, 1, 20, 1); // 默认 8 ticks，可调 1~20

    // 新增可选：是否在打开 GUI 时整理
    private final BoolValue guiSort = new BoolValue("SortInGUI", "GUI整理", false);

    public AutoArmor() {
        super("AutoArmor", "自动装甲", Category.Player);

        armorPieces[0] = boots;
        armorPieces[1] = leggings;
        armorPieces[2] = chestplate;
        armorPieces[3] = helmet;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (isEnabled()) onTick();
        });
    }

    @Override
    public void onEnable() {
        for (ArmorPiece piece : armorPieces) piece.resetTimer();
    }

    @Override
    public void onDisable() {
        for (ArmorPiece piece : armorPieces) piece.resetTimer();
    }

    private void onTick() {
        if (mc.player == null) return;

        Screen currentScreen = mc.currentScreen;

        // 可选：是否允许在 GUI 打开时整理
        if (!guiSort.get() && currentScreen != null) return;
        if (mc.player.isCreative() && currentScreen != null) return;
        // 每件盔甲减延迟
        for (ArmorPiece piece : armorPieces) piece.tickTimer();
        // 重置每件盔甲
        for (ArmorPiece piece : armorPieces) piece.reset();
        // 遍历玩家背包
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || !isArmor(stack)) continue;

            int slotId = getItemSlotId(stack);
            switch (slotId) {
                case 0 -> boots.add(stack, i);
                case 1 -> leggings.add(stack, i);
                case 2 -> chestplate.add(stack, i);
                case 3 -> helmet.add(stack, i);
            }
        }

        // 计算每件盔甲当前装备评分
        for (ArmorPiece piece : armorPieces) piece.calculate();

        // 按评分排序（高分优先）
        Arrays.sort(armorPieces, Comparator.comparingInt(ArmorPiece::getSortScore).reversed());

        // 尝试装备
        for (ArmorPiece piece : armorPieces) piece.apply();
    }

    private boolean isArmor(ItemStack stack) {
        return stack.isIn(ItemTags.FOOT_ARMOR) || stack.isIn(ItemTags.LEG_ARMOR)
                || stack.isIn(ItemTags.CHEST_ARMOR) || stack.isIn(ItemTags.HEAD_ARMOR);
    }

    private int getItemSlotId(ItemStack stack) {
        if (stack.getItem() == Items.ELYTRA) return 2; // 胸甲
        if (stack.isIn(ItemTags.FOOT_ARMOR)) return 0;
        if (stack.isIn(ItemTags.LEG_ARMOR)) return 1;
        if (stack.isIn(ItemTags.CHEST_ARMOR)) return 2;
        if (stack.isIn(ItemTags.HEAD_ARMOR)) return 3;
        return -1;
    }

    private class ArmorPiece {
        private final EquipmentSlot slot;
        private int bestSlot;
        private int bestScore;
        private int score;
        private int durability;

        private int timer;

        public ArmorPiece(EquipmentSlot slot) {
            this.slot = slot;
        }

        public void reset() {
            bestSlot = -1;
            bestScore = -1;
            score = -1;
            durability = Integer.MAX_VALUE;
        }

        public void resetTimer() {
            timer = 0;
        }

        public void tickTimer() {
            if (timer > 0) timer--;
        }

        public void add(ItemStack stack, int slot) {
            int s = getScore(stack);
            if (s > bestScore) {
                bestScore = s;
                bestSlot = slot;
            }
        }

        public void calculate() {
            ItemStack equipped = mc.player.getEquippedStack(slot);
            score = getScore(equipped);
            if (!equipped.isEmpty()) {
                durability = equipped.getMaxDamage() - equipped.getDamage();
            }
        }

        public int getSortScore() {
            return bestScore;
        }

        public void apply() {
            if (timer > 0) return;
            if (bestScore > score && bestSlot != -1) {
                ItemStack fromStack = mc.player.getInventory().getStack(bestSlot);
                ItemStack equipped = mc.player.getEquippedStack(slot);

                mc.player.getInventory().setStack(bestSlot, equipped);
                mc.player.equipStack(slot, fromStack);

                timer = delayTicksValue.get(); // 使用可调延迟
            }
        }

        private int getScore(ItemStack stack) {
            if (stack.isEmpty()) return 0;
            int s = 0;
            if (stack.hasEnchantments()) s += 10;
            s += stack.getMaxDamage() - stack.getDamage();
            return s;
        }
    }
}
