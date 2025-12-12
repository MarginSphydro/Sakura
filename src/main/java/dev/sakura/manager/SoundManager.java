package dev.sakura.manager;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

import static dev.sakura.Sakura.mc;

public class SoundManager {
    private static final Set<String> REGISTERED_SOUND_FILES = new HashSet<>();

    public static final SoundEvent ENABLE = registerSound("enable");
    public static final SoundEvent DISABLE = registerSound("disable");
    public static final SoundEvent JELLO_ENABLE = registerSound("activate");
    public static final SoundEvent JELLO_DISABLE = registerSound("deactivate");

    private static SoundEvent registerSound(String name) {
        registerSoundFile(name + ".ogg");
        Identifier id = Identifier.of("sakura", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static void registerSoundFile(String soundFile) {
        REGISTERED_SOUND_FILES.add(soundFile);
    }

    public void playSound(SoundEvent sound) {
        playSound(sound, 1.2f, 0.75f);
    }

    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (mc.player != null) {
            mc.executeSync(() -> mc.player.playSound(sound, volume, pitch));
        }
    }

    public static Set<String> getRegisteredSoundFiles() {
        return new HashSet<>(REGISTERED_SOUND_FILES);
    }
}