package dev.sakura.manager.impl;

import dev.sakura.utils.player.FindItemResult;
import dev.sakura.utils.player.InvUtil;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

import static dev.sakura.Sakura.mc;

public class PlaceManager {
    public static void placeBlock(BlockHitResult blockHitResult, FindItemResult result, boolean swing, boolean swapBack) {
        int slot = result.isOffhand() ? mc.player.getInventory().selectedSlot : result.slot();

        InvUtil.swap(slot, swapBack);

        interact(blockHitResult, result.getHand(), swing);

        if (swapBack) InvUtil.swapBack();
    }

    public static void interact(BlockHitResult blockHitResult, Hand hand, boolean swing) {
        boolean hookSneaking = mc.player.isSneaking();
        mc.player.setSneaking(false);

        ActionResult result = mc.interactionManager.interactBlock(mc.player, hand, blockHitResult);

        if (result.isAccepted()) {
            if (swing) mc.player.swingHand(hand);
            else mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        mc.player.setSneaking(hookSneaking);
    }
}
