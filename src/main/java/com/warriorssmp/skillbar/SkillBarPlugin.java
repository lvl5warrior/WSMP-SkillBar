package com.warriorssmp.skillbar;

import com.warriorssmp.skillbar.command.SkillsCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkillBarPlugin extends JavaPlugin {

    private static SkillBarPlugin instance;

    private SkillLookup skillLookup;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        instance = this;

        this.skillLookup = new SkillLookup(this);
        this.menuManager = new MenuManager(this);

        getServer().getPluginManager().registerEvents(menuManager, this);
        getCommand("skills").setExecutor(new SkillsCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SkillBarPlaceholders(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        logInstalledSkills();
    }

    private void logInstalledSkills() {
        var installed = skillLookup.installedSkills();
        if (installed.isEmpty()) {
            getLogger().warning("No WSMP skill plugins found on this server — /skills will show "
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

    public MenuManager menuManager() {
        return menuManager;
    }
}
