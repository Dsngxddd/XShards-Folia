package com.xshards.utils;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

/**
 * Utility class for sending action bar messages that works across different Bukkit API versions
 */
public class ActionBarUtil {
    
    /**
     * Initialize the action bar utility
     * This method is called when the plugin starts
     */
    public static void initialize() {
        // No initialization needed for this simplified version
    }

    /**
     * Send an action bar message to a player
     * This method uses the title API as a fallback for action bar messages
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        
        try {
            // Use the title API as a fallback for action bar messages
            // This works on all versions of Bukkit
            player.sendTitle("", message, 0, 20, 5);
        } catch (Exception e) {
            // If all else fails, just send a chat message
            player.sendMessage(message);
        }
    }
}