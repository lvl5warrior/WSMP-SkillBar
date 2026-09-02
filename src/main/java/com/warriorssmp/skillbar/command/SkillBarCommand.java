package com.warriorssmp.skillbar.command;

import com.warriorssmp.skillbar.SkillBarPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SkillBarCommand implements CommandExecutor {

    private final SkillBarPlugin plugin;

    public SkillBarCommand(SkillBarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        boolean currentlyOn = plugin.preferences().isEnabled(player.getUniqueId());
        boolean turnOn = !currentlyOn;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on")) turnOn = true;
            else if (args[0].equalsIgnoreCase("off")) turnOn = false;
            else {
                player.sendMessage("§eUsage: /skillbar [on|off]");
                return true;
            }
        }

        plugin.preferences().setEnabled(player.getUniqueId(), turnOn);
        if (turnOn) {
            plugin.bossBarManager().showFor(player);
            player.sendMessage("§aSkill bar enabled.");
        } else {
            plugin.bossBarManager().hideFor(player);
            player.sendMessage("§7Skill bar disabled.");
        }
        return true;
    }
}
