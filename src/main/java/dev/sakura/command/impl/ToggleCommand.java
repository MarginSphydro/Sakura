package dev.sakura.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sakura.command.Command;
import dev.sakura.command.ModuleArgumentType;
import dev.sakura.module.Module;
import dev.sakura.utils.client.ChatUtils;
import net.minecraft.command.CommandSource;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("Toggle", "Enables/Disables a module", literal("toggle", "t"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", ModuleArgumentType.module())
                        .executes(c -> {
                            Module module = ModuleArgumentType.getModule(c, "module");
                            module.toggle();
                            ChatUtils.addChatMessage(module.getName() + " is now " +
                                    (module.isEnabled() ? "§aenabled" : "§cdisabled") + "§f.");
                            return 1;
                        }))
                .executes(c -> {
                    ChatUtils.addChatMessage("Usage: .toggle <module>");
                    return 1;
                });
    }
}
