package com.warriorssmp.skillbar;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Registers %wsmpskillbar_highest_skill%, %wsmpskillbar_highest_level%, and
 * %wsmpskillbar_highest_skill_level% — whichever installed skill the player
 * has the highest level in, computed the same way as the /skills menu (via
 * SkillLookup's reflection against each plugin's public API). If two skills
 * tie for highest, whichever comes first in SkillLookup's fixed order wins.
 */
public final class SkillBarPlaceholders extends PlaceholderExpansion {

    private final SkillBarPlugin plugin;

    public SkillBarPlaceholders(SkillBarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wsmpskillbar";
    }

    @Override
    public @NotNull String getAuthor() {
        return "WarriorsSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        List<SkillLookup.SkillLevel> levels = plugin.skillLookup().getAllLevels(player.getUniqueId());
        Optional<SkillLookup.SkillLevel> highest = levels.stream()
                .max(Comparator.comparingInt(SkillLookup.SkillLevel::level));

        return switch (params.toLowerCase()) {
            case "highest_skill" -> highest.map(SkillLookup.SkillLevel::displayName).orElse("");
            case "highest_level" -> highest.map(l -> String.valueOf(l.level())).orElse("");
            case "highest_skill_level" -> highest.map(l -> l.displayName() + " - §6Lv." + l.level()).orElse("");
            default -> null;
        };
    }
}
