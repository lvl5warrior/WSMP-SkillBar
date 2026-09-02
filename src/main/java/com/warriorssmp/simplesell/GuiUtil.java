package com.warriorssmp.simplesell;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiUtil {

    /**
     * Hides the default extra tooltip lines Minecraft adds automatically
     * (attack damage, attack speed, "Unbreakable", enchant glow text, etc.)
     * so decorative GUI icons only show the name/lore we actually set.
     */
    public static void hideExtras(ItemMeta meta) {
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );
    }

    public static ItemStack namedItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(coloredLore);
        hideExtras(meta);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack coloredPane(Material material, String name) {
        return namedItem(material, name);
    }
}
