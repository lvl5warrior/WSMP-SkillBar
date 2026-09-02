package com.warriorssmp.simplesell;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;

public class SimpleSellPlugin extends JavaPlugin {

    private Economy economy;
    private double defaultPrice;
    private double globalMultiplier;
    private final Set<String> blacklist = new HashSet<>();

    private LevelManager levelManager;
    private BuyBackManager buyBackManager;
    private ShopGUI shopGUI;
    private BrowseGUI browseGUI;
    private BuyBackGUI buyBackGUI;
    private SellBinGUI sellBinGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling WSMP-SimpleSell. Make sure Vault + EssentialsX (or another economy plugin) is installed.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        levelManager = new LevelManager(this);
        buyBackManager = new BuyBackManager();

        SellService sellService = new SellService(this, levelManager, buyBackManager);

        SellCommand sellCommand = new SellCommand(this, sellService);
        getCommand("sell").setExecutor(sellCommand);
        getCommand("sell").setTabCompleter(sellCommand);

        shopGUI = new ShopGUI(this, sellService, levelManager);
        browseGUI = new BrowseGUI(this, levelManager);
        buyBackGUI = new BuyBackGUI(this, buyBackManager);
        sellBinGUI = new SellBinGUI(this, sellService);

        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(browseGUI, this);
        getServer().getPluginManager().registerEvents(buyBackGUI, this);
        getServer().getPluginManager().registerEvents(sellBinGUI, this);

        getCommand("shop").setExecutor((CommandSender sender, Command command, String label, String[] args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            shopGUI.open((Player) sender);
            return true;
        });

        // Clean up expired buy-back entries every 5 minutes to keep memory tidy
        getServer().getScheduler().runTaskTimer(this, buyBackManager::cleanupAll, 6000L, 6000L);

        // Shows a brief action-bar HUD (price, rarity, level bar) for a few seconds
        // whenever you switch to a different item - does NOT modify the actual item,
        // so stacking is never affected.
        new HeldItemHudTask(this, levelManager).runTaskTimer(this, 10L, 10L);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WSMPPlaceholders(this, levelManager).register();
            getLogger().info("Registered PlaceholderAPI placeholders (%wsmpsimplesell_...%)");
        } else {
            getLogger().info("PlaceholderAPI not found - skipping placeholder registration (this is optional).");
        }

        getLogger().info("WSMP-SimpleSell enabled! Every item now has a value.");
    }

    private void loadSettings() {
        reloadConfig();
        defaultPrice = getConfig().getDouble("default-price", 1.0);
        globalMultiplier = getConfig().getDouble("global-multiplier", 1.0);
        blacklist.clear();
        for (String mat : getConfig().getStringList("blacklist")) {
            blacklist.add(mat.toUpperCase());
        }
    }

    public void reloadSettings() {
        loadSettings();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public double getPrice(String materialName) {
        double base;
        if (getConfig().contains("prices." + materialName)) {
            // Explicitly priced in config.yml - use that value directly
            base = getConfig().getDouble("prices." + materialName);
        } else {
            // Not explicitly priced - use a price appropriate to the item's rarity tier
            // instead of a flat fallback (so a Netherite Axe doesn't sell for the same $1
            // as a stick just because neither is hand-listed in the config).
            Material material = Material.matchMaterial(materialName);
            Rarity rarity = material != null ? RarityClassifier.classify(material, this) : Rarity.COMMON;
            base = rarity.getDefaultPrice();
        }
        return base * globalMultiplier;
    }

    public boolean isBlacklisted(String materialName) {
        return blacklist.contains(materialName.toUpperCase());
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public BrowseGUI getBrowseGUI() {
        return browseGUI;
    }

    public BuyBackGUI getBuyBackGUI() {
        return buyBackGUI;
    }

    public SellBinGUI getSellBinGUI() {
        return sellBinGUI;
    }
}
