package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Sound;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkManager {
    private final Xshards plugin;
    private final Map<UUID, AfkData> afkData;
    private final File afkDataFile;
    private final File afkLocationFile; // File for AFK location
    private FileConfiguration afkDataConfig;
    private Location afkLocation; // The designated AFK location
    private final Map<UUID, Long> afkStartTime; // Track when a player entered AFK mode

    public AfkManager(Xshards plugin) {
        this.plugin = plugin;
        this.afkData = new HashMap<>();
        this.afkStartTime = new HashMap<>(); // Initialize afkStartTime map
        this.afkDataFile = new File(plugin.getDataFolder(), "afkdata.yml");
        this.afkLocationFile = new File(plugin.getDataFolder(), "afkmod.yml"); // Ensure afkmod.yml exists
        loadAfkData();
        loadAfkLocation(); // Load AFK location from the config
    }

    // Load AFK location from config
    public void loadAfkLocation() {
        if (plugin.getConfig().contains("afkLocation")) {
            String worldName = plugin.getConfig().getString("afkLocation.world");
            double x = plugin.getConfig().getDouble("afkLocation.x");
            double y = plugin.getConfig().getDouble("afkLocation.y");
            double z = plugin.getConfig().getDouble("afkLocation.z");
            afkLocation = new Location(Bukkit.getWorld(worldName), x, y, z);
        } else {
            afkLocation = null;
        }
    }
    
    // AfkManager.java

public void removeAfkLocation() {
    afkLocation = null; // Set the AFK location to null
}

// Add this method to save the AFK location in afkmod.yml if necessary
public void saveAfkLocation() {
    if (afkLocation != null) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(afkLocationFile);
        config.set("afkLocation.world", afkLocation.getWorld().getName());
        config.set("afkLocation.x", afkLocation.getX());
        config.set("afkLocation.y", afkLocation.getY());
        config.set("afkLocation.z", afkLocation.getZ());
        try {
            config.save(afkLocationFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save AFK location: " + e.getMessage());
        }
    }
}

    // Set AFK location from player's current position
    public void setAfkLocation(Player player) {
        afkLocation = player.getLocation(); // Set location to player's current location
        saveAfkLocationToConfig(); // Save to config
    }

    // Save AFK location to configuration file
    private void saveAfkLocationToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("afkLocation.world", afkLocation.getWorld().getName());
        config.set("afkLocation.x", afkLocation.getX());
        config.set("afkLocation.y", afkLocation.getY());
        config.set("afkLocation.z", afkLocation.getZ());
        plugin.saveConfig(); // Save the changes to the config file
    }

    public void setAfk(Player player) {
        if (!afkData.containsKey(player.getUniqueId())) {
            setLastLocation(player, player.getLocation());
            teleportToAfk(player); // Teleport the player to the AFK location
            afkStartTime.put(player.getUniqueId(), System.currentTimeMillis()); // Record AFK start time
        }
    }

    public void saveAllAfkData() {
        saveAfkData(); // This will save all the AFK data
    }

    // Get AFK location
    public Location getAfkLocation() {
        return afkLocation;
    }

    // Get the last saved location before the player went AFK
    public Location getLastLocation(Player player) {
        return afkData.get(player.getUniqueId()).getLastLocation();
    }

    public boolean isAfk(Player player) {
        return afkData.containsKey(player.getUniqueId()); // Check if the player has AFK data
    }

    // Set the player's last location before AFK
    public void setLastLocation(Player player, Location location) {
        AfkData data = afkData.getOrDefault(player.getUniqueId(), new AfkData()); // Use default constructor
        data.setLastLocation(location);
        afkData.put(player.getUniqueId(), data);
        saveAfkData();  // Save after updating
    }

    // Save all AFK data
    public void saveAfkData() {
        for (UUID uuid : afkData.keySet()) {
            AfkData data = afkData.get(uuid);
            afkDataConfig.set(uuid.toString() + ".lastLocation", data.getLastLocation());
        }
        try {
            afkDataConfig.save(afkDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save AFK data: " + e.getMessage());
        }
    }

    // Load AFK data from afkdata.yml
    public void loadAfkData() {
        if (!afkDataFile.exists()) {
            plugin.saveResource("afkdata.yml", false);
        }
        afkDataConfig = YamlConfiguration.loadConfiguration(afkDataFile);
        for (String uuidStr : afkDataConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            Location lastLocation = (Location) afkDataConfig.get(uuidStr + ".lastLocation");
            afkData.put(uuid, new AfkData(lastLocation));
        }
    }

    // Remove AFK data (when player exits AFK)
    public void removeAfkData(Player player) {
        afkData.remove(player.getUniqueId());
        afkStartTime.remove(player.getUniqueId()); // Remove AFK start time
        saveAfkData();
    }

    // Teleport player to AFK location
    public void teleportToAfk(Player player) {
        if (afkLocation != null) {
            setLastLocation(player, player.getLocation()); // Save player's current location
            player.teleport(afkLocation); // Teleport to AFK location
            startAfkShardEarning(player); // Start AFK shard earning
        } else {
            player.sendMessage("AFK location is not set.");
        }
    }

    // Quit AFK and teleport back to the last location
    public void quitAfk(Player player) {
        Location lastLocation = getLastLocation(player);
        if (lastLocation != null) {
            player.teleport(lastLocation); // Teleport player back to their last position
            removeAfkData(player); // Remove player from AFK
            player.sendMessage("You have quit AFK mode.");
        } else {
            player.sendMessage("You were not in AFK mode.");
        }
    }

    // Start shard earning when player is AFK
    private void startAfkShardEarning(Player player) {
    long afkEarnSeconds = plugin.getConfig().getLong("afkearnseconds", 30); // Default to 30 seconds
    int afkAmount = plugin.getConfig().getInt("afkamount", 1); // Default earn 1 shard per interval

    // Track time until the next shard earning
    new BukkitRunnable() {
        int countdown = (int) afkEarnSeconds; // Countdown variable

        @Override
        public void run() {
            if (isAfk(player)) {
                // Update the title to show the countdown
                player.sendTitle("", ChatColor.LIGHT_PURPLE + "EARN SHARDS IN " + countdown, 7, 12, 7);

                // Check if the countdown has reached 0
                if (countdown <= 0) {
                    plugin.getShardManager().addShards(player, afkAmount);
                    player.sendMessage(ChatColor.GREEN + "You earned " + afkAmount + " shard(s) while AFK!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f); // Notify player

                    // Reset the countdown for the next earning interval
                    countdown = (int) afkEarnSeconds; 
                } else {
                    countdown--; // Decrement the countdown
                }
            } else {
                cancel(); // Stop the task if the player is no longer AFK
            }
        }
    }.runTaskTimer(plugin, 0, 20); // Check every second (20 ticks)
}

    // Check if player has moved from their initial location
    public boolean hasPlayerMoved(Player player, Location initialLocation) {
        Location currentLocation = player.getLocation();
        return initialLocation.getX() != currentLocation.getX() ||
               initialLocation.getY() != currentLocation.getY() ||
               initialLocation.getZ() != currentLocation.getZ();
    }

    public Xshards getPlugin() {
        return this.plugin; // Getter for the plugin
    }
}