package dev.sakura.module.impl.render;

import dev.sakura.events.client.GameJoinEvent;
import dev.sakura.events.client.TickEvent;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import dev.sakura.values.impl.EnumValue;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {

    public enum Mode {
        Gamma, Potion
    }

    public Fullbright() {
        super("Fullbright", Category.Render);
    }

    public final EnumValue<Mode> mode = new EnumValue<>("Mode", Mode.Gamma);

    @Override
    public void onEnable() {
        if (mc.player != null && mc.world != null
                && mode.is(Mode.Potion)) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0)); // INFINITE
        }
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.world != null
                && mode.is(Mode.Potion)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    @EventHandler
    public void onGameJoin(GameJoinEvent event) {
        onDisable();
        onEnable();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (mode.is(Mode.Potion)) {
            if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0));
            }
        } else {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }

    public boolean isGamma() {
        return isEnabled() && mode.is(Mode.Gamma);
    }
}
