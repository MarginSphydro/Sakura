package dev.sakura.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sakura.Sakura;
import dev.sakura.command.Command;
import dev.sakura.command.ModuleArgumentType;
import dev.sakura.gui.dropdown.panel.CategoryPanel;
import dev.sakura.module.Module;
import dev.sakura.utils.client.ChatUtils;
import net.minecraft.command.CommandSource;

public class ResetCommand extends Command {
    public ResetCommand() {
        super("Reset", "Resets module settings or HUD positions", literal("reset"));
    }

    @Override
    public void buildCommand(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(literal("all").executes(c -> {
                    resetAll();
                    ChatUtils.addChatMessage("§a已重置所有配置为默认值。");
                    return 1;
                }))
                .then(literal("module")
                        .then(argument("module", ModuleArgumentType.module())
                                .executes(c -> {
                                    Module module = ModuleArgumentType.getModule(c, "module");
                                    module.reset();
                                    ChatUtils.addChatMessage("§a已重置模块 " + module.getName() + " 为默认值。");
                                    return 1;
                                })))
                .executes(c -> {
                    ChatUtils.addChatMessage("§e用法:");
                    ChatUtils.addChatMessage("  §7" + Sakura.COMMAND.getPrefix() + "reset all §f- 重置所有配置");
                    ChatUtils.addChatMessage("  §7" + Sakura.COMMAND.getPrefix() + "reset module <名称> §f- 重置指定模块");
                    return 1;
                });
    }

    private void resetAll() {
        for (Module module : Sakura.MODULE.getAllModules()) {
            module.reset();
        }
        resetClickGuiPanels();
    }

    private void resetClickGuiPanels() {
        float xOffset = 50;
        for (CategoryPanel panel : Sakura.CLICKGUI.getPanels()) {
            panel.setX(xOffset);
            panel.setY(20);
            panel.setOpened(true);
            xOffset += panel.getWidth() + 20;
        }
        Sakura.CLICKGUI.scroll = 0;
    }
}
