package com.xshards;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.xshards.Xshards;
import com.xshards.utils.MessageManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages cross-server connections for BungeeCord/Velocity
 */
public class ProxyManager {

    private final Xshards plugin;
    private final MessageManager messages;
    private final boolean enabled;
    private final List<String> afkServers;
    private final String fallbackServer;
    private final boolean returnToOrigin;

    // Track which server each player came from
    private final Map<UUID, String> originServers;

    // Round-robin counter for server selection
    private final AtomicInteger serverIndex;

    public ProxyManager(Xshards plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.enabled = plugin.getConfig().getBoolean("earning.afk.cross-server.enabled", false);
        this.afkServers = plugin.getConfig().getStringList("earning.afk.cross-server.servers");
        this.fallbackServer = plugin.getConfig().getString("earning.afk.cross-server.fallback-server", "lobby");
        this.returnToOrigin = plugin.getConfig().getBoolean("earning.afk.cross-server.return-to-origin", true);
        this.originServers = new HashMap<>();
        this.serverIndex = new AtomicInteger(0);

        if (enabled && !afkServers.isEmpty()) {
            // Register plugin messaging channel
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
            plugin.getLogger().info("Cross-server support enabled with " + afkServers.size() + " AFK servers");
        }
    }

    /**
     * Check if cross-server is enabled
     */
    public boolean isEnabled() {
        return enabled && !afkServers.isEmpty();
    }

    /**
     * Send player to an AFK server
     */
    public void sendToAfkServer(Player player) {
        if (!isEnabled()) {
            return;
        }

        // Save current server
        saveOriginServer(player);

        // Get next AFK server (round-robin)
        String targetServer = getNextAfkServer();

        // Send message
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("server", targetServer);
        messages.send(player, "afk.connecting-server", placeholders);

        // Connect to server
        connectToServer(player, targetServer);
    }

    /**
     * Return player to their origin server
     */
    public void returnToOriginServer(Player player) {
        if (!isEnabled() || !returnToOrigin) {
            return;
        }

        UUID uuid = player.getUniqueId();
        String origin = originServers.get(uuid);

        if (origin != null) {
            messages.send(player, "afk.returning-origin");
            connectToServer(player, origin);
            originServers.remove(uuid);
        }
    }

    /**
     * Get the next AFK server using round-robin
     */
    private String getNextAfkServer() {
        int index = serverIndex.getAndUpdate(i -> (i + 1) % afkServers.size());
        return afkServers.get(index);
    }

    /**
     * Save the player's current server
     */
    private void saveOriginServer(Player player) {
        // On BungeeCord/Velocity, we can't directly get the current server name
        // So we'll use a plugin message to request it
        // For now, we'll store a placeholder
        originServers.put(player.getUniqueId(), "unknown");

        // In a real implementation, you'd use:
        // ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // out.writeUTF("GetServer");
        // player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        // And handle the response in a PluginMessageListener
    }

    /**
     * Connect player to a specific server
     */
    private void connectToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to connect player to server: " + e.getMessage());
            messages.send(player, "afk.server-error");
        }
    }

    /**
     * Get fallback server
     */
    public String getFallbackServer() {
        return fallbackServer;
    }

    /**
     * Clean up origin server data for a player
     */
    public void removeOriginServer(Player player) {
        originServers.remove(player.getUniqueId());
    }

    /**
     * Unregister plugin channels
     */
    public void shutdown() {
        if (enabled) {
            plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, "BungeeCord");
        }
    }
}