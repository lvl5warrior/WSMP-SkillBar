package com.warriorssmp.simplesell;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final SimpleSellPlugin plugin;
    private final SellService sellService;

    public SellCommand(SimpleSellPlugin plugin, SellService sellService) {
        this.plugin = plugin;
        this.sellService = sellService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /sell hand | /sell all | /sell price");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "hand":
                sellService.sellHand(player);
                return true;
            case "all":
                sellService.sellAll(player);
                return true;
            case "price":
                sellService.checkPrice(player);
                return true;
            case "reload":
                if (player.hasPermission("simplesell.reload")) {
                    plugin.reloadSettings();
                    player.sendMessage(ChatColor.GREEN + "SimpleSell config reloaded.");
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
                }
                return true;
            default:
                player.sendMessage(ChatColor.YELLOW + "Usage: /sell hand | /sell all | /sell price");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("hand");
            options.add("all");
            options.add("price");
        }
        return options;
    }
}
