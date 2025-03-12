package com.xshards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AfkListener implements Listener {
    private final AfkManager afkManager;

    public AfkListener(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Allow only /quitafk command while AFK
        if (afkManager.isAfk(player) && !command.equals("/quitafk")) {
            player.sendMessage("You can't use commands while in AFK mode. Use /quitafk to exit AFK mode.");
            event.setCancelled(true); // Cancel other commands
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (afkManager.isAfk(player)) {
            afkManager.removeAfkData(player); // Clean up AFK data on quit
        }
    }
}