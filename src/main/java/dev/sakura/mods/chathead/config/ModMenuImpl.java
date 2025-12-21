package dev.sakura.mods.chat_heads.fabric.config;



public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Mod Menu 2.0.14+ handles missing classes by disabling the config screen (and printing a warning)
        // we put a custom screen instead, to not confuse users
        return parent -> {
            if (Compat.isClothConfigLoaded()) {
                return AutoConfigClient.getConfigScreen(ChatHeadsConfigData.class, parent).get();
            } else {
                return new MissingClothConfigScreen(parent);
            }
        };
    }
}