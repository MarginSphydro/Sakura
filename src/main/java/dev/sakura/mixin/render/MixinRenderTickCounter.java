package dev.sakura.mixin.render;

import dev.sakura.Sakura;
import dev.sakura.events.client.TimerEvent;
import dev.sakura.manager.Managers;
import dev.sakura.module.impl.player.TimerModule;
import net.minecraft.client.render.RenderTickCounter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTickCounter.Dynamic.class)
public class MixinRenderTickCounter {
    @Shadow
    private float lastFrameDuration;

    @Inject(at = {@At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderTickCounter$Dynamic;prevTimeMillis:J", opcode = Opcodes.PUTFIELD, ordinal = 0) }, method = {"beginRenderTick(J)I" })
    public void onBeginRenderTick(long long_1, CallbackInfoReturnable<Integer> cir) {
        TimerEvent event = new TimerEvent();
        Sakura.EVENT_BUS.post(event);
        TimerModule timer = Managers.MODULE.getModule(TimerModule.class);
        if (!event.isCancelled()) {
            if (event.isModified()) {
                lastFrameDuration *= event.get();
            } else {
                lastFrameDuration *= timer.getTimerSpeed();
            }
        }
    }
}
