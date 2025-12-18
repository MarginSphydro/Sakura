package dev.sakura.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sakura.command.Command;
import dev.sakura.manager.Managers;
import dev.sakura.utils.client.ChatUtils;
import net.minecraft.command.CommandSource;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("Help", "Shows all available commands", literal("help", "h", "?"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> {
            ChatUtils.addChatMessage("§7--- §fAvailable Commands §7---");
            for (Command command : Managers.COMMAND.getCommands()) {
                ChatUtils.addChatMessage("§7." + command.getName().toLowerCase() + " §f- " + command.getDescription());
            }
            return 1;
        });
    }
}
