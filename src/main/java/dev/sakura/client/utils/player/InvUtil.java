package dev.sakura.client.utils.player;

import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

import static dev.sakura.client.Sakura.mc;

public class InvUtil {
    public static int previousSlot = -1;
    public static int[] invSlots;

    public static int getEnchantmentLevel(ItemStack stack, RegistryKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        return mc.world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOptional(enchantment)
                .map(enchantmentReference -> EnchantmentHelper.getLevel(enchantmentReference, stack)).orElse(0);
    }

    public static boolean testInMainHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getMainHandStack());
    }

    public static boolean testInMainHand(Item... items) {
        return testInMainHand(itemStack -> {
            for (var item : items) if (itemStack.isOf(item)) return true;
            return false;
        });
    }

    public static boolean testInOffHand(Predicate<ItemStack> predicate) {
        return predicate.test(mc.player.getOffHandStack());
    }

    public static boolean testInOffHand(Item... items) {
        return testInOffHand(itemStack -> {
            for (var item : items) if (itemStack.isOf(item)) return true;
            return false;
        });
    }

    public static boolean testInHands(Predicate<ItemStack> predicate) {
        return testInMainHand(predicate) || testInOffHand(predicate);
    }

    public static boolean testInHands(Item... items) {
        return testInMainHand(items) || testInOffHand(items);
    }

    public static boolean testInHotbar(Predicate<ItemStack> predicate) {
        if (testInHands(predicate)) return true;

        for (int i = SlotUtil.HOTBAR_START; i <= SlotUtil.HOTBAR_END; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) return true;
        }

        return false;
    }

    public static boolean testInHotbar(Item... items) {
        return testInHotbar(itemStack -> {
            for (var item : items) if (itemStack.isOf(item)) return true;
            return false;
        });
    }

    public static FindItemResult findEmpty() {
        return find(ItemStack::isEmpty);
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult findInHotbar(Predicate<ItemStack> isGood) {
        if (testInOffHand(isGood)) {
            return new FindItemResult(SlotUtil.OFFHAND, mc.player.getOffHandStack().getCount());
        }

        if (testInMainHand(isGood)) {
            return new FindItemResult(mc.player.getInventory().selectedSlot, mc.player.getMainHandStack().getCount());
        }

        return find(isGood, 0, 8);
    }

    public static FindItemResult find(Item... items) {
        return find(itemStack -> {
            for (Item item : items) {
                if (itemStack.getItem() == item) return true;
            }
            return false;
        });
    }

    public static FindItemResult find(Predicate<ItemStack> isGood) {
        if (mc.player == null) return new FindItemResult(0, 0);
        return find(isGood, 0, mc.player.getInventory().size());
    }

    public static FindItemResult find(Predicate<ItemStack> isGood, int start, int end) {
        if (mc.player == null) return new FindItemResult(0, 0);

        int slot = -1, count = 0;

        for (int i = start; i <= end; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
                count += stack.getCount();
            }
        }

        return new FindItemResult(slot, count);
    }

    public static FindItemResult findFastestTool(BlockState state) {
        float bestScore = 1;
        int slot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isSuitableFor(state)) continue;

            float score = stack.getMiningSpeedMultiplier(state);
            if (score > bestScore) {
                bestScore = score;
                slot = i;
            }
        }

        return new FindItemResult(slot, 1);
    }

    public static boolean swap(int slot, boolean swapBack) {
        if (slot == SlotUtil.OFFHAND) return true;
        if (slot < 0 || slot > 8) return false;
        if (swapBack && previousSlot == -1) previousSlot = mc.player.getInventory().selectedSlot;
        else if (!swapBack) previousSlot = -1;

        mc.player.getInventory().setSelectedSlot(slot);
        mc.interactionManager.syncSelectedSlot();
        return true;
    }

    public static boolean swapBack() {
        if (previousSlot == -1) return false;

        boolean return_ = swap(previousSlot, false);
        previousSlot = -1;
        return return_;
    }

    public static boolean invSwap(int slot) {
        if (slot >= 0) {
            int containerSlot = slot;
            if (slot < 9) containerSlot += 36;
            else if (slot == 40) containerSlot = 45;

            ScreenHandler handler = mc.player.currentScreenHandler;
            int selectedSlot = mc.player.getInventory().selectedSlot;

            mc.interactionManager.clickSlot(handler.syncId, containerSlot, selectedSlot, SlotActionType.SWAP, mc.player);

            invSlots = new int[]{containerSlot, selectedSlot};
            return true;
        }
        return false;
    }

    public static void invSwapBack() {
        if (invSlots == null || invSlots.length < 2) return;
        ScreenHandler handler = mc.player.currentScreenHandler;

        mc.interactionManager.clickSlot(handler.syncId, invSlots[0], invSlots[1], SlotActionType.SWAP, mc.player);
    }

    public static void moveItem(int fromIdx, int toIdx) {
        int containerSlot = fromIdx;
        if (fromIdx < 9) containerSlot += 36;
        else if (fromIdx == 40) containerSlot = 45;

        if (fromIdx < 9) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, toIdx, fromIdx, SlotActionType.SWAP, mc.player);
        } else {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, toIdx, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    public static void dropHand() {
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty())
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, ScreenHandler.EMPTY_SPACE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
    }
}
