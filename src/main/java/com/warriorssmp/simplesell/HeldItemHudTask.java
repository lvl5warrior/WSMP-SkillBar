package com.warriorssmp.simplesell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeldItemHudTask extends BukkitRunnable {

    // How long the HUD stays visible after you switch to (or pick up) a different item
    private static final long DISPLAY_DURATION_MILLIS = 3500;

    private final SimpleSellPlugin plugin;
    private final LevelManager levelManager;

    private static class HeldState {
        String materialName;
        long showUntil;
    }

    private final Map<UUID, HeldState> states = new HashMap<>();

    public HeldItemHudTask(SimpleSellPlugin plugin, LevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            String matName = (item == null || item.getType() == Material.AIR) ? null : item.getType().name();
            if (matName != null && plugin.isBlacklisted(matName)) matName = null;

            HeldState state = states.computeIfAbsent(player.getUniqueId(), id -> new HeldState());

            boolean changed = matName != null && !matName.equals(state.materialName);
            if (changed) {
                state.materialName = matName;
                state.showUntil = now + DISPLAY_DURATION_MILLIS;
            } else if (matName == null) {
                state.materialName = null; // empty hand clears tracking so re-holding the same item later re-triggers
            }

            if (matName != null && now < state.showUntil) {
                player.sendActionBar(buildLine(player, matName));
            }
        }
    }

    private Component buildLine(Player player, String matName) {
        double basePrice = plugin.getPrice(matName);
        int level = levelManager.getLevel(player, matName);
        double effectivePrice = basePrice * LevelManager.getMultiplier(level);
        Rarity rarity = levelManager.getRarity(matName);
        String bar = levelManager.buildProgressBar(player, matName, 8);

        String next;
        if (level < LevelManager.getMaxLevel()) {
            long needed = levelManager.quantityNeededForNextLevel(player, matName);
            next = ChatColor.GRAY + "(" + needed + " to next lvl)";
        } else {
            next = ChatColor.GOLD.toString() + ChatColor.BOLD + "MAX";
        }

        String line = ChatColor.WHITE + LevelManager.formatName(matName)
                + ChatColor.GRAY + " | " + ChatColor.GREEN + "$" + String.format("%.2f", effectivePrice)
                + ChatColor.GRAY + " | " + rarity.getColoredName()
                + ChatColor.GRAY + " | " + bar + " " + next;

        return LegacyComponentSerializer.legacySection().deserialize(line);
    }
}
