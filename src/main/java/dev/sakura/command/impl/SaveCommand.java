package dev.sakura.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sakura.command.Command;
import dev.sakura.manager.Managers;
import dev.sakura.utils.client.ChatUtil;
import net.minecraft.command.CommandSource;

public class SaveCommand extends Command {
    public SaveCommand() {
        super("Save", "Saves all configurations", literal("save", "s"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> {
            Managers.CONFIG.saveDefaultConfig();
            ChatUtil.addChatMessage("All configurations saved.");
            return 1;
        });
    }
}
