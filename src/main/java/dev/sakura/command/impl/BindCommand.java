package dev.sakura.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sakura.Sakura;
import dev.sakura.command.Command;
import dev.sakura.command.ModuleArgumentType;
import dev.sakura.module.Module;
import dev.sakura.utils.client.ChatUtils;
import dev.sakura.utils.client.KeyUtils;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import org.lwjgl.glfw.GLFW;

public class BindCommand extends Command {
    public BindCommand() {
        super("Bind", "Keybinds a module", literal("bind", "b"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.module())
                        .then(argument("key", StringArgumentType.string())
                                .executes(c -> {
                                    Module module = ModuleArgumentType.getModule(c, "module");
                                    String keyName = StringArgumentType.getString(c, "key");

                                    if (keyName.equalsIgnoreCase("none")) {
                                        module.setKey(InputUtil.UNKNOWN_KEY.getCode());
                                        ChatUtils.addChatMessage("Unbound " + module.getName() + ".");
                                        Sakura.CONFIG.saveDefaultConfig();
                                        return 1;
                                    }

                                    InputUtil.Key key = KeyUtils.getKeyFromName(keyName);
                                    if (key == InputUtil.UNKNOWN_KEY || key.getCode() == GLFW.GLFW_KEY_UNKNOWN) {
                                        ChatUtils.addChatMessage("Invalid key: " + keyName);
                                        return 0;
                                    }

                                    module.setKey(key.getCode());
                                    ChatUtils.addChatMessage("Bound " + module.getName() + " to " + keyName.toUpperCase() + ".");
                                    Sakura.CONFIG.saveDefaultConfig();
                                    return 1;
                                }))
                        .executes(c -> {
                            ChatUtils.addChatMessage("Usage: .bind <module> <key>");
                            return 1;
                        }))
                .executes(c -> {
                    ChatUtils.addChatMessage("Usage: .bind <module> <key>");
                    return 1;
                });
    }
}
