package com.warriorssmp.simplesell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellService {

    private final SimpleSellPlugin plugin;
    private final LevelManager levelManager;
    private final BuyBackManager buyBackManager;

    public SellService(SimpleSellPlugin plugin, LevelManager levelManager, BuyBackManager buyBackManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.buyBackManager = buyBackManager;
    }

    public void sellHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You aren't holding anything to sell.");
            return;
        }

        String matName = item.getType().name();
        if (plugin.isBlacklisted(matName)) {
            player.sendMessage(ChatColor.RED + "That item can't be sold.");
            return;
        }

        int amount = item.getAmount();
        ItemStack soldCopy = item.clone();
        double total = calculateSaleTotal(player, matName, amount);

        player.getInventory().setItemInMainHand(null);
        payPlayer(player, total);
        recordSale(player, matName, amount, soldCopy, total);
        showExpPopup(player, amount);

        int level = levelManager.getLevel(player, matName);
        player.sendMessage(ChatColor.GREEN + "Sold " + amount + "x " + formatName(matName)
                + ChatColor.GREEN + " for " + ChatColor.GOLD + "$" + String.format("%.2f", total)
                + ChatColor.GRAY + " (Lv." + level + ")");
    }

    public void sellAll(Player player) {
        Inventory inv = player.getInventory();
        double total = 0.0;
        int itemsSold = 0;

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) continue;

            String matName = item.getType().name();
            if (plugin.isBlacklisted(matName)) continue;

            int amount = item.getAmount();
            ItemStack soldCopy = item.clone();
            double lineTotal = calculateSaleTotal(player, matName, amount);
            total += lineTotal;
            itemsSold += amount;
            contents[i] = null;

            recordSale(player, matName, amount, soldCopy, lineTotal);
        }

        if (itemsSold == 0) {
            player.sendMessage(ChatColor.RED + "You don't have anything sellable in your inventory.");
            return;
        }

        inv.setStorageContents(contents);
        payPlayer(player, total);
        showExpPopup(player, itemsSold);

        player.sendMessage(ChatColor.GREEN + "Sold " + itemsSold + " items for "
                + ChatColor.GOLD + "$" + String.format("%.2f", total));
    }

    /**
     * Sells a single item stack that's already been removed from the player's inventory
     * (used by the drag-and-drop sell bin). Returns the total paid for it.
     */
    public double sellFromBin(Player player, ItemStack item) {
        String matName = item.getType().name();
        int amount = item.getAmount();
        ItemStack soldCopy = item.clone();
        double total = calculateSaleTotal(player, matName, amount);

        payPlayer(player, total);
        recordSale(player, matName, amount, soldCopy, total);
        showExpPopup(player, amount);
        return total;
    }

    public void checkPrice(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You aren't holding anything.");
            return;
        }
        String matName = item.getType().name();
        if (plugin.isBlacklisted(matName)) {
            player.sendMessage(ChatColor.RED + formatName(matName) + " cannot be sold.");
            return;
        }
        double unitPrice = getEffectivePrice(player, matName);
        int level = levelManager.getLevel(player, matName);
        player.sendMessage(ChatColor.YELLOW + formatName(matName) + ChatColor.YELLOW + " sells for "
                + ChatColor.GOLD + "$" + String.format("%.2f", unitPrice) + ChatColor.YELLOW + " each ("
                + item.getAmount() + "x = $" + String.format("%.2f", unitPrice * item.getAmount()) + ")"
                + ChatColor.GRAY + " [Lv." + level + "]");
    }

    /** The price for a single unit, including the player's level bonus for that item. */
    public double getEffectivePrice(Player player, String materialName) {
        double basePrice = plugin.getPrice(materialName);
        int level = levelManager.getLevel(player, materialName);
        return basePrice * LevelManager.getMultiplier(level);
    }

    private double calculateSaleTotal(Player player, String materialName, int amount) {
        return getEffectivePrice(player, materialName) * amount;
    }

    private void recordSale(Player player, String materialName, int amount, ItemStack soldCopy, double total) {
        levelManager.addSold(player, materialName, amount);
        buyBackManager.recordSale(player, soldCopy, total);
    }

    private void showExpPopup(Player player, int amount) {
        player.sendActionBar(Component.text("+" + amount + " EXP", NamedTextColor.GREEN));
    }

    private void payPlayer(Player player, double amount) {
        Economy econ = plugin.getEconomy();
        econ.depositPlayer(player, amount);
    }

    public String formatName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
