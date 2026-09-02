package com.warriorssmp.skillbar.command;

import com.warriorssmp.skillbar.SkillBarPlugin;
import com.warriorssmp.skillbar.SkillLookup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

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

        List<SkillLookup.SkillLevel> levels = plugin.skillLookup().getAllLevels(player.getUniqueId());
        if (levels.isEmpty()) {
            player.sendMessage("§cNo WSMP skill plugins are installed on this server.");
            return true;
        }

        player.sendMessage("§6§l--- Your Skill Levels ---");
        for (SkillLookup.SkillLevel level : levels) {
            int barLength = 20;
            int filled = (int) Math.round(level.progressFraction() * barLength);
            String bar = "§a" + "█".repeat(filled) + "§7" + "░".repeat(barLength - filled);
            String progress = level.maxLevel()
                    ? "§7(MAX LEVEL)"
                    : "§7(" + level.xpIntoLevel() + "/" + level.xpForNextLevel() + " XP)";
            player.sendMessage("§f" + level.displayName() + " §7— Level §f" + level.level() + " " + progress);
            player.sendMessage("  " + bar);
        }
        return true;
    }
}
