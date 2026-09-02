package com.warriorssmp.simplesell;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class SellBinGUI implements Listener {

    private static final String TITLE = ChatColor.translateAlternateColorCodes('&',
            "&4&lWSMP &8» &7Drag & Drop to Sell");
    private static final int SIZE = 36;

    // Top row is a border/info row; everything else (slots 9-35) is the actual sell bin
    private static final Set<Integer> BORDER_SLOTS = new HashSet<>();
    private static final int BACK_SLOT = 8;
    static {
        for (int i = 0; i < 9; i++) BORDER_SLOTS.add(i);
    }

    private final SimpleSellPlugin plugin;
    private final SellService sellService;

    public SellBinGUI(SimpleSellPlugin plugin, SellService sellService) {
        this.plugin = plugin;
        this.sellService = sellService;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);

        ItemStack border = GuiUtil.namedItem(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int slot : BORDER_SLOTS) {
            gui.setItem(slot, border);
        }
        gui.setItem(4, GuiUtil.namedItem(Material.HOPPER, "&a&lDrag items here to sell",
                "&7Drop any items into the",
                "&7empty slots below.",
                "",
                "&eClosing this screen sells",
                "&eeverything inside it!"));
        gui.setItem(BACK_SLOT, GuiUtil.namedItem(Material.ARROW, "&7Back to Shop",
                "&7Anything still in the bin",
                "&7sells automatically first."));

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        int rawSlot = event.getRawSlot();
        // Only block interacting with the top border row - the bin area (and the
        // player's own inventory below it) works completely normally
        if (rawSlot >= 0 && rawSlot < SIZE && BORDER_SLOTS.contains(rawSlot)) {
            event.setCancelled(true);
            if (rawSlot == BACK_SLOT && event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                plugin.getShopGUI().open(player); // closing this inventory (below) triggers the sell-on-close logic first
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < SIZE && BORDER_SLOTS.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        Inventory closed = event.getInventory();
        double total = 0.0;
        int itemsSold = 0;

        for (int slot = 9; slot < SIZE; slot++) {
            ItemStack item = closed.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String matName = item.getType().name();
            if (plugin.isBlacklisted(matName)) {
                giveBack(player, item);
                closed.setItem(slot, null);
                continue;
            }

            int amount = item.getAmount();
            total += sellService.sellFromBin(player, item);
            itemsSold += amount;
            closed.setItem(slot, null);
        }

        if (itemsSold > 0) {
            player.sendMessage(ChatColor.GREEN + "Sold " + itemsSold + " items from the sell bin for "
                    + ChatColor.GOLD + "$" + String.format("%.2f", total));
        }
    }

    private void giveBack(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }
}
