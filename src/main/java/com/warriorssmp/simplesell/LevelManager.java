package com.warriorssmp.simplesell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LevelManager {

    private static final int MAX_LEVEL = 99;

    // Relative growth shape (same for every item) - proportions of the way to max quantity
    // at each level. Grows slowly then explodes near the top, RuneScape-style.
    private static final double[] CURVE_RATIO = buildCurveRatios();

    // One threshold table per rarity tier, generated from the shared curve shape scaled
    // to that tier's target max quantity (see Rarity.getMaxLevelQuantity()).
    private static final Map<Rarity, long[]> THRESHOLDS_BY_RARITY = new EnumMap<>(Rarity.class);
    static {
        for (Rarity rarity : Rarity.values()) {
            THRESHOLDS_BY_RARITY.put(rarity, buildThresholds(rarity.getMaxLevelQuantity()));
        }
    }

    private final SimpleSellPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, Map<String, Long>> cache = new HashMap<>();

    public LevelManager(SimpleSellPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    private static double[] buildCurveRatios() {
        double[] points = new double[MAX_LEVEL + 1];
        double accumulated = 0;
        for (int level = 1; level < MAX_LEVEL; level++) {
            accumulated += Math.floor(level + 300.0 * Math.pow(2, level / 7.0));
            points[level + 1] = Math.floor(accumulated / 4.0);
        }
        double max = points[MAX_LEVEL];
        double[] ratios = new double[MAX_LEVEL + 1];
        for (int level = 1; level <= MAX_LEVEL; level++) {
            ratios[level] = points[level] / max;
        }
        return ratios;
    }

    private static long[] buildThresholds(long targetMaxQuantity) {
        long[] thresholds = new long[MAX_LEVEL + 1];
        long previous = -1;
        for (int level = 1; level <= MAX_LEVEL; level++) {
            long value = Math.round(CURVE_RATIO[level] * targetMaxQuantity);
            if (value <= previous) value = previous + 1;
            thresholds[level] = value;
            previous = value;
        }
        thresholds[1] = 0; // everyone starts at level 1 with 0 sold
        return thresholds;
    }

    private File playerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private Map<String, Long> getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            Map<String, Long> data = new HashMap<>();
            File file = playerFile(id);
            if (file.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                for (String key : yaml.getKeys(false)) {
                    data.put(key, yaml.getLong(key));
                }
            }
            return data;
        });
    }

    public void addSold(Player player, String materialName, int amount) {
        Rarity rarity = getRarity(materialName);
        Map<String, Long> data = getPlayerData(player.getUniqueId());
        long before = data.getOrDefault(materialName, 0L);
        long after = before + amount;
        data.put(materialName, after);
        save(player.getUniqueId(), data);

        int oldLevel = levelForQuantity(before, rarity);
        int newLevel = levelForQuantity(after, rarity);
        if (newLevel > oldLevel) {
            notifyLevelUp(player, materialName, newLevel);

            // Check every milestone level crossed in this one sale (in case a big /sell all
            // jumps straight past 25 or 50 in one go) rather than only the final level reached.
            List<Integer> milestones = plugin.getConfig().getIntegerList("discord.quarterly-levels");
            for (int milestone : milestones) {
                if (milestone > oldLevel && milestone <= newLevel) {
                    announceMilestone(player, materialName, milestone);
                }
            }
        }
    }

    private void announceMilestone(Player player, String materialName, int level) {
        String itemDisplay = formatName(materialName);

        if (plugin.getConfig().getBoolean("discord.enabled", true)) {
            String channel = plugin.getConfig().getString("discord.channel", "global");
            String message = "🏆 " + player.getName() + " just reached Level " + level + " selling " + itemDisplay + "!";
            // Uses DiscordSRV's built-in broadcast command from console rather than a hard
            // dependency on the DiscordSRV plugin itself - works as long as DiscordSRV is installed.
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord broadcast " + channel + " " + message);
        }

        if (level >= getMaxLevel()) {
            announceServerWide(player, materialName);
        }
    }

    private void announceServerWide(Player player, String materialName) {
        String itemDisplay = formatName(materialName);
        String message = ChatColor.GOLD + "" + ChatColor.BOLD + "\u2605 " + player.getName()
                + ChatColor.YELLOW + " has reached " + ChatColor.GOLD + "LEVEL 99" + ChatColor.YELLOW
                + " selling " + ChatColor.GOLD + itemDisplay + ChatColor.YELLOW + "! " + ChatColor.GOLD + "\u2605";

        Bukkit.broadcastMessage(message);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showTitle(Title.title(
                    Component.text(player.getName(), NamedTextColor.GOLD),
                    Component.text("Reached LEVEL 99 in " + itemDisplay + "!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        }
    }

    private void notifyLevelUp(Player player, String materialName, int newLevel) {
        String itemDisplay = formatName(materialName);

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Level Up!] " + ChatColor.YELLOW
                + itemDisplay + ChatColor.GRAY + " is now " + ChatColor.GOLD + "Level " + newLevel + ChatColor.GRAY + "!");

        player.showTitle(Title.title(
                Component.text("LEVEL UP!", NamedTextColor.GOLD),
                Component.text(itemDisplay + " - Lv. " + newLevel, NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));
    }

    public static String formatName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public long getSoldCount(Player player, String materialName) {
        return getPlayerData(player.getUniqueId()).getOrDefault(materialName, 0L);
    }

    public static class HighestLevelResult {
        public final String materialName;
        public final int level;
        public final long quantitySold;

        public HighestLevelResult(String materialName, int level, long quantitySold) {
            this.materialName = materialName;
            this.level = level;
            this.quantitySold = quantitySold;
        }
    }

    /** Finds the player's single highest-leveled item across everything they've ever sold. */
    public HighestLevelResult getHighestLevel(UUID uuid) {
        Map<String, Long> data = getPlayerData(uuid);
        String bestMaterial = null;
        int bestLevel = 0;
        long bestQuantity = 0;

        for (Map.Entry<String, Long> entry : data.entrySet()) {
            Rarity rarity = getRarity(entry.getKey());
            int level = levelForQuantity(entry.getValue(), rarity);
            if (level > bestLevel) {
                bestLevel = level;
                bestMaterial = entry.getKey();
                bestQuantity = entry.getValue();
            }
        }

        if (bestMaterial == null) return null;
        return new HighestLevelResult(bestMaterial, bestLevel, bestQuantity);
    }

    public Rarity getRarity(String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) return Rarity.COMMON;
        return RarityClassifier.classify(material, plugin);
    }

    public int getLevel(Player player, String materialName) {
        long sold = getSoldCount(player, materialName);
        return levelForQuantity(sold, getRarity(materialName));
    }

    public static int levelForQuantity(long quantitySold, Rarity rarity) {
        long[] thresholds = THRESHOLDS_BY_RARITY.get(rarity);
        for (int level = MAX_LEVEL; level >= 1; level--) {
            if (quantitySold >= thresholds[level]) {
                return level;
            }
        }
        return 1;
    }

    public long quantityNeededForNextLevel(Player player, String materialName) {
        Rarity rarity = getRarity(materialName);
        long sold = getSoldCount(player, materialName);
        int currentLevel = levelForQuantity(sold, rarity);
        if (currentLevel >= MAX_LEVEL) return 0;
        long[] thresholds = THRESHOLDS_BY_RARITY.get(rarity);
        return thresholds[currentLevel + 1] - sold;
    }

    /** Progress (0.0 to 1.0) through the current level, for drawing a progress bar. */
    public double getLevelProgress(Player player, String materialName) {
        Rarity rarity = getRarity(materialName);
        long sold = getSoldCount(player, materialName);
        int level = levelForQuantity(sold, rarity);
        if (level >= MAX_LEVEL) return 1.0;
        long[] thresholds = THRESHOLDS_BY_RARITY.get(rarity);
        long currentFloor = thresholds[level];
        long nextCeiling = thresholds[level + 1];
        long span = nextCeiling - currentFloor;
        if (span <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, (double) (sold - currentFloor) / span));
    }

    public static int getMaxLevel() {
        return MAX_LEVEL;
    }

    /**
     * The sell price multiplier earned from an item's level.
     * +0.5% per level, so max level (99) gives roughly +49.5% price.
     */
    public static double getMultiplier(int level) {
        return 1.0 + (level - 1) * 0.005;
    }

    /** Builds a colored text progress bar, e.g. [||||||....] Lv.42 */
    public String buildProgressBar(Player player, String materialName, int barLength) {
        int level = getLevel(player, materialName);
        double progress = getLevelProgress(player, materialName);
        int filled = (int) Math.round(progress * barLength);
        Rarity rarity = getRarity(materialName);

        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.DARK_GRAY).append("[");
        bar.append(rarity.getColor());
        for (int i = 0; i < filled; i++) {
            bar.append("|");
        }
        bar.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < barLength; i++) {
            bar.append("|");
        }
        bar.append(ChatColor.DARK_GRAY).append("] ");
        bar.append(ChatColor.GOLD).append("Lv.").append(level);
        return bar.toString();
    }

    private void save(UUID uuid, Map<String, Long> data) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(playerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save level data for " + uuid + ": " + e.getMessage());
        }
    }
}
