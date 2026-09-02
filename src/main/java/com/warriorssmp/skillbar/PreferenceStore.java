package com.warriorssmp.skillbar;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Whether each player wants the boss bar shown. Defaults to on for anyone
 *  not in the file yet. */
public final class PreferenceStore {

    private final SkillBarPlugin plugin;
    private final File file;
    private final Map<UUID, Boolean> cache = new HashMap<>();

    public PreferenceStore(SkillBarPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "preferences.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                cache.put(UUID.fromString(key), yml.getBoolean(key, true));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public boolean isEnabled(UUID uuid) {
        return cache.getOrDefault(uuid, true);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        cache.put(uuid, enabled);
        save();
    }

    private void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (var entry : cache.entrySet()) {
            yml.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save SkillBar preferences.yml", e);
        }
    }
}
