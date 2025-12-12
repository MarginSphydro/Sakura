package dev.sakura.mixin.input;

import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @Author：Gu-Yuemang
 * @Date：12/7/2025 1:55 PM
 */
@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {
    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onHandleInputEvents(CallbackInfo info) {
    }
}
