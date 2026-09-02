package com.warriorssmp.simplesell;

import org.bukkit.ChatColor;

public enum Rarity {
    TRIVIAL("Trivial", ChatColor.GRAY, 750_000, 0.25),
    COMMON("Common", ChatColor.WHITE, 60_000, 2.0),
    UNCOMMON("Uncommon", ChatColor.GREEN, 12_000, 8.0),
    RARE("Rare", ChatColor.BLUE, 1_800, 45.0),
    EPIC("Epic", ChatColor.LIGHT_PURPLE, 180, 300.0),
    LEGENDARY("Legendary", ChatColor.GOLD, 18, 3000.0);

    private final String displayName;
    private final ChatColor color;
    private final long maxLevelQuantity; // total quantity sold needed to reach level 99
    private final double defaultPrice;   // base sell price used when an item has no explicit price set

    Rarity(String displayName, ChatColor color, long maxLevelQuantity, double defaultPrice) {
        this.displayName = displayName;
        this.color = color;
        this.maxLevelQuantity = maxLevelQuantity;
        this.defaultPrice = defaultPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public long getMaxLevelQuantity() {
        return maxLevelQuantity;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public String getColoredName() {
        return color + displayName;
    }
}
