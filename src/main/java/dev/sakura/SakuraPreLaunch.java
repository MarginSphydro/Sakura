package dev.sakura;

import dev.sakura.manager.impl.SoundManager;
import net.fabricmc.api.ClientModInitializer;

public class SakuraPreLaunch implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SoundManager.init();
    }
}
