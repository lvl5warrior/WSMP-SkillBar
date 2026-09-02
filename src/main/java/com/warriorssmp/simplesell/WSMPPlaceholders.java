package com.warriorssmp.simplesell;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class WSMPPlaceholders extends PlaceholderExpansion {

    private final SimpleSellPlugin plugin;
    private final LevelManager levelManager;

    public WSMPPlaceholders(SimpleSellPlugin plugin, LevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }

    @Override
    public String getIdentifier() {
        return "wsmpsimplesell";
    }

    @Override
    public String getAuthor() {
        return "WarriorsSMP";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // keep this expansion registered across /papi reload
    }

    /**
     * Available placeholders:
     * %wsmpsimplesell_highest_level%          -> just the level number, e.g. "42"
     * %wsmpsimplesell_highest_item%            -> the item name, e.g. "Diamond"
     * %wsmpsimplesell_highest_rarity%          -> the rarity name, e.g. "Rare"
     * %wsmpsimplesell_highest_full%            -> colored "Item - Lv.X" combined, ready for a hologram line
     */
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        LevelManager.HighestLevelResult result = levelManager.getHighestLevel(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "highest_level":
                return result == null ? "0" : String.valueOf(result.level);

            case "highest_item":
                return result == null ? "None" : LevelManager.formatName(result.materialName);

            case "highest_rarity":
                return result == null ? "None" : levelManager.getRarity(result.materialName).getDisplayName();

            case "highest_full":
                if (result == null) return "&7No items leveled yet";
                Rarity rarity = levelManager.getRarity(result.materialName);
                return rarity.getColor() + LevelManager.formatName(result.materialName)
                        + " &7- &6Lv." + result.level;

            default:
                return null; // let PlaceholderAPI know this placeholder isn't ours
        }
    }
}
