package dev.sakura.mixin;

import dev.sakura.nanovg.NanoVGRenderer;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinMain {
    @Unique
    private static boolean nanovgInitialized = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!nanovgInitialized) {
            NanoVGRenderer.INSTANCE.initNanoVG();
            nanovgInitialized = true;
        }
    }
}
