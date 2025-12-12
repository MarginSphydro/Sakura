package dev.sakura;

import dev.sakura.command.CommandManager;
import dev.sakura.config.ConfigManager;
import dev.sakura.gui.dropdown.DropDownClickGui;
import dev.sakura.gui.hud.HudEditorScreen;
import dev.sakura.manager.Managers;
import dev.sakura.module.ModuleManager;
import dev.sakura.shaders.Shader2DUtils;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;

public class Sakura implements ClientModInitializer {
    public static final String MOD_NAME = "Sakura";
    public static final String MOD_VER = BuildConfig.VERSION + "-" + BuildConfig.BUILD_IDENTIFIER;

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final IEventBus EVENT_BUS = new EventBus();

    public static MinecraftClient mc;

    public static ModuleManager MODULE;
    public static DropDownClickGui CLICKGUI;
    public static HudEditorScreen HUDEDITOR;
    public static ConfigManager CONFIG;
    public static CommandManager COMMAND;

    @Override
    public void onInitializeClient() {
        LOGGER.info("正在开始初始化!");

        mc = MinecraftClient.getInstance();

        // 事件巴士(doge 初始化
        EVENT_BUS.registerLambdaFactory(Sakura.class.getPackageName(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        // 初始化Managers
        Managers.init();

        // 初始化Modules
        MODULE = new ModuleManager();
        MODULE.Init();

        // 初始化ClickGui
        CLICKGUI = new DropDownClickGui();

        // 初始化HudEditor
        HUDEDITOR = new HudEditorScreen();

        // 初始化Config
        CONFIG = new ConfigManager();
        CONFIG.loadDefaultConfig();

        // 初始化Command
        COMMAND = new CommandManager();

        // 初始化Shaders
        Shader2DUtils.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("正在保存配置并且关闭游戏!");
            CONFIG.saveDefaultConfig();
        }));

        LOGGER.info("初始化完成!");
    }
}
