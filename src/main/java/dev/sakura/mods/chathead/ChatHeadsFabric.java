package dev.sakura.mods.chat_heads.fabric;


import net.fabricmc.api.ClientModInitializer;

public class ChatHeadsFabric implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ChatHeads.init();
	}
}
