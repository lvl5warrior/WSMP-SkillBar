package com.warriorssmp.skillbar.command;

import com.warriorssmp.skillbar.SkillBarPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SkillsCommand implements CommandExecutor {

    private final SkillBarPlugin plugin;

    public SkillsCommand(SkillBarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        plugin.menuManager().openSkillsMenu(player);
        return true;
    }
}
