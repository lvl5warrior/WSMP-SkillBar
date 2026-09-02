package com.warriorssmp.skillbar;

import com.warriorssmp.skillbar.model.XpTable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Reads each player's level out of whichever WSMP skill plugins are actually
 * installed on this server, entirely via reflection against their existing
 * public API (`plugin.dataStore().get(uuid).totalXp`) — no compile-time
 * dependency on any of the six plugin jars, no code changes needed in any of
 * them, and a missing/disabled plugin is just skipped rather than breaking
 * anything.
 *
 * Side effect worth knowing: calling dataStore().get(uuid) on a plugin the
 * player has never used will lazily create an empty (level 1) player-data
 * entry for them in that plugin, the same as it would if the player opened
 * that plugin's menu for the first time. Harmless, but it does mean SkillBar
 * can cause a "playerdata/<uuid>.yml" file to appear in a skill plugin's
 * folder for a player who's never touched that skill.
 */
public final class SkillLookup {

    public record SkillDef(String pluginName, String mainClass, String displayName, Material icon) {}

    private static final List<SkillDef> SKILLS = List.of(
            new SkillDef("WSMP-Mining", "com.warriorssmp.mining.MiningPlugin", "Mining", Material.DIAMOND_PICKAXE),
            new SkillDef("WSMP-Woodcutting", "com.warriorssmp.woodcutting.WoodcuttingPlugin", "Woodcutting", Material.DIAMOND_AXE),
            new SkillDef("WSMP-Farming", "com.warriorssmp.farming.FarmingPlugin", "Farming", Material.DIAMOND_HOE),
            new SkillDef("WSMP-Fishing", "com.warriorssmp.fishing.FishingPlugin", "Fishing", Material.FISHING_ROD),
            new SkillDef("WSMP-Cooking", "com.warriorssmp.cooking.CookingPlugin", "Cooking", Material.CAKE),
            new SkillDef("WSMP-Hunter", "com.warriorssmp.hunter.HunterPlugin", "Hunter", Material.DIAMOND_SWORD)
    );

    public record SkillLevel(String displayName, Material icon, int level, long totalXp,
                              long xpIntoLevel, long xpForNextLevel, boolean maxLevel) {

        public double progressFraction() {
            if (maxLevel || xpForNextLevel <= 0) return 1.0;
            double frac = (double) xpIntoLevel / xpForNextLevel;
            return Math.max(0.0, Math.min(1.0, frac));
        }
    }

    private final SkillBarPlugin plugin;

    public SkillLookup(SkillBarPlugin plugin) {
        this.plugin = plugin;
    }

    /** Which of the six skill plugins are actually installed and enabled right now. */
    public List<SkillDef> installedSkills() {
        List<SkillDef> present = new ArrayList<>();
        for (SkillDef def : SKILLS) {
            Plugin p = Bukkit.getPluginManager().getPlugin(def.pluginName());
            if (p != null && p.isEnabled()) present.add(def);
        }
        return present;
    }

    public List<SkillLevel> getAllLevels(UUID uuid) {
        List<SkillLevel> results = new ArrayList<>();
        for (SkillDef def : installedSkills()) {
            SkillLevel level = getLevel(def, uuid);
            if (level != null) results.add(level);
        }
        return results;
    }

    /** Opens that skill plugin's own main menu for the player, via
     *  `plugin.menuManager().openMainMenu(player)` — the exact same accessor
     *  every WSMP skill plugin already exposes. Returns false if the plugin
     *  isn't installed or its API shape doesn't match. */
    public boolean openSkillMenu(SkillDef def, org.bukkit.entity.Player player) {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin(def.pluginName());
            if (p == null) return false;

            Method menuManagerMethod = p.getClass().getMethod("menuManager");
            Object menuManager = menuManagerMethod.invoke(p);

            Method openMainMenu = menuManager.getClass().getMethod("openMainMenu", org.bukkit.entity.Player.class);
            openMainMenu.invoke(menuManager, player);
            return true;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Couldn't open " + def.pluginName() + "'s menu — its API may have changed.", e);
            return false;
        }
    }

    private SkillLevel getLevel(SkillDef def, UUID uuid) {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin(def.pluginName());
            if (p == null) return null;

            Method dataStoreMethod = p.getClass().getMethod("dataStore");
            Object dataStore = dataStoreMethod.invoke(p);

            Method getMethod = dataStore.getClass().getMethod("get", UUID.class);
            Object playerData = getMethod.invoke(dataStore, uuid);

            long totalXp = playerData.getClass().getField("totalXp").getLong(playerData);
            int level = XpTable.levelForXp(totalXp);
            long xpAtLevel = XpTable.xpForLevel(level);
            long xpAtNext = XpTable.xpForNextLevel(level);
            boolean maxLevel = level >= XpTable.MAX_LEVEL;
            long xpIntoLevel = totalXp - xpAtLevel;
            long xpNeeded = Math.max(1, xpAtNext - xpAtLevel);

            return new SkillLevel(def.displayName(), def.icon(), level, totalXp, xpIntoLevel, xpNeeded, maxLevel);
        } catch (ReflectiveOperationException | ClassCastException e) {
            plugin.getLogger().log(Level.WARNING,
                    "Couldn't read a level from " + def.pluginName() + " — its API may have changed.", e);
            return null;
        }
    }
}
