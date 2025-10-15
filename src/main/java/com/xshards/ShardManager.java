package com.xshards;

import com.xshards.DatabaseManager;
import com.xshards.scheduler.SchedulerAdapter;
import com.xshards.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player shard data
 */
public class ShardManager {

    private final DatabaseManager databaseManager;
    private final SchedulerAdapter scheduler;
    private final MessageManager messages;

    // Cache for player shard data
    private final Map<UUID, Integer> shardCache;

    // Pending shop purchases
    private final Map<UUID, Object> pendingPurchases;

    public ShardManager(org.bukkit.plugin.Plugin plugin, DatabaseManager databaseManager,
                        SchedulerAdapter scheduler, MessageManager messages) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
        this.messages = messages;
        this.shardCache = new ConcurrentHashMap<>();
        this.pendingPurchases = new ConcurrentHashMap<>();

        loadAllPlayerData();
    }

    /**
     * Add shards to a player
     */
    public void addShards(Player player, int amount) {
        UUID uuid = player.getUniqueId();

        // Update cache
        int current = shardCache.getOrDefault(uuid, 0);
        int newAmount = current + amount;
        shardCache.put(uuid, newAmount);

        // Send message if amount is positive
        if (amount > 0) {
            messages.sendShardsEarned(player, amount);
        }

        // Save asynchronously
        scheduler.runAsync(() -> savePlayerData(player));
    }

    /**
     * Get player's shard count
     */
    public int getShards(Player player) {
        UUID uuid = player.getUniqueId();

        // Check cache first
        if (shardCache.containsKey(uuid)) {
            return shardCache.get(uuid);
        }

        // Load from database
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT shards FROM player_shards WHERE uuid = ?")) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int shards = rs.getInt("shards");
                shardCache.put(uuid, shards);
                return shards;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error getting player shards: " + e.getMessage());
        }

        // Default to 0
        shardCache.put(uuid, 0);
        return 0;
    }

    /**
     * Set player's shard count
     */
    public void setShards(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        shardCache.put(uuid, Math.max(0, amount));
        scheduler.runAsync(() -> savePlayerData(player));
    }

    /**
     * Save player data to database
     */
    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        int shards = shardCache.getOrDefault(uuid, 0);

        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                    ? "INSERT INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), shards=VALUES(shards)"
                    : "INSERT OR REPLACE INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, playerName);
                stmt.setInt(3, shards);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save player data for " + playerName, e);
        }
    }

    /**
     * Load player data from database
     */
    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT shards FROM player_shards WHERE uuid = ?")) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int shards = rs.getInt("shards");
                shardCache.put(uuid, shards);
            } else {
                // New player
                shardCache.put(uuid, 0);
                savePlayerData(player);
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load player data for " + player.getName(), e);
            shardCache.put(uuid, 0);
        }
    }

    /**
     * Save all player data
     */
    public void saveAllPlayerData() {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                    ? "INSERT INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), shards=VALUES(shards)"
                    : "INSERT OR REPLACE INTO player_shards (uuid, player_name, shards) VALUES (?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<UUID, Integer> entry : shardCache.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        stmt.setString(1, entry.getKey().toString());
                        stmt.setString(2, player.getName());
                        stmt.setInt(3, entry.getValue());
                        stmt.addBatch();
                    }
                }
                stmt.executeBatch();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save all player data", e);
        }
    }

    /**
     * Load all player data (for online players)
     */
    public void loadAllPlayerData() {
        shardCache.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player);
        }
    }

    /**
     * Set pending purchase for a player
     */
    public void setPendingPurchase(Player player, Object item) {
        pendingPurchases.put(player.getUniqueId(), item);
    }

    /**
     * Get pending purchase for a player
     */
    public Object getPendingPurchase(Player player) {
        return pendingPurchases.get(player.getUniqueId());
    }

    /**
     * Clear pending purchase for a player
     */
    public void clearPendingPurchase(Player player) {
        pendingPurchases.remove(player.getUniqueId());
    }

    /**
     * Get cached shard data
     */
    public Map<UUID, Integer> getShardCache() {
        return shardCache;
    }
}