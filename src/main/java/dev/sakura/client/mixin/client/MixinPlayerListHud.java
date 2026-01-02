package dev.sakura.client.mixin.client;

import dev.sakura.client.Sakura;
import dev.sakura.client.module.impl.client.Chat;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHud {
    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(PlayerListEntry playerListEntry, CallbackInfoReturnable<Text> info) {
        Chat chat = Sakura.MODULES.getModule(Chat.class);

        if (chat.isEnabled() && chat.enableTab.get()) info.setReturnValue(chat.getPlayerName(playerListEntry));
    }
}
