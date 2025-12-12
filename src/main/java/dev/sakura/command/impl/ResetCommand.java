package dev.sakura.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.sakura.Sakura;
import dev.sakura.command.Command;
import dev.sakura.command.ModuleArgumentType;
import dev.sakura.module.Category;
import dev.sakura.module.HudModule;
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
                    ChatUtils.addChatMessage("§aAll configurations have been reset.");
                    return 1;
                }))
                .then(literal("module")
                        .then(argument("module", ModuleArgumentType.module())
                                .executes(c -> {
                                    Module module = ModuleArgumentType.getModule(c, "module");
                                    resetModule(module);
                                    ChatUtils.addChatMessage("§aModule " + module.getName() + " has been reset.");
                                    return 1;
                                })))
                .then(literal("hud")
                        .then(literal("all").executes(c -> {
                            resetAllHud();
                            ChatUtils.addChatMessage("§aAll HUD positions have been reset.");
                            return 1;
                        }))
                        .then(literal("category")
                                .then(argument("category", StringArgumentType.word())
                                        .suggests((c, b) -> {
                                            for (Category cat : Category.values()) {
                                                b.suggest(cat.name().toLowerCase());
                                            }
                                            return b.buildFuture();
                                        })
                                        .executes(c -> {
                                            String catName = StringArgumentType.getString(c, "category");
                                            Category category = null;
                                            for (Category cat : Category.values()) {
                                                if (cat.name().equalsIgnoreCase(catName)) {
                                                    category = cat;
                                                    break;
                                                }
                                            }
                                            if (category == null) {
                                                ChatUtils.addChatMessage("§cCategory not found: " + catName);
                                                return 0;
                                            }
                                            resetHudCategory(category);
                                            ChatUtils.addChatMessage("§aHUD positions for category " + category.name() + " have been reset.");
                                            return 1;
                                        }))))
                .executes(c -> {
                    ChatUtils.addChatMessage("§eUsage:");
                    ChatUtils.addChatMessage("  §7" + Sakura.COMMAND.getPrefix() + "reset all §f- Reset all configurations");
                    ChatUtils.addChatMessage("  §7" + Sakura.COMMAND.getPrefix() + "reset module <name> §f- Reset a specific module");
                    ChatUtils.addChatMessage("  §7" + Sakura.COMMAND.getPrefix() + "reset hud all §f- Reset all HUD positions");
                    ChatUtils.addChatMessage("  §7" + Sakura.COMMAND.getPrefix() + "reset hud category <name> §f- Reset HUD positions by category");
                    return 1;
                });
    }

    private void resetAll() {
        for (Module module : Sakura.MODULE.getAllModules()) {
            resetModule(module);
        }
    }

    private void resetModule(Module module) {
        module.setState(false);
        if (!module.getName().equalsIgnoreCase("ClickGui")) {
            module.setKey(-1);
        }
        if (module instanceof HudModule hud) {
            resetHudPosition(hud);
        }
    }

    private void resetAllHud() {
        for (HudModule hud : Sakura.MODULE.getAllHudModules()) {
            resetHudPosition(hud);
        }
    }

    private void resetHudCategory(Category category) {
        for (Module module : Sakura.MODULE.getModsByCategory(category)) {
            if (module instanceof HudModule hud) {
                resetHudPosition(hud);
            }
        }
    }

    private void resetHudPosition(HudModule hud) {
        hud.setX(10);
        hud.setY(10);
    }
}
