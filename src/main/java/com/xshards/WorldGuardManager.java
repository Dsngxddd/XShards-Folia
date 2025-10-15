package com.xshards;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Manager for WorldGuard integration
 */
public class WorldGuardManager {

    private final Plugin plugin;
    private final boolean enabled;
    private String afkRegionName;

    public WorldGuardManager(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = checkWorldGuard();

        if (enabled) {
            this.afkRegionName = plugin.getConfig().getString("earning.afk.worldguard.region", "afk-zone");
            plugin.getLogger().info("WorldGuard integration enabled! AFK region: " + afkRegionName);
        } else {
            plugin.getLogger().info("WorldGuard not found - region features disabled");
        }
    }

    /**
     * Check if WorldGuard is available
     */
    private boolean checkWorldGuard() {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if WorldGuard integration is enabled
     */
    public boolean isEnabled() {
        return enabled && plugin.getConfig().getBoolean("earning.afk.worldguard.enabled", false);
    }

    /**
     * Check if player is in the AFK region
     */
    public boolean isInAfkRegion(Player player) {
        if (!isEnabled()) {
            return false;
        }

        try {
            Location location = player.getLocation();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

            if (regions == null) {
                return false;
            }

            ApplicableRegionSet set = regions.getApplicableRegions(BukkitAdapter.asBlockVector(location));

            for (ProtectedRegion region : set) {
                if (region.getId().equalsIgnoreCase(afkRegionName)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard region: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if region requirement is enabled
     */
    public boolean requiresRegion() {
        return isEnabled() && plugin.getConfig().getBoolean("earning.afk.worldguard.require-region", true);
    }

    /**
     * Check if auto-start is enabled
     */
    public boolean isAutoStartEnabled() {
        return isEnabled() && plugin.getConfig().getBoolean("earning.afk.worldguard.auto-start", true);
    }

    /**
     * Check if auto-stop is enabled
     */
    public boolean isAutoStopEnabled() {
        return isEnabled() && plugin.getConfig().getBoolean("earning.afk.worldguard.auto-stop", true);
    }

    /**
     * Get the AFK region name
     */
    public String getAfkRegionName() {
        return afkRegionName;
    }

    /**
     * Set the AFK region name
     */
    public void setAfkRegionName(String regionName) {
        this.afkRegionName = regionName;
        plugin.getConfig().set("earning.afk.worldguard.region", regionName);
        plugin.saveConfig();
    }
}