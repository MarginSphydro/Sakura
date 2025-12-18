package dev.sakura.manager;

import dev.sakura.manager.impl.AccountManager;

public class Managers {
    private static boolean initialized;

    public static AccountManager ACCOUNT;

    public static void init() {
        if (initialized) return;

        ACCOUNT = new AccountManager();

        initialized = true;
    }
}
