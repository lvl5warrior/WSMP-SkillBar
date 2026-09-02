package com.warriorssmp.skillbar;

import org.bukkit.Bukkit;
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

/** A single read-only GUI page listing every installed skill's level and
 *  progress toward the next one — snapshotted at open time, same as every
 *  other WSMP plugin's menus (reopen to refresh). */
public final class MenuManager implements Listener {

    private static final String TITLE = "§6§l⭐ Your Skills";

    private final SkillBarPlugin plugin;

    public MenuManager(SkillBarPlugin plugin) {
        this.plugin = plugin;
    }

    public void openSkillsMenu(Player player) {
        List<SkillLookup.SkillLevel> levels = plugin.skillLookup().getAllLevels(player.getUniqueId());

        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        if (levels.isEmpty()) {
            gui.setItem(13, item(Material.BARRIER, "§cNo WSMP skill plugins found",
                    List.of("§7None of Mining/Woodcutting/Farming/", "§7Fishing/Cooking/Hunter are installed.")));
        } else {
            int[] slots = {10, 11, 12, 13, 14, 15, 16};
            int slot = 0;
            for (SkillLookup.SkillLevel level : levels) {
                if (slot >= slots.length) break;
                gui.setItem(slots[slot++], skillItem(level));
            }
        }

        gui.setItem(22, closeButton());
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler());
        }
        player.openInventory(gui);
    }

    private ItemStack skillItem(SkillLookup.SkillLevel level) {
        int barLength = 20;
        int filled = (int) Math.round(level.progressFraction() * barLength);
        String bar = "§a" + "█".repeat(filled) + "§7" + "░".repeat(barLength - filled);

        List<String> lore = new ArrayList<>();
        lore.add("§7Level §f" + level.level());
        if (level.maxLevel()) {
            lore.add("§eMAX LEVEL");
        } else {
            lore.add("§7XP: §f" + level.xpIntoLevel() + " / " + level.xpForNextLevel());
            lore.add(bar);
        }

        return item(level.icon(), "§6§l" + level.displayName(), lore);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack closeButton() {
        return item(Material.BARRIER, "§c❌ Close", List.of());
    }

    private ItemStack filler() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && clicked.getType() == Material.BARRIER
                && clicked.hasItemMeta() && "§c❌ Close".equals(clicked.getItemMeta().getDisplayName())) {
            player.closeInventory();
        }
    }
}
