package com.warriorssmp.simplesell;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowseGUI implements Listener {

    private static final String TITLE_PREFIX = ChatColor.translateAlternateColorCodes('&', "&4&lWSMP &8» &7Browse &8- Page ");
    private static final int ITEMS_PER_PAGE = 45;

    private final SimpleSellPlugin plugin;
    private final LevelManager levelManager;
    private final Map<String, Integer> lastPage = new HashMap<>();
    private List<Material> itemList;

    public BrowseGUI(SimpleSellPlugin plugin, LevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }

    /**
     * Builds the full sellable item catalog directly from the server's live Material list.
     * This automatically includes every item the running server version supports
     * (weapons, tools, and anything added in newer game versions) without needing
     * a manually maintained list.
     */
    private List<Material> getItemList() {
        if (itemList == null) {
            itemList = new ArrayList<>();
            for (Material material : Material.values()) {
                if (!material.isItem()) continue; // skip block-only/technical materials
                if (material.isAir()) continue;
                if (material.isLegacy()) continue;
                if (plugin.isBlacklisted(material.name())) continue;
                itemList.add(material);
            }
            itemList.sort((a, b) -> a.name().compareTo(b.name()));
        }
        return itemList;
    }

    public void open(Player player, int page) {
        List<Material> items = getItemList();
        int maxPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, maxPage));
        lastPage.put(player.getUniqueId().toString(), page);

        Inventory gui = Bukkit.createInventory(null, 54, TITLE_PREFIX + (page + 1) + "/" + (maxPage + 1));

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        for (int i = start; i < end; i++) {
            Material material = items.get(i);
            String matName = material.name();

            double basePrice = plugin.getPrice(matName);
            int level = levelManager.getLevel(player, matName);
            long sold = levelManager.getSoldCount(player, matName);
            double effectivePrice = basePrice * LevelManager.getMultiplier(level);
            Rarity rarity = levelManager.getRarity(matName);

            ItemStack display = new ItemStack(material);
            ItemMeta meta = display.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&f" + formatName(matName)));

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Sell price: &a$" + String.format("%.2f", effectivePrice)));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Rarity: " + rarity.getColoredName()));
            lore.add(levelManager.buildProgressBar(player, matName, 10));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8" + sold + " sold total"));
            if (level < LevelManager.getMaxLevel()) {
                long needed = levelManager.quantityNeededForNextLevel(player, matName);
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Sell &e" + needed + "&7 more for next level"));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&6&lMAX LEVEL"));
            }
            meta.setLore(lore);
            GuiUtil.hideExtras(meta);
            display.setItemMeta(meta);

            gui.setItem(i - start, display);
        }

        if (page > 0) {
            gui.setItem(45, GuiUtil.namedItem(Material.ARROW, "&ePrevious Page"));
        }
        gui.setItem(49, GuiUtil.namedItem(Material.BARRIER, "&7Back to Shop"));
        if (page < maxPage) {
            gui.setItem(53, GuiUtil.namedItem(Material.ARROW, "&eNext Page"));
        }

        player.openInventory(gui);
    }

    private String formatName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int currentPage = lastPage.getOrDefault(player.getUniqueId().toString(), 0);

        if (slot == 45) {
            open(player, currentPage - 1);
        } else if (slot == 53) {
            open(player, currentPage + 1);
        } else if (slot == 49) {
            plugin.getShopGUI().open(player);
        }
    }
}
