package dev.sakura.mixin.render;

import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(SplashTextResourceSupplier.class)
public class MixinSplashTextResourceSupplier {
    @Unique
    private boolean override = true;
    @Unique
    private static final Random random = new Random();
    @Unique
    private final List<String> meteorSplashes = getMeteorSplashes();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void onApply(CallbackInfoReturnable<SplashTextRenderer> cir) {
        if (override)
            cir.setReturnValue(new SplashTextRenderer(meteorSplashes.get(random.nextInt(meteorSplashes.size()))));
        override = !override;
        cir.cancel();
    }

    @Unique
    private static List<String> getMeteorSplashes() {
        return List.of(
                "爸爸妈妈我出来了",
                "Pig god oh yes oh yes",
                "你怎么这么松啊你",
                "Oh dream im happy oh yes"
        );
    }
}
