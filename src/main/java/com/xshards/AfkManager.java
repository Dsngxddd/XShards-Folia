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
    private final File afkLocationFile;
    private final File playerLocationsFile;
    private FileConfiguration afkDataConfig;
    private Location afkLocation;
    private final Map<UUID, Long> afkStartTime;
    private FileConfiguration playerLocationsConfig;

    public AfkManager(Xshards plugin) {
        this.plugin = plugin;
        this.afkData = new HashMap<>();
        this.afkStartTime = new HashMap<>();
        this.afkDataFile = new File(plugin.getDataFolder(), "afkdata.yml");
        this.afkLocationFile = new File(plugin.getDataFolder(), "afkmod.yml");
        this.playerLocationsFile = new File(plugin.getDataFolder(), "playerlocations.yml");
        loadAfkData();
        loadAfkLocation();
        loadPlayerLocations();
    }

    private void loadPlayerLocations() {
        if (!playerLocationsFile.exists()) {
            try {
                playerLocationsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create player locations file: " + e.getMessage());
            }
        }
        playerLocationsConfig = YamlConfiguration.loadConfiguration(playerLocationsFile);
    }

    private void savePlayerLocation(Player player) {
        String path = player.getUniqueId().toString();
        Location loc = player.getLocation();
        
        playerLocationsConfig.set(path + ".world", loc.getWorld().getName());
        playerLocationsConfig.set(path + ".x", loc.getX());
        playerLocationsConfig.set(path + ".y", loc.getY());
        playerLocationsConfig.set(path + ".z", loc.getZ());
        playerLocationsConfig.set(path + ".yaw", loc.getYaw());
        playerLocationsConfig.set(path + ".pitch", loc.getPitch());
        
        try {
            playerLocationsConfig.save(playerLocationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player location: " + e.getMessage());
        }
    }

    private Location getStoredPlayerLocation(Player player) {
        String path = player.getUniqueId().toString();
        if (!playerLocationsConfig.contains(path)) {
            return null;
        }

        String worldName = playerLocationsConfig.getString(path + ".world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            return null;
        }

        double x = playerLocationsConfig.getDouble(path + ".x");
        double y = playerLocationsConfig.getDouble(path + ".y");
        double z = playerLocationsConfig.getDouble(path + ".z");
        float yaw = (float) playerLocationsConfig.getDouble(path + ".yaw");
        float pitch = (float) playerLocationsConfig.getDouble(path + ".pitch");

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

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

    public void removeAfkLocation() {
        afkLocation = null;
    }

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

    public void setAfkLocation(Player player) {
        afkLocation = player.getLocation();
        saveAfkLocationToConfig();
    }

    private void saveAfkLocationToConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("afkLocation.world", afkLocation.getWorld().getName());
        config.set("afkLocation.x", afkLocation.getX());
        config.set("afkLocation.y", afkLocation.getY());
        config.set("afkLocation.z", afkLocation.getZ());
        plugin.saveConfig();
    }

    public void setAfk(Player player) {
        if (!afkData.containsKey(player.getUniqueId())) {
            savePlayerLocation(player);
            teleportToAfk(player);
            afkStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    public Location getAfkLocation() {
        return afkLocation;
    }

    public Location getLastLocation(Player player) {
        Location storedLocation = getStoredPlayerLocation(player);
        return storedLocation != null ? storedLocation : player.getLocation();
    }

    public boolean isAfk(Player player) {
        return afkData.containsKey(player.getUniqueId());
    }

    public void setLastLocation(Player player, Location location) {
        AfkData data = afkData.getOrDefault(player.getUniqueId(), new AfkData());
        data.setLastLocation(location);
        afkData.put(player.getUniqueId(), data);
        saveAfkData();
    }

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

    public void removeAfkData(Player player) {
        afkData.remove(player.getUniqueId());
        afkStartTime.remove(player.getUniqueId());
        saveAfkData();
    }

    public void teleportToAfk(Player player) {
        if (afkLocation != null) {
            player.teleport(afkLocation);
            startAfkShardEarning(player);
        } else {
            player.sendMessage("AFK location is not set.");
        }
    }

    public void quitAfk(Player player) {
        Location lastLocation = getLastLocation(player);
        if (lastLocation != null) {
            player.teleport(lastLocation);
            removeAfkData(player);
            player.sendMessage("You have quit AFK mode.");
        } else {
            player.sendMessage("You were not in AFK mode.");
        }
    }

    private void startAfkShardEarning(Player player) {
        long afkEarnSeconds = plugin.getConfig().getLong("afkearnseconds", 30);
        int afkAmount = plugin.getConfig().getInt("afkamount", 1);

        new BukkitRunnable() {
            int countdown = (int) afkEarnSeconds;

            @Override
            public void run() {
                if (isAfk(player)) {
                    player.sendTitle("", ChatColor.LIGHT_PURPLE + "EARN SHARDS IN " + countdown, 7, 12, 7);

                    if (countdown <= 0) {
                        plugin.getShardManager().addShards(player, afkAmount);
                        player.sendMessage(ChatColor.GREEN + "You earned " + afkAmount + " shard(s) while AFK!");
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        countdown = (int) afkEarnSeconds;
                    } else {
                        countdown--;
                    }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public boolean hasPlayerMoved(Player player, Location initialLocation) {
        Location currentLocation = player.getLocation();
        return initialLocation.getX() != currentLocation.getX() ||
               initialLocation.getY() != currentLocation.getY() ||
               initialLocation.getZ() != currentLocation.getZ();
    }

    public Xshards getPlugin() {
        return this.plugin;
    }
}