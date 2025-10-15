package com.xshards;

import com.xshards.ProxyManager;
import com.xshards.scheduler.SchedulerAdapter;
import com.xshards.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Xshards extends JavaPlugin {
    private ShardManager shardManager;
    private ShopManager shopManager;
    private AfkManager afkManager;
    private DatabaseManager databaseManager;
    private SchedulerAdapter scheduler;
    private MessageManager messageManager;
    private WorldGuardManager worldGuardManager;
    private ProxyManager proxyManager;

    @Override
    public void onEnable() {
        // Initialize ActionBarUtil
        com.xshards.utils.ActionBarUtil.initialize();

        // Save the default config if it doesn't exist
        saveDefaultConfig();

        // Create storage directory if it doesn't exist
        File storageDir = new File(getDataFolder(), "storage");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Initialize scheduler adapter (Folia/Bukkit compatibility)
        scheduler = new SchedulerAdapter(this);
        getLogger().info("Running on " + (scheduler.isFolia() ? "Folia" : "Bukkit"));

        // Initialize message manager
        messageManager = new MessageManager(getConfig());

        // Initialize database manager first
        databaseManager = new DatabaseManager(this);

        // Initialize WorldGuard manager
        worldGuardManager = new WorldGuardManager(this);

        // Initialize proxy manager
        proxyManager = new ProxyManager(this, messageManager);

        // Initialize managers with proper dependencies
        shardManager = new ShardManager(this, databaseManager, scheduler, messageManager);
        shopManager = new ShopManager(this);
        afkManager = new AfkManager(this, scheduler, messageManager, worldGuardManager, proxyManager);

        // Register commands
        getCommand("shards").setExecutor(new ShardCommand(shardManager, messageManager));
        getCommand("store").setExecutor(new ShopCommand(shopManager));
        getCommand("xshards").setExecutor(new XshardsCommand(this, messageManager));
        getCommand("afk").setExecutor(new AfkCommand(afkManager, messageManager));
        getCommand("setafk").setExecutor(new SetAfkCommand(afkManager, messageManager));
        getCommand("quitafk").setExecutor(new QuitAfkCommand(afkManager, messageManager));
        getCommand("afkremove").setExecutor(new AfkRemoveCommand(afkManager, messageManager));

        // Register listeners
        getServer().getPluginManager().registerEvents(new ShardListener(shardManager, this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, shardManager), this);
        getServer().getPluginManager().registerEvents(
                new AfkListener(afkManager, worldGuardManager, messageManager, scheduler), this
        );

        // PlaceholderAPI integration
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new XshardsPlaceholder(shardManager).register();
            getLogger().info("PlaceholderAPI detected. Shards placeholders registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not be available.");
        }

        // Clear any lingering AFK data from previous server session
        try {
            databaseManager.getConnection().prepareStatement("DELETE FROM afk_status").executeUpdate();
            getLogger().info("AFK status data has been reset on server startup.");
        } catch (Exception e) {
            getLogger().warning("Failed to clear AFK status data: " + e.getMessage());
        }

        getLogger().info("Xshards v2.0.0 has been enabled with " +
                (scheduler.isFolia() ? "Folia" : "Bukkit") + " support!");
    }

    @Override
    public void onDisable() {
        // Check if managers were initialized properly
        if (afkManager != null) {
            // Remove all players from AFK mode
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (afkManager.isAfk(player)) {
                    afkManager.quitAfk(player);
                }
            }
            afkManager.shutdown();
        }

        // Save all data if managers were initialized
        if (shardManager != null) {
            shardManager.saveAllPlayerData();
        }

        if (shopManager != null) {
            shopManager.saveShopData();
        }

        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        // Cancel all scheduled tasks
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        // Shutdown proxy manager
        if (proxyManager != null) {
            proxyManager.shutdown();
        }

        getLogger().info("Xshards has been disabled.");
    }

    public ShardManager getShardManager() {
        return this.shardManager;
    }

    public ShopManager getShopManager() {
        return this.shopManager;
    }

    public AfkManager getAfkManager() {
        return this.afkManager;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public SchedulerAdapter getScheduler() {
        return this.scheduler;
    }

    public WorldGuardManager getWorldGuardManager() {
        return this.worldGuardManager;
    }

    public ProxyManager getProxyManager() {
        return this.proxyManager;
    }

    public void reloadPlugin() {
        reloadConfig();

        // Reload message manager
        messageManager = new MessageManager(getConfig());

        // Reload database connection if storage type changed
        String currentStorageType = databaseManager.getStorageType();
        String configStorageType = getConfig().getString("storage.type", "sqlite");

        if (!currentStorageType.equals(configStorageType)) {
            getLogger().info("Storage type changed from " + currentStorageType + " to " + configStorageType + ". Reconnecting...");
            databaseManager.close();
            databaseManager = new DatabaseManager(this);

            // Reload all data
            shardManager.loadAllPlayerData();
            shopManager.loadShopData();
        }
    }
}