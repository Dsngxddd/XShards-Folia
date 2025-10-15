package com.xshards;

import com.xshards.AfkManager;
import com.xshards.scheduler.SchedulerAdapter;
import com.xshards.utils.MessageManager;
import com.xshards.WorldGuardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

/**
 * Listener for AFK-related events
 */
public class AfkListener implements Listener {

    private final AfkManager afkManager;
    private final WorldGuardManager worldGuard;
    private final MessageManager messages;
    private final SchedulerAdapter scheduler;

    public AfkListener(AfkManager afkManager, WorldGuardManager worldGuard,
                       MessageManager messages, SchedulerAdapter scheduler) {
        this.afkManager = afkManager;
        this.worldGuard = worldGuard;
        this.messages = messages;
        this.scheduler = scheduler;
    }

    /**
     * Handle player join - remove AFK status if exists
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Schedule after login plugins process
        scheduler.runGlobalDelayed(() -> {
            if (afkManager.isAfk(player)) {
                afkManager.removeAfkData(player);
                messages.send(player, "afk.auto-stopped");
            }
        }, 20L); // 1 second delay
    }

    /**
     * Handle player quit - clean up AFK data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (afkManager.isAfk(player) || afkManager.isPendingAfk(player)) {
            afkManager.removeAfkData(player);
        }
    }

    /**
     * Handle player movement - check WorldGuard regions
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if WorldGuard is enabled
        if (!worldGuard.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        // Check if player moved between blocks
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        boolean wasInRegion = worldGuard.isInAfkRegion(player);

        // Small delay to check new location
        scheduler.runGlobalDelayed(() -> {
            if (!player.isOnline()) return;

            boolean isInRegion = worldGuard.isInAfkRegion(player);

            // Player entered AFK region
            if (!wasInRegion && isInRegion) {
                messages.sendAfkRegionEntered(player);

                // Auto-start if enabled
                if (worldGuard.isAutoStartEnabled() && !afkManager.isAfk(player)) {
                    scheduler.runGlobalDelayed(() -> {
                        if (player.isOnline() && worldGuard.isInAfkRegion(player)) {
                            afkManager.startAfkProcess(player);
                        }
                    }, 40L); // 2 second delay
                }
            }

            // Player left AFK region
            if (wasInRegion && !isInRegion) {
                messages.sendAfkRegionLeft(player);

                // Auto-stop if enabled
                if (worldGuard.isAutoStopEnabled() && afkManager.isAfk(player)) {
                    afkManager.quitAfk(player);
                }
            }
        }, 5L); // 250ms delay
    }

    /**
     * Block commands while in AFK mode
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Allow only /quitafk command while AFK
        if (afkManager.isAfk(player) && !command.startsWith("/quitafk")) {
            event.setCancelled(true);
            messages.send(player, "afk.not-afk");
        }
    }

    /**
     * Block interactions while in AFK mode
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (afkManager.isAfk(player)) {
            event.setCancelled(true);
            messages.send(player, "afk.not-afk");
        }
    }

    /**
     * Prevent damage to AFK players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (afkManager.isAfk(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent AFK players from dealing damage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            if (afkManager.isAfk(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handle player teleport events
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Cancel pending AFK if player teleports
        if (afkManager.isPendingAfk(player)) {
            afkManager.cancelAfkProcess(player);
            messages.sendAfkCancelledMovement(player);
        }
    }
}