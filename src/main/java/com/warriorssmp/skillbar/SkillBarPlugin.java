package com.warriorssmp.skillbar;

import com.warriorssmp.skillbar.command.SkillBarCommand;
import com.warriorssmp.skillbar.command.SkillsCommand;
import com.warriorssmp.skillbar.listener.PlayerConnectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkillBarPlugin extends JavaPlugin {

    private static SkillBarPlugin instance;

    private SkillLookup skillLookup;
    private BossBarManager bossBarManager;
    private PreferenceStore preferences;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.skillLookup = new SkillLookup(this);
        this.preferences = new PreferenceStore(this);
        this.bossBarManager = new BossBarManager(this, skillLookup);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getCommand("skillbar").setExecutor(new SkillBarCommand(this));
        getCommand("skills").setExecutor(new SkillsCommand(this));

        bossBarManager.start();
        logInstalledSkills();
    }

    @Override
    public void onDisable() {
        if (bossBarManager != null) bossBarManager.stop();
    }

    private void logInstalledSkills() {
        var installed = skillLookup.installedSkills();
        if (installed.isEmpty()) {
            getLogger().warning("No WSMP skill plugins found on this server — the boss bar will show "
                    + "\"No WSMP skill plugins found\" until at least one of Mining/Woodcutting/Farming/"
                    + "Fishing/Cooking/Hunter is installed.");
        } else {
            getLogger().info("Tracking " + installed.size() + " skill plugin(s): "
                    + installed.stream().map(SkillLookup.SkillDef::displayName).reduce((a, b) -> a + ", " + b).orElse(""));
        }
    }

    public static SkillBarPlugin get() {
        return instance;
    }

    public SkillLookup skillLookup() {
        return skillLookup;
    }

    public BossBarManager bossBarManager() {
        return bossBarManager;
    }

    public PreferenceStore preferences() {
        return preferences;
    }
}
