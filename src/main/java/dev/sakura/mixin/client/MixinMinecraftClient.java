package dev.sakura.mixin.client;

import dev.sakura.Sakura;
import dev.sakura.events.client.TickEvent;
import dev.sakura.events.input.HandleInputEvent;
import dev.sakura.shaders.WindowResizeCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    @Final
    private Window window;

    @Inject(method = "run", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        Sakura.init();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPreTick(CallbackInfo info) {
        Sakura.EVENT_BUS.post(new TickEvent.Pre());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPostTick(CallbackInfo info) {
        Sakura.EVENT_BUS.post(new TickEvent.Post());
    }

    @Inject(method = "handleInputEvents", at = @At(value = "HEAD"))
    private void onHandleInputEvents(CallbackInfo info) {
        Sakura.EVENT_BUS.post(new HandleInputEvent());
    }

    @ModifyArg(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setTitle(Ljava/lang/String;)V"))
    private String setTitle(String original) {
        return "桜";
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void captureResize(CallbackInfo ci) {
        WindowResizeCallback.EVENT.invoker().onResized((MinecraftClient) (Object) this, this.window);
    }
}
