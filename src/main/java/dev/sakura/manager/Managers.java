package dev.sakura.manager;

public class Managers {
    public static SoundManager SOUND;

    private static boolean initialized;

    public static void init() {
        if (initialized) return;

        SOUND = new SoundManager();

        initialized = true;
    }
}
