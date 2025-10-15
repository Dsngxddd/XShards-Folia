package com.xshards;

import com.xshards.Xshards;
import com.xshards.ProxyManager;
import com.xshards.scheduler.SchedulerAdapter;
import com.xshards.utils.MessageManager;
import com.xshards.WorldGuardManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages AFK system with WorldGuard integration, Folia support, and Cross-Server
 */
public class AfkManager {

    private final Xshards plugin;
    private final SchedulerAdapter scheduler;
    private final MessageManager messages;
    private final WorldGuardManager worldGuard;
    private final ProxyManager proxyManager;

    // AFK location (legacy mode)
    private Location afkLocation;

    // Active AFK players
    private final Map<UUID, AfkSession> activeSessions;

    // Pending teleports (countdown active)
    private final Map<UUID, PendingTeleport> pendingTeleports;

    // Boss bars for countdowns
    private final Map<UUID, BossBar> countdownBars;

    public AfkManager(Xshards plugin, SchedulerAdapter scheduler, MessageManager messages,
                      WorldGuardManager worldGuard, ProxyManager proxyManager) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.messages = messages;
        this.worldGuard = worldGuard;
        this.proxyManager = proxyManager;
        this.activeSessions = new ConcurrentHashMap<>();
        this.pendingTeleports = new ConcurrentHashMap<>();
        this.countdownBars = new ConcurrentHashMap<>();

        loadAfkLocation();
        loadActiveSessions();
    }

    /**
     * Load AFK location from database
     */
    private void loadAfkLocation() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT world, x, y, z FROM afk_location WHERE id = 1")) {

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String worldName = rs.getString("world");
                World world = plugin.getServer().getWorld(worldName);

                if (world != null) {
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    afkLocation = new Location(world, x, y, z);
                    plugin.getLogger().info("Loaded AFK location: " + worldName);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load AFK location: " + e.getMessage());
        }
    }

    /**
     * Load active AFK sessions from database
     */
    private void loadActiveSessions() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT uuid, start_time FROM afk_status WHERE is_afk = 1 OR is_afk = TRUE")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long startTime = rs.getLong("start_time");

                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    activeSessions.put(uuid, new AfkSession(startTime));
                }
            }

            plugin.getLogger().info("Loaded " + activeSessions.size() + " active AFK sessions");
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load AFK sessions: " + e.getMessage());
        }
    }

    /**
     * Set AFK location
     */
    public void setAfkLocation(Player player) {
        Location loc = player.getLocation();

        // Check for Nether/End
        if (loc.getWorld().getEnvironment() == World.Environment.NETHER ||
                loc.getWorld().getEnvironment() == World.Environment.THE_END) {
            messages.sendAdminNetherEndBlocked(player);
            return;
        }

        this.afkLocation = loc.clone();

        // Save to database
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = plugin.getDatabaseManager().getStorageType().equals("mysql")
                    ? "INSERT INTO afk_location (id, world, x, y, z) VALUES (1, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z)"
                    : "INSERT OR REPLACE INTO afk_location (id, world, x, y, z) VALUES (1, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, loc.getWorld().getName());
                stmt.setDouble(2, loc.getX());
                stmt.setDouble(3, loc.getY());
                stmt.setDouble(4, loc.getZ());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save AFK location: " + e.getMessage());
        }

        messages.sendAdminLocationSet(player);
    }

    /**
     * Remove AFK location
     */
    public void removeAfkLocation() {
        this.afkLocation = null;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM afk_location WHERE id = 1")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove AFK location: " + e.getMessage());
        }
    }

    /**
     * Start AFK process for a player
     */
    public void startAfkProcess(Player player) {
        UUID uuid = player.getUniqueId();

        // Check if already AFK
        if (isAfk(player)) {
            messages.sendAfkAlready(player);
            return;
        }

        // Check if already pending
        if (isPendingAfk(player)) {
            messages.send(player, "afk.cancelled-movement");
            return;
        }

        // Check WorldGuard region requirement
        if (worldGuard.requiresRegion() && !worldGuard.isInAfkRegion(player)) {
            messages.sendAfkRegionRequired(player);
            return;
        }

        // Check if AFK location is set (legacy mode)
        if (!worldGuard.requiresRegion() && afkLocation == null) {
            messages.sendAfkNoLocation(player);
            return;
        }

        // Get countdown delay
        int delay = plugin.getConfig().getInt("earning.afk.teleport-delay", 5);

        // Save current location
        Location startLocation = player.getLocation().clone();

        // Create pending teleport
        PendingTeleport pending = new PendingTeleport(startLocation, delay);
        pendingTeleports.put(uuid, pending);

        // Create boss bar with Turkish message
        BossBar bossBar = plugin.getServer().createBossBar(
                "§6AFK'ye §e" + delay + " §6saniye...",
                BarColor.YELLOW,
                BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        countdownBars.put(uuid, bossBar);

        // Start countdown
        messages.sendAfkEntering(player, delay);
        startCountdown(player, pending, bossBar);
    }

    /**
     * Start countdown timer
     */
    private void startCountdown(Player player, PendingTeleport pending, BossBar bossBar) {
        UUID uuid = player.getUniqueId();
        int totalDelay = plugin.getConfig().getInt("earning.afk.teleport-delay", 5);

        scheduler.runGlobalTimer(new Runnable() {
            int remaining = totalDelay;

            @Override
            public void run() {
                // Check if still pending
                if (!pendingTeleports.containsKey(uuid) || !player.isOnline()) {
                    cleanup(uuid);
                    return;
                }

                // Check if player moved
                if (hasPlayerMoved(player, pending.getStartLocation())) {
                    messages.sendAfkCancelledMovement(player);
                    cancelAfkProcess(player);
                    return;
                }

                // Update boss bar with Turkish message
                double progress = (double) remaining / totalDelay;
                bossBar.setProgress(Math.max(0, Math.min(1, progress)));
                bossBar.setTitle("§6AFK'ye §e" + remaining + " §6saniye...");

                if (remaining <= 0) {
                    // Complete teleport
                    completeAfkProcess(player, pending);
                    return;
                }

                remaining--;
            }
        }, 0L, 20L); // Every second
    }

    /**
     * Complete AFK process and teleport player
     */
    private void completeAfkProcess(Player player, PendingTeleport pending) {
        UUID uuid = player.getUniqueId();

        // Clean up countdown
        cleanup(uuid);

        // Save player's location to database
        savePlayerLocation(player, pending.getStartLocation());

        // Handle cross-server teleport
        if (proxyManager.isEnabled()) {
            // Send to AFK server
            proxyManager.sendToAfkServer(player);
        } else if (!worldGuard.requiresRegion() && afkLocation != null) {
            // Legacy teleport mode
            scheduler.runAtLocation(player.getLocation(), () -> {
                player.teleport(afkLocation);
            });
        }

        // Create AFK session
        AfkSession session = new AfkSession(System.currentTimeMillis());
        activeSessions.put(uuid, session);

        // Save to database
        saveAfkStatus(uuid, true, session.getStartTime());

        // Send message
        messages.sendAfkStarted(player);

        // Start earning task
        startEarningTask(player);
    }

    /**
     * Cancel AFK process
     */
    public void cancelAfkProcess(Player player) {
        UUID uuid = player.getUniqueId();
        pendingTeleports.remove(uuid);
        cleanup(uuid);
    }

    /**
     * Quit AFK mode
     */
    public void quitAfk(Player player) {
        UUID uuid = player.getUniqueId();

        // Check if in pending
        if (isPendingAfk(player)) {
            cancelAfkProcess(player);
            messages.send(player, "afk.cancelled-movement");
            return;
        }

        // Check if AFK
        if (!isAfk(player)) {
            messages.sendAfkNotInMode(player);
            return;
        }

        // Get saved location
        Location savedLocation = getSavedLocation(uuid);

        // Handle cross-server return
        if (proxyManager.isEnabled()) {
            proxyManager.returnToOriginServer(player);
        } else if (savedLocation != null && !worldGuard.requiresRegion()) {
            // Legacy teleport back
            scheduler.runAtLocation(player.getLocation(), () -> {
                player.teleport(savedLocation);
            });
        }

        // Remove session
        activeSessions.remove(uuid);
        saveAfkStatus(uuid, false, 0);

        messages.sendAfkQuit(player);
    }

    /**
     * Start earning task for AFK player
     */
    private void startEarningTask(Player player) {
        UUID uuid = player.getUniqueId();
        int interval = plugin.getConfig().getInt("earning.afk.interval", 30);
        int amount = plugin.getConfig().getInt("earning.afk.amount", 1);

        scheduler.runGlobalTimer(new Runnable() {
            int countdown = interval;

            @Override
            public void run() {
                // Check if still AFK and online
                if (!isAfk(player) || !player.isOnline()) {
                    return;
                }

                // Check WorldGuard region if required
                if (worldGuard.requiresRegion() && !worldGuard.isInAfkRegion(player)) {
                    return;
                }

                if (countdown <= 0) {
                    // Give shards
                    plugin.getShardManager().addShards(player, amount);
                    messages.sendAfkEarned(player, amount);

                    // Play sound
                    scheduler.runAtLocation(player.getLocation(), () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                    });

                    countdown = interval;
                } else {
                    countdown--;
                }
            }
        }, 0L, 20L);
    }

    /**
     * Check if player has moved
     */
    private boolean hasPlayerMoved(Player player, Location original) {
        Location current = player.getLocation();
        double tolerance = plugin.getConfig().getDouble("earning.afk.movement-tolerance", 0.1);
        return original.distanceSquared(current) > (tolerance * tolerance);
    }

    /**
     * Save player location to database
     */
    private void savePlayerLocation(Player player, Location location) {
        UUID uuid = player.getUniqueId();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            String sql = plugin.getDatabaseManager().getStorageType().equals("mysql")
                    ? "INSERT INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE world=VALUES(world), x=VALUES(x), y=VALUES(y), z=VALUES(z), yaw=VALUES(yaw), pitch=VALUES(pitch)"
                    : "INSERT OR REPLACE INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, location.getWorld().getName());
                stmt.setDouble(3, location.getX());
                stmt.setDouble(4, location.getY());
                stmt.setDouble(5, location.getZ());
                stmt.setFloat(6, location.getYaw());
                stmt.setFloat(7, location.getPitch());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save player location: " + e.getMessage());
        }
    }

    /**
     * Get saved player location
     */
    private Location getSavedLocation(UUID uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT world, x, y, z, yaw, pitch FROM player_locations WHERE uuid = ?")) {

            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String worldName = rs.getString("world");
                World world = plugin.getServer().getWorld(worldName);

                if (world != null) {
                    return new Location(
                            world,
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get saved location: " + e.getMessage());
        }

        return null;
    }

    /**
     * Save AFK status to database
     */
    private void saveAfkStatus(UUID uuid, boolean isAfk, long startTime) {
        scheduler.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                String sql = plugin.getDatabaseManager().getStorageType().equals("mysql")
                        ? "INSERT INTO afk_status (uuid, is_afk, start_time) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE is_afk=VALUES(is_afk), start_time=VALUES(start_time)"
                        : "INSERT OR REPLACE INTO afk_status (uuid, is_afk, start_time) VALUES (?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setBoolean(2, isAfk);
                    stmt.setLong(3, startTime);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save AFK status: " + e.getMessage());
            }
        });
    }

    /**
     * Cleanup countdown resources
     */
    private void cleanup(UUID uuid) {
        // Remove boss bar
        BossBar bar = countdownBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }

        // Remove pending teleport
        pendingTeleports.remove(uuid);
    }

    /**
     * Check if player is AFK
     */
    public boolean isAfk(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Check if player is pending AFK
     */
    public boolean isPendingAfk(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    /**
     * Get AFK location
     */
    public Location getAfkLocation() {
        return afkLocation;
    }

    /**
     * Remove AFK data for a player
     */
    public void removeAfkData(Player player) {
        UUID uuid = player.getUniqueId();
        activeSessions.remove(uuid);
        pendingTeleports.remove(uuid);
        cleanup(uuid);
        saveAfkStatus(uuid, false, 0);

        // Clean up proxy data
        if (proxyManager.isEnabled()) {
            proxyManager.removeOriginServer(player);
        }
    }

    /**
     * Get WorldGuard manager
     */
    public WorldGuardManager getWorldGuardManager() {
        return worldGuard;
    }

    /**
     * Get proxy manager
     */
    public ProxyManager getProxyManager() {
        return proxyManager;
    }

    /**
     * Get plugin instance
     */
    public Xshards getPlugin() {
        return plugin;
    }

    /**
     * Shutdown manager
     */
    public void shutdown() {
        // Remove all boss bars
        for (BossBar bar : countdownBars.values()) {
            bar.removeAll();
        }
        countdownBars.clear();

        // Clear sessions
        activeSessions.clear();
        pendingTeleports.clear();
    }
}

/**
 * Represents an active AFK session
 */
class AfkSession {
    private final long startTime;

    public AfkSession(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }
}

/**
 * Represents a pending AFK teleport
 */
class PendingTeleport {
    private final Location startLocation;
    private final int totalDelay;

    public PendingTeleport(Location startLocation, int totalDelay) {
        this.startLocation = startLocation;
        this.totalDelay = totalDelay;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public int getTotalDelay() {
        return totalDelay;
    }
}