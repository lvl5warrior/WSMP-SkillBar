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

public class ShopGUI implements Listener {

    private static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&4&lWarriors &6&lSMP &8» &7Shop");

    private final SimpleSellPlugin plugin;
    private final SellService sellService;
    private final LevelManager levelManager;

    private static final int BALANCE_SLOT = 4;
    private static final int SELL_HAND_SLOT = 11;
    private static final int PRICE_CHECK_SLOT = 13;
    private static final int SELL_ALL_SLOT = 15;
    private static final int BROWSE_SLOT = 20;
    private static final int SELL_BIN_SLOT = 22;
    private static final int BUYBACK_SLOT = 24;
    private static final int INFO_SLOT = 16;

    public ShopGUI(SimpleSellPlugin plugin, SellService sellService, LevelManager levelManager) {
        this.plugin = plugin;
        this.sellService = sellService;
        this.levelManager = levelManager;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        ItemStack border = GuiUtil.coloredPane(Material.RED_STAINED_GLASS_PANE, " ");
        ItemStack borderAccent = GuiUtil.coloredPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26};
        for (int slot : borderSlots) {
            gui.setItem(slot, (slot % 2 == 0) ? border : borderAccent);
        }

        Economy econ = plugin.getEconomy();
        double balance = econ != null ? econ.getBalance(player) : 0;
        gui.setItem(BALANCE_SLOT, GuiUtil.namedItem(Material.GOLD_INGOT,
                "&6&lYour Balance",
                "&f$" + String.format("%.2f", balance)));

        ItemStack handItem = player.getInventory().getItemInMainHand();
        List<String> handLore = new ArrayList<>();
        if (handItem != null && handItem.getType() != Material.AIR && !plugin.isBlacklisted(handItem.getType().name())) {
            String matName = handItem.getType().name();
            double unitPrice = sellService.getEffectivePrice(player, matName);
            double total = unitPrice * handItem.getAmount();
            int level = levelManager.getLevel(player, matName);
            handLore.add("&7Holding: &f" + sellService.formatName(matName) + " x" + handItem.getAmount());
            handLore.add("&7Worth: &a$" + String.format("%.2f", total) + " &8(Lv." + level + ")");
            handLore.add("");
            handLore.add("&e&lClick to sell!");
        } else {
            handLore.add("&7You aren't holding anything sellable.");
        }
        // Use a plain, non-weapon icon so no attack damage/speed lore ever shows
        gui.setItem(SELL_HAND_SLOT, GuiUtil.namedItem(Material.GOLDEN_APPLE, "&c&lSell Item in Hand", handLore.toArray(new String[0])));

        gui.setItem(SELL_ALL_SLOT, GuiUtil.namedItem(Material.CHEST, "&c&lSell Entire Inventory",
                "&7Sells every sellable item",
                "&7you're carrying at once.",
                "",
                "&e&lClick to sell all!"));

        List<String> priceLore = new ArrayList<>();
        if (handItem != null && handItem.getType() != Material.AIR) {
            if (plugin.isBlacklisted(handItem.getType().name())) {
                priceLore.add("&c" + sellService.formatName(handItem.getType().name()) + " cannot be sold.");
            } else {
                String matName = handItem.getType().name();
                double unitPrice = sellService.getEffectivePrice(player, matName);
                priceLore.add("&f" + sellService.formatName(matName));
                priceLore.add("&7Sells for &a$" + String.format("%.2f", unitPrice) + " &7each");
            }
        } else {
            priceLore.add("&7Hold an item to see its price here.");
        }
        gui.setItem(PRICE_CHECK_SLOT, GuiUtil.namedItem(Material.PAPER, "&6&lPrice Check", priceLore.toArray(new String[0])));

        gui.setItem(INFO_SLOT, GuiUtil.namedItem(Material.WRITTEN_BOOK, "&e&lShop Guide",
                "&7New here? Read this to learn",
                "&7how selling, leveling, and",
                "&7rarity tiers all work.",
                "",
                "&e&lClick to open the guide!"));

        gui.setItem(BROWSE_SLOT, GuiUtil.namedItem(Material.BOOK, "&b&lBrowse Shop Prices",
                "&7See the value of every item",
                "&7in the shop, plus your",
                "&7selling level for each.",
                "",
                "&e&lClick to browse!"));

        gui.setItem(SELL_BIN_SLOT, GuiUtil.namedItem(Material.HOPPER, "&a&lDrag & Drop to Sell",
                "&7Toss any items into a bin",
                "&7and sell them all at once",
                "&7just by closing the screen.",
                "",
                "&e&lClick to open!"));

        gui.setItem(BUYBACK_SLOT, GuiUtil.namedItem(Material.CLOCK, "&d&lBuy Back",
                "&7Bought back items you sold",
                "&7within the last &e30 minutes&7.",
                "",
                "&e&lClick to view!"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot == SELL_HAND_SLOT) {
            sellService.sellHand(player);
            player.closeInventory();
        } else if (slot == SELL_ALL_SLOT) {
            sellService.sellAll(player);
            player.closeInventory();
        } else if (slot == PRICE_CHECK_SLOT) {
            sellService.checkPrice(player);
        } else if (slot == INFO_SLOT) {
            player.closeInventory();
            player.openBook(InfoBook.build());
        } else if (slot == BROWSE_SLOT) {
            plugin.getBrowseGUI().open(player, 0);
        } else if (slot == SELL_BIN_SLOT) {
            plugin.getSellBinGUI().open(player);
        } else if (slot == BUYBACK_SLOT) {
            plugin.getBuyBackGUI().open(player);
        }
    }
}
