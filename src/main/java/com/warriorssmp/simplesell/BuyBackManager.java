package com.warriorssmp.simplesell;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BuyBackManager {

    public static final long EXPIRY_MILLIS = 30 * 60 * 1000L; // 30 minutes
    private static final int MAX_ENTRIES_PER_PLAYER = 27; // fits one GUI page

    public static class SoldEntry {
        public final ItemStack item;
        public final double pricePaid;
        public final long soldAt;

        public SoldEntry(ItemStack item, double pricePaid, long soldAt) {
            this.item = item;
            this.pricePaid = pricePaid;
            this.soldAt = soldAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - soldAt > EXPIRY_MILLIS;
        }

        public long millisRemaining() {
            return Math.max(0, EXPIRY_MILLIS - (System.currentTimeMillis() - soldAt));
        }
    }

    private final Map<UUID, List<SoldEntry>> history = new HashMap<>();

    public void recordSale(Player player, ItemStack item, double totalPricePaid) {
        List<SoldEntry> entries = history.computeIfAbsent(player.getUniqueId(), id -> new ArrayList<>());
        entries.add(0, new SoldEntry(item.clone(), totalPricePaid, System.currentTimeMillis()));
        while (entries.size() > MAX_ENTRIES_PER_PLAYER) {
            entries.remove(entries.size() - 1);
        }
    }

    public List<SoldEntry> getEntries(Player player) {
        cleanupExpired(player.getUniqueId());
        return history.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void removeEntry(Player player, SoldEntry entry) {
        List<SoldEntry> entries = history.get(player.getUniqueId());
        if (entries != null) entries.remove(entry);
    }

    private void cleanupExpired(UUID uuid) {
        List<SoldEntry> entries = history.get(uuid);
        if (entries == null) return;
        entries.removeIf(SoldEntry::isExpired);
    }

    /** Called periodically to purge expired entries for everyone (memory cleanup). */
    public void cleanupAll() {
        for (List<SoldEntry> entries : history.values()) {
            entries.removeIf(SoldEntry::isExpired);
        }
    }
}
