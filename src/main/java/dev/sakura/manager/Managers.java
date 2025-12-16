package dev.sakura.manager;

import dev.sakura.manager.impl.RotationManager;

public class Managers {


    private static boolean initialized;

    public static void init() {
        if (initialized) return;

        RotationManager.INSTANCE.toString();

        initialized = true;
    }
}
