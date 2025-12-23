package dev.sakura.manager;

import dev.sakura.Sakura;
import dev.sakura.command.CommandManager;
import dev.sakura.config.ConfigManager;
import dev.sakura.manager.impl.*;
import dev.sakura.module.ModuleManager;

public class Managers {
    private static boolean initialized;

    public static AccountManager ACCOUNT;
    public static ModuleManager MODULE;
    public static ConfigManager CONFIG;
    public static CommandManager COMMAND;
    public static RenderManager RENDER;
    public static ExtrapolationManager EXTRAPOLATION;
    public static ChatAnimationUpdater CHAT_ANIMATION;

    public static void init() {
        if (initialized) return;

        ACCOUNT = new AccountManager();
        MODULE = new ModuleManager();
        CONFIG = new ConfigManager();
        COMMAND = new CommandManager();
        RENDER = new RenderManager();
        EXTRAPOLATION = new ExtrapolationManager();
        CHAT_ANIMATION = new ChatAnimationUpdater();

        // 触发 NotificationManager 的类加载
        NotificationManager.send("Initializing notifications...", 1L);

        // 注册事件监听器
        Sakura.EVENT_BUS.subscribe(CHAT_ANIMATION);

        initialized = true;
    }
}