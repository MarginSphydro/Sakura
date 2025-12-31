package dev.sakura.client.manager.impl;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

import static dev.sakura.client.Sakura.mc;

public class SoundManager {
    private static final Set<String> REGISTERED_SOUND_FILES = new HashSet<>();
    private static boolean initialized = false;

    public static SoundEvent ENABLE;
    public static SoundEvent DISABLE;
    public static SoundEvent JELLO_ENABLE;
    public static SoundEvent JELLO_DISABLE;

    public static void init() {
        if (initialized) return;

        ENABLE = registerSound("enable");
        DISABLE = registerSound("disable");
        JELLO_ENABLE = registerSound("activate");
        JELLO_DISABLE = registerSound("deactivate");

        initialized = true;
    }

    private static SoundEvent registerSound(String name) {
        registerSoundFile(name + ".ogg");
        Identifier id = Identifier.of("sakura", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static void registerSoundFile(String soundFile) {
        REGISTERED_SOUND_FILES.add(soundFile);
    }

    public static void playSound(SoundEvent sound) {
        playSound(sound, 1.2f, 0.75f);
    }

    public static void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == null || mc.player == null) return;
        mc.executeSync(() -> mc.player.playSound(sound, volume, pitch));
    }

    public static Set<String> getRegisteredSoundFiles() {
        return new HashSet<>(REGISTERED_SOUND_FILES);
    }
}
