package dev.sakura.module.impl.combat;

import dev.sakura.events.client.TickEvent;
import dev.sakura.gui.clickgui.ClickGuiScreen;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.utils.combat.CombatUtil;
import dev.sakura.utils.entity.InventoryUtil;
import dev.sakura.utils.time.TimerUtil;
import dev.sakura.values.impl.BoolValue;
import dev.sakura.values.impl.EnumValue;
import dev.sakura.values.impl.NumberValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class AutoPot extends Module {
    public static AutoPot INSTANCE;

    private final EnumValue<Page> page = new EnumValue<>("Page", Page.General);

    private final NumberValue<Double> delay = new NumberValue<>("Delay", 5.0, 0.0, 10.0, 0.5,
            () -> page.is(Page.General));
    private final BoolValue usingPause = new BoolValue("UsingPause", true,
            () -> page.is(Page.General));
    private final BoolValue inventory = new BoolValue("InventorySwap", true,
            () -> page.is(Page.General));
    private final BoolValue onlyGround = new BoolValue("OnlyGround", true,
            () -> page.is(Page.General));
    private final BoolValue rangeCheck = new BoolValue("RangeCheck", false,
            () -> page.is(Page.General));
    private final NumberValue<Double> range = new NumberValue<>("Range", 15.0, 0.0, 50.0, 0.5,
            () -> page.is(Page.General) && rangeCheck.get());
    private final NumberValue<Integer> pitch = new NumberValue<>("Pitch", 88, 75, 90, 1,
            () -> page.is(Page.General));
    private final BoolValue snapBack = new BoolValue("SnapBack", true,
            () -> page.is(Page.General));

    private final BoolValue speed = new BoolValue("Speed", true, () -> page.is(Page.Effects));
    private final BoolValue resistance = new BoolValue("Turtlemaster", true, () -> page.is(Page.Effects));
    private final BoolValue slowFalling = new BoolValue("SlowFalling", true, () -> page.is(Page.Effects));
    private final BoolValue strength = new BoolValue("Strength", true, () -> page.is(Page.Effects));
    private final BoolValue fireResistance = new BoolValue("FireResistance", false, () -> page.is(Page.Effects));
    private final BoolValue regeneration = new BoolValue("Regeneration", false, () -> page.is(Page.Effects));

    private final TimerUtil delayTimer = new TimerUtil();

    private boolean throwing = false;
    private float lastYaw, lastPitch;

    public AutoPot() {
        super("AutoPot", Category.Combat);
        INSTANCE = this;
    }

    public enum Page {
        General,
        Effects
    }

    @Override
    protected void onEnable() {
        throwing = false;
        delayTimer.reset();
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Override
    protected void onDisable() {
        throwing = false;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        setSuffix(page.get().name());

        if (rangeCheck.get()) {
            PlayerEntity enemy = CombatUtil.getClosestEnemy(range.get());
            if (enemy == null) return;
        }

        if (onlyGround.get()) {
            if (!mc.player.isOnGround()) return;
            if (mc.world.isAir(BlockPos.ofFloored(mc.player.getPos().add(0, -1, 0)))) return;
        }

        if (resistance.get() && needsEffect(StatusEffects.RESISTANCE, 2)) {
            tryThrowPotion(StatusEffects.RESISTANCE.value());
            return;
        }

        if (speed.get() && needsEffect(StatusEffects.SPEED, 0)) {
            tryThrowPotion(StatusEffects.SPEED.value());
            return;
        }

        if (slowFalling.get() && needsEffect(StatusEffects.SLOW_FALLING, 0)) {
            tryThrowPotion(StatusEffects.SLOW_FALLING.value());
            return;
        }

        if (strength.get() && needsEffect(StatusEffects.STRENGTH, 0)) {
            tryThrowPotion(StatusEffects.STRENGTH.value());
            return;
        }

        if (fireResistance.get() && needsEffect(StatusEffects.FIRE_RESISTANCE, 0)) {
            tryThrowPotion(StatusEffects.FIRE_RESISTANCE.value());
            return;
        }

        if (regeneration.get() && needsEffect(StatusEffects.REGENERATION, 0)) {
            tryThrowPotion(StatusEffects.REGENERATION.value());
        }
    }

    private boolean needsEffect(RegistryEntry<StatusEffect> effect, int minAmplifier) {
        if (!mc.player.hasStatusEffect(effect)) {
            return true;
        }
        if (minAmplifier > 0) {
            StatusEffectInstance instance = mc.player.getStatusEffect(effect);
            return instance != null && instance.getAmplifier() < minAmplifier;
        }
        return false;
    }

    private void tryThrowPotion(StatusEffect targetEffect) {
        throwing = checkThrow(targetEffect);
        if (throwing && delayTimer.hasReached(delay.get() * 1000)) {
            throwPotion(targetEffect);
        }
    }

    public void throwPotion(StatusEffect targetEffect) {
        if (mc.player == null || mc.interactionManager == null) return;

        int oldSlot = mc.player.getInventory().selectedSlot;
        int newSlot;

        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();

        if (inventory.get() && (newSlot = InventoryUtil.findPotionInventorySlot(targetEffect)) != -1) {
            sendLookPacket(lastYaw, pitch.get().floatValue());
            InventoryUtil.inventorySwap(newSlot, oldSlot);
            useItem();
            InventoryUtil.inventorySwap(newSlot, oldSlot);
            InventoryUtil.syncInventory();
            if (snapBack.get()) {
                sendLookPacket(lastYaw, lastPitch);
            }
            delayTimer.reset();
        } else if ((newSlot = InventoryUtil.findPotion(targetEffect)) != -1) {
            sendLookPacket(lastYaw, pitch.get().floatValue());
            InventoryUtil.switchToSlot(newSlot);
            useItem();
            InventoryUtil.switchToSlot(oldSlot);
            if (snapBack.get()) {
                sendLookPacket(lastYaw, lastPitch);
            }
            delayTimer.reset();
        }
    }

    private void sendLookPacket(float yaw, float pitch) {
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision
        ));
    }

    private void useItem() {
        if (mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
    }

    public boolean checkThrow(StatusEffect targetEffect) {
        if (isDisabled()) return false;

        if (mc.currentScreen != null
                && !(mc.currentScreen instanceof ChatScreen)
                && !(mc.currentScreen instanceof InventoryScreen)
                && !(mc.currentScreen instanceof ClickGuiScreen)
                && !(mc.currentScreen instanceof GameMenuScreen)) {
            return false;
        }

        if (usingPause.get() && mc.player.isUsingItem()) {
            return false;
        }

        boolean hasInHotbar = InventoryUtil.findPotion(targetEffect) != -1;
        boolean hasInInventory = inventory.get() && InventoryUtil.findPotionInventorySlot(targetEffect) != -1;

        return hasInHotbar || hasInInventory;
    }

    public boolean isThrowing() {
        return throwing;
    }
}