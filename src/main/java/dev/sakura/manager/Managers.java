package dev.sakura.manager;

import dev.sakura.command.CommandManager;
import dev.sakura.config.ConfigManager;
import dev.sakura.manager.impl.AccountManager;
import dev.sakura.module.ModuleManager;

public class Managers {
    private static boolean initialized;

    public static AccountManager ACCOUNT;
    public static ModuleManager MODULE;
    public static ConfigManager CONFIG;
    public static CommandManager COMMAND;

    public static void init() {
        if (initialized) return;

        ACCOUNT = new AccountManager();
        MODULE = new ModuleManager();
        CONFIG = new ConfigManager();
        COMMAND = new CommandManager();

        initialized = true;
    }
}
