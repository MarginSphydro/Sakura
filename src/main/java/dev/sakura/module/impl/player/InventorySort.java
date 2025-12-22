package dev.sakura.module.impl.player;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.NumberValue;
import dev.sakura.values.impl.BoolValue;

import java.util.*;

public class InventorySort extends Module {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private int delay = 0;

    public InventorySort() {
        super("InventorySort", Category.Player);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled()) return;
            if (mc.player == null || mc.interactionManager == null) return;
            tickSort();
        });
    }

    private final NumberValue<Integer> sortDelay =
            new NumberValue<>("SortDelay", 1, 0, 10, 1);


    @Override
    public void onEnable() {
        delay = 0;
    }

    @Override
    public void onDisable() {
        delay = 0;
    }

    private final BoolValue sortInGui =
            new BoolValue("SortInGui", false);


    private void tickSort() {

        // 打开界面时是否整理
        if (mc.currentScreen != null) {
            // 创造模式下打开 GUI 时禁止整理（防吞物品）
            if (mc.player.getAbilities().creativeMode) return;

            if (!sortInGui.get()) return;
        }

        if (delay > 0) {
            delay--;
            return;
        }

        PlayerInventory inv = mc.player.getInventory();

        // ===== 0. 合并一次同类 =====
        if (mergeOnce(inv)) {
            delay = sortDelay.get();
            return;
        }

        // ===== 1. 统计可堆叠物品总数 =====
        Map<ItemKey, Integer> totalCount = new HashMap<>();

        for (int i = 9; i < inv.main.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty() || !stack.isStackable()) continue;

            ItemKey key = new ItemKey(stack);
            totalCount.put(key, totalCount.getOrDefault(key, 0) + stack.getCount());
        }

        // ===== 2. 收集非空槽位 =====
        List<Integer> slots = new ArrayList<>();

        for (int i = 9; i < inv.main.size(); i++) {
            if (!inv.getStack(i).isEmpty()) {
                slots.add(i);
            }
        }

        // ===== 3. 排序规则 =====
        slots.sort((a, b) -> {
            ItemStack sa = inv.getStack(a);
            ItemStack sb = inv.getStack(b);

            boolean stackA = sa.isStackable();
            boolean stackB = sb.isStackable();

            if (stackA && !stackB) return -1;
            if (!stackA && stackB) return 1;

            if (stackA) {
                int ca = totalCount.getOrDefault(new ItemKey(sa), sa.getCount());
                int cb = totalCount.getOrDefault(new ItemKey(sb), sb.getCount());
                return Integer.compare(cb, ca);
            }

            return 0;
        });

        // ===== 4. 模拟玩家交换 =====
        for (int t = 0; t < slots.size(); t++) {
            int from = slots.get(t);
            int to = 9 + t;

            if (from == to) continue;

            clickSwap(from, to);
            delay = sortDelay.get();
            return;
        }
    }

    /* ===== 合并一对同类 ===== */

    private boolean mergeOnce(PlayerInventory inv) {
        int syncId = mc.player.currentScreenHandler.syncId;

        for (int i = 9; i < inv.main.size(); i++) {
            ItemStack a = inv.getStack(i);
            if (a.isEmpty() || !a.isStackable() || a.getCount() >= a.getMaxCount()) continue;

            for (int j = i + 1; j < inv.main.size(); j++) {
                ItemStack b = inv.getStack(j);
                if (b.isEmpty()) continue;

                if (!ItemStack.areItemsAndComponentsEqual(a, b)) continue;

                mc.interactionManager.clickSlot(syncId, j, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(syncId, j, 0, SlotActionType.PICKUP, mc.player);

                return true;
            }
        }
        return false;
    }

    /* ===== 交换两个槽位 ===== */

    private void clickSwap(int a, int b) {
        int syncId = mc.player.currentScreenHandler.syncId;

        mc.interactionManager.clickSlot(syncId, a, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, b, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, a, 0, SlotActionType.PICKUP, mc.player);
    }

    /* ===== ItemKey（1.21.4 安全） ===== */

    private static class ItemKey {
        private final ItemStack stack;

        ItemKey(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ItemKey k)) return false;
            return ItemStack.areItemsAndComponentsEqual(stack, k.stack);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    stack.getItem(),
                    stack.getComponents().hashCode()
            );
        }
    }
}
