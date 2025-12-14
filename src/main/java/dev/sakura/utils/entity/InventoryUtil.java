package dev.sakura.utils.entity;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static int lastSlot = -1;
    private static int lastSelect = -1;

    public static void inventorySwap(int slot, int selectedSlot) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (slot == lastSlot) {
            switchToSlot(lastSelect);
            lastSlot = -1;
            lastSelect = -1;
            return;
        }
        if (slot - 36 == selectedSlot) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, selectedSlot, SlotActionType.SWAP, mc.player);
    }

    public static void switchToSlot(int slot) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    public static ItemStack getStackInSlot(int i) {
        if (mc.player == null) return ItemStack.EMPTY;
        return mc.player.getInventory().getStack(i);
    }

    public static int findItem(Item input) {
        for (int i = 0; i < 9; ++i) {
            Item item = getStackInSlot(i).getItem();
            if (Item.getRawId(item) != Item.getRawId(input)) continue;
            return i;
        }
        return -1;
    }

    public static int findBlock(Block blockIn) {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack == ItemStack.EMPTY || !(stack.getItem() instanceof BlockItem) || ((BlockItem) stack.getItem()).getBlock() != blockIn)
                continue;
            return i;
        }
        return -1;
    }

    public static int findBlock() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem) {
                Block block = ((BlockItem) stack.getItem()).getBlock();
                if (block != Blocks.COBWEB) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static int findBlockInventorySlot(Block block) {
        return findItemInventorySlot(block.asItem());
    }

    public static int findItemInventorySlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 45; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i < 9 ? i + 36 : i;
        }
        return -1;
    }

    public static Map<Integer, ItemStack> getInventoryAndHotbarSlots() {
        HashMap<Integer, ItemStack> fullInventorySlots = new HashMap<>();
        if (mc.player == null) return fullInventorySlots;
        for (int current = 0; current <= 44; ++current) {
            fullInventorySlots.put(current, mc.player.getInventory().getStack(current));
        }
        return fullInventorySlots;
    }

    public static int getItemCount(Item item) {
        int count = 0;
        for (Map.Entry<Integer, ItemStack> entry : getInventoryAndHotbarSlots().entrySet()) {
            if (entry.getValue().getItem() != item) continue;
            count = count + entry.getValue().getCount();
        }
        return count;
    }

    public static int getPotionCount(StatusEffect targetEffect) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 45; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isOf(Items.SPLASH_POTION)) continue;
            List<StatusEffectInstance> effects = getPotionEffects(itemStack);
            for (StatusEffectInstance effect : effects) {
                if (effect.getEffectType().value() == targetEffect) {
                    count += itemStack.getCount();
                    break;
                }
            }
        }
        return count;
    }

    public static int findPotionInventorySlot(StatusEffect targetEffect) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 45; ++i) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isOf(Items.SPLASH_POTION)) continue;
            List<StatusEffectInstance> effects = getPotionEffects(itemStack);
            for (StatusEffectInstance effect : effects) {
                if (effect.getEffectType().value() == targetEffect) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    public static int findPotion(StatusEffect targetEffect) {
        for (int i = 0; i < 9; ++i) {
            ItemStack itemStack = getStackInSlot(i);
            if (!itemStack.isOf(Items.SPLASH_POTION)) continue;
            List<StatusEffectInstance> effects = getPotionEffects(itemStack);
            for (StatusEffectInstance effect : effects) {
                if (effect.getEffectType().value() == targetEffect) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<StatusEffectInstance> getPotionEffects(ItemStack itemStack) {
        var potionContents = itemStack.get(DataComponentTypes.POTION_CONTENTS);
        if (potionContents != null) {
            Iterable<StatusEffectInstance> effects = potionContents.getEffects();
            List<StatusEffectInstance> list = new ArrayList<>();
            for (StatusEffectInstance effect : effects) {
                list.add(effect);
            }
            return list;
        }
        return List.of();
    }

    public static void syncInventory() {
        if (mc.player == null) return;
        mc.player.getInventory().updateItems();
    }
}