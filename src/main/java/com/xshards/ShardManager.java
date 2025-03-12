package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ShardManager {
    private final Xshards plugin;
    private final Map<Player, ShardData> playerData;
    private final File playerDataFile;
    private final Map<Player, ShopItem> pendingPurchases; // Map for pending purchases

    public ShardManager(Xshards plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.pendingPurchases = new HashMap<>(); // Initialize pending purchases
        loadAllPlayerData(); // Load data for all players initially
    }

    // Add shards to a player's account
    public void addShards(Player player, int amount) {
        ShardData data = playerData.getOrDefault(player, new ShardData());
        data.addShards(amount);
        playerData.put(player, data);
        player.sendMessage("You earned " + amount + " shards!");
        savePlayerData(player); // Save player's data immediately after updating
    }

    // Get the number of shards for a player
    public int getShards(Player player) {
        return playerData.getOrDefault(player, new ShardData()).getShards();
    }

    // Save the data for a specific player
    public void savePlayerData(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        String playerName = player.getName();
        config.set(playerName + ".shards", getShards(player));
        try {
            config.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + playerName + "!", e);
        }
    }

    // Load data for a specific player
    public void loadPlayerData(Player player) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        String playerName = player.getName();
        int shards = config.getInt(playerName + ".shards", 0); // Default to 0 shards if not found
        playerData.put(player, new ShardData(shards));
    }

    // Save data for all players
    public void saveAllPlayerData() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        for (Map.Entry<Player, ShardData> entry : playerData.entrySet()) {
            String playerName = entry.getKey().getName();
            config.set(playerName + ".shards", entry.getValue().getShards());
        }
        try {
            config.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data!", e);
        }
    }

    // Load data for all players (used at startup or if needed to load full data)
    public void loadAllPlayerData() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
        for (String playerName : config.getKeys(false)) {
            int shards = config.getInt(playerName + ".shards", 0); // Default to 0 if no data exists
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                playerData.put(player, new ShardData(shards));
            }
        }
    }

    // Pending purchase methods
    public void setPendingPurchase(Player player, ShopItem item) {
        pendingPurchases.put(player, item);
    }

    public ShopItem getPendingPurchase(Player player) {
        return pendingPurchases.get(player);
    }

    public void clearPendingPurchase(Player player) {
        pendingPurchases.remove(player);
    }
}