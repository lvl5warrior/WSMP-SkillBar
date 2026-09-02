package com.warriorssmp.simplesell;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BuyBackGUI implements Listener {

    private static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&4&lWSMP &8» &7Buy Back &8(&e30 min&8)");

    private final SimpleSellPlugin plugin;
    private final BuyBackManager buyBackManager;

    public BuyBackGUI(SimpleSellPlugin plugin, BuyBackManager buyBackManager) {
        this.plugin = plugin;
        this.buyBackManager = buyBackManager;
    }

    public void open(Player player) {
        List<BuyBackManager.SoldEntry> entries = buyBackManager.getEntries(player);
        Inventory gui = Bukkit.createInventory(null, 36, TITLE);

        if (entries.isEmpty()) {
            gui.setItem(13, GuiUtil.namedItem(Material.BARRIER, "&cNothing to buy back",
                    "&7Items you sell appear here", "&7for 30 minutes."));
        } else {
            for (int i = 0; i < entries.size() && i < 27; i++) {
                BuyBackManager.SoldEntry entry = entries.get(i);
                ItemStack display = entry.item.clone();
                ItemMeta meta = display.getItemMeta();
                List<String> lore = new ArrayList<>();
                if (meta.hasLore()) lore.addAll(meta.getLore());
                lore.add("");
                long secondsLeft = entry.millisRemaining() / 1000;
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7Buy back for &a$" + String.format("%.2f", entry.pricePaid)));
                lore.add(ChatColor.translateAlternateColorCodes('&',
                        "&7Expires in &e" + formatTime(secondsLeft)));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&e&lClick to buy back!"));
                meta.setLore(lore);
                GuiUtil.hideExtras(meta);
                display.setItemMeta(meta);
                gui.setItem(i, display);
            }
        }

        // Back button
        gui.setItem(31, GuiUtil.namedItem(Material.ARROW, "&7Back to Shop"));

        player.openInventory(gui);
    }

    private String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + "m " + seconds + "s";
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 31) {
            plugin.getShopGUI().open(player);
            return;
        }

        List<BuyBackManager.SoldEntry> entries = buyBackManager.getEntries(player);
        if (slot < 0 || slot >= entries.size()) return;

        BuyBackManager.SoldEntry entry = entries.get(slot);
        Economy econ = plugin.getEconomy();

        if (econ.getBalance(player) < entry.pricePaid) {
            player.sendMessage(ChatColor.RED + "You don't have enough money to buy that back.");
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Your inventory is full.");
            return;
        }

        econ.withdrawPlayer(player, entry.pricePaid);
        player.getInventory().addItem(entry.item.clone());
        buyBackManager.removeEntry(player, entry);

        player.sendMessage(ChatColor.GREEN + "Bought back for " + ChatColor.GOLD + "$" + String.format("%.2f", entry.pricePaid));
        open(player); // refresh the screen
    }
}
