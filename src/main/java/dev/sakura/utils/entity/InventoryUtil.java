package dev.sakura.utils.entity;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static int findBlock(Block block) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int findBlockInventorySlot(Block block) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static void switchToSlot(int slot) {
        if (mc.player == null || slot < 0 || slot > 8) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static void inventorySwap(int from, int to) {
        if (mc.player == null || mc.interactionManager == null) return;
        int syncId = mc.player.currentScreenHandler.syncId;

        int fromSlot = from < 9 ? from + 36 : from;
        int toSlot = to < 9 ? to + 36 : to;

        mc.interactionManager.clickSlot(syncId, fromSlot, toSlot, SlotActionType.SWAP, mc.player);
    }
}