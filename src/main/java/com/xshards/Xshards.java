package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Xshards extends JavaPlugin {
    private ShardManager shardManager;
    private ShopManager shopManager;
    private AfkManager afkManager; // Declare AfkManager

    @Override
    public void onEnable() {
        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Ensure the data files exist
        createDataFile("playerdata.yml");
        createDataFile("killsystem.yml");
        createDataFile("shopdata.yml");
        createDataFile("afkdata.yml"); // Ensure afk data file exists
        createDataFile("afkmod.yml");  // Ensure afk mod data file exists

        // Initialize managers
        shardManager = new ShardManager(this);
        shopManager = new ShopManager(this);
        afkManager = new AfkManager(this); // Initialize AfkManager

        // Load all data when the plugin is enabled
        shopManager.loadShopData(); // Load shop data
        afkManager.loadAfkData(); // Load AFK data
        afkManager.loadAfkLocation(); // Load AFK location
        shardManager.loadAllPlayerData();

        // Register commands and listeners
        getCommand("shards").setExecutor(new ShardCommand(shardManager));
        getCommand("store").setExecutor(new ShopCommand(shopManager));
        getCommand("xshards").setExecutor(new XshardsCommand(this));
        getCommand("afk").setExecutor(new AfkCommand(afkManager)); // Use existing afkManager instance
        getCommand("setafk").setExecutor(new SetAfkCommand(afkManager)); // Register setafk command
        getCommand("quitafk").setExecutor(new QuitAfkCommand(afkManager)); // Register quitafk command
        getCommand("afkremove").setExecutor(new AfkRemoveCommand(afkManager)); // Register afkremove command
        getServer().getPluginManager().registerEvents(new ShardListener(shardManager, this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, shardManager), this);
        getServer().getPluginManager().registerEvents(new AfkListener(afkManager), this); // Register AFK listener

        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new XshardsPlaceholder(shardManager).register();
            getLogger().info("PlaceholderAPI detected. Shards placeholders registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not be available.");
        }

        getLogger().info("Xshards has been enabled!");
    }

    @Override
public void onDisable() {
    // Remove all players from AFK mode
    for (Player player : Bukkit.getOnlinePlayers()) {
        if (afkManager.isAfk(player)) {
            afkManager.quitAfk(player);
        }
    }

    shardManager.saveAllPlayerData(); // Save data for all players
    afkManager.saveAllAfkData(); // Save AFK data for all players
    shopManager.saveShopData(); // Save shop data
    getLogger().info("Xshards has been disabled.");
}

    public ShardManager getShardManager() {
        return this.shardManager; // Add this method to access ShardManager
    }

    public void reloadPlugin() {
        reloadConfig();
        shopManager.loadShopData();
        // Reload other necessary data
    }
    
    public ShopManager getShopManager() {
        return this.shopManager;
    }

    public AfkManager getAfkManager() {
        return this.afkManager; // Add this method to access AfkManager
    }

    private void createDataFile(String filename) {
        File file = new File(getDataFolder(), filename);
        if (!file.exists()) {
            saveResource(filename, false);
        }
    }
}