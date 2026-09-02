package com.warriorssmp.skillbar;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One boss bar per online player, cycling through whichever skills are
 * installed every few seconds. A player with the feature toggled off (see
 * PreferenceStore) never gets a bar shown to them at all.
 */
public final class BossBarManager {

    private final SkillBarPlugin plugin;
    private final SkillLookup lookup;
    private final Map<UUID, BossBar> activeBars = new HashMap<>();
    private final Map<UUID, Integer> cycleIndex = new HashMap<>();
    private BukkitTask cycleTask;

    public BossBarManager(SkillBarPlugin plugin, SkillLookup lookup) {
        this.plugin = plugin;
        this.lookup = lookup;
    }

    public void start() {
        long intervalTicks = plugin.getConfig().getLong("settings.cycle-seconds", 4) * 20L;
        cycleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new java.util.ArrayList<>(activeBars.keySet())) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        removeBar(uuid);
                        continue;
                    }
                    tick(player);
                }
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (cycleTask != null) cycleTask.cancel();
        for (UUID uuid : new java.util.ArrayList<>(activeBars.keySet())) {
            removeBar(uuid);
        }
    }

    /** Shows the bar for a player if their preference has it enabled. Called on join. */
    public void showFor(Player player) {
        if (!plugin.preferences().isEnabled(player.getUniqueId())) return;
        if (activeBars.containsKey(player.getUniqueId())) return;

        BossBar bar = BossBar.bossBar(Component.text("Loading skills..."), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        activeBars.put(player.getUniqueId(), bar);
        cycleIndex.put(player.getUniqueId(), 0);
        player.showBossBar(bar);
        tick(player);
    }

    public void hideFor(Player player) {
        removeBar(player.getUniqueId());
    }

    public void removeBar(UUID uuid) {
        BossBar bar = activeBars.remove(uuid);
        cycleIndex.remove(uuid);
        if (bar == null) return;
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) player.hideBossBar(bar);
    }

    public boolean isShowing(UUID uuid) {
        return activeBars.containsKey(uuid);
    }

    private void tick(Player player) {
        BossBar bar = activeBars.get(player.getUniqueId());
        if (bar == null) return;

        List<SkillLookup.SkillLevel> levels = lookup.getAllLevels(player.getUniqueId());
        if (levels.isEmpty()) {
            bar.name(Component.text("§7No WSMP skill plugins found"));
            bar.progress(0f);
            return;
        }

        int idx = cycleIndex.getOrDefault(player.getUniqueId(), 0) % levels.size();
        cycleIndex.put(player.getUniqueId(), idx + 1);

        SkillLookup.SkillLevel level = levels.get(idx);
        bar.color(level.color());

        String text = level.maxLevel()
                ? "§f" + level.displayName() + " §7— Level " + level.level() + " §f(MAX)"
                : "§f" + level.displayName() + " §7— Level " + level.level() + " §f(" + level.xpIntoLevel() + "/" + level.xpForNextLevel() + " XP)";
        bar.name(Component.text(text));
        bar.progress((float) level.progressFraction());
    }
}
