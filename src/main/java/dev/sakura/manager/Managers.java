package dev.sakura.manager;

import dev.sakura.manager.impl.AccountManager;
import dev.sakura.manager.impl.RotationManager;

public class Managers {
    private static boolean initialized;

    public static AccountManager ACCOUNT;

    public static void init() {
        if (initialized) return;

        ACCOUNT = new AccountManager();
        RotationManager.INSTANCE.toString();

        initialized = true;
    }
}
