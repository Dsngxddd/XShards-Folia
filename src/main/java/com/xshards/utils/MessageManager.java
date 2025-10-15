package com.xshards.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin messages with color code support
 */
public class MessageManager {

    private final FileConfiguration config;
    private final String prefix;

    public MessageManager(FileConfiguration config) {
        this.config = config;
        this.prefix = translateColors(config.getString("messages.prefix", "&8[&dXShards&8]&r "));
    }

    /**
     * Translate color codes
     */
    private String translateColors(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Get a message from config with placeholders
     */
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = config.getString("messages." + path, path);
        message = translateColors(message);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    /**
     * Get a message from config without placeholders
     */
    public String getMessage(String path) {
        return getMessage(path, null);
    }

    /**
     * Send a message to a player with prefix
     */
    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        sender.sendMessage(prefix + message);
    }

    /**
     * Send a message to a player with prefix
     */
    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    /**
     * Send a message without prefix
     */
    public void sendRaw(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getMessage(path, placeholders);
        sender.sendMessage(message);
    }

    /**
     * Send a message without prefix
     */
    public void sendRaw(CommandSender sender, String path) {
        sendRaw(sender, path, null);
    }

    // Convenience methods for common messages

    public void sendAfkEntering(Player player, int seconds) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("seconds", String.valueOf(seconds));
        send(player, "afk.entering", placeholders);
    }

    public void sendAfkCancelledMovement(Player player) {
        send(player, "afk.cancelled-movement");
    }

    public void sendAfkStarted(Player player) {
        send(player, "afk.started");
    }

    public void sendAfkAlready(Player player) {
        send(player, "afk.already-afk");
    }

    public void sendAfkNotInMode(Player player) {
        send(player, "afk.not-afk");
    }

    public void sendAfkQuit(Player player) {
        send(player, "afk.quit");
    }

    public void sendAfkDisabled(Player player) {
        send(player, "afk.disabled");
    }

    public void sendAfkNoLocation(Player player) {
        send(player, "afk.no-location");
    }

    public void sendAfkEarned(Player player, int amount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        send(player, "afk.earned", placeholders);
    }

    public void sendAfkRegionRequired(Player player) {
        send(player, "afk.region-required");
    }

    public void sendAfkRegionEntered(Player player) {
        send(player, "afk.region-entered");
    }

    public void sendAfkRegionLeft(Player player) {
        send(player, "afk.region-left");
    }

    public void sendAfkAutoStarted(Player player) {
        send(player, "afk.auto-started");
    }

    public void sendAfkAutoStopped(Player player) {
        send(player, "afk.auto-stopped");
    }

    public void sendAdminLocationSet(Player player) {
        send(player, "admin.location-set");
        sendRaw(player, "admin.recommend-custom-world");
    }

    public void sendAdminLocationRemoved(Player player) {
        send(player, "admin.location-removed");
    }

    public void sendAdminNetherEndBlocked(Player player) {
        send(player, "admin.nether-end-blocked");
        sendRaw(player, "admin.recommend-custom-world");
    }

    public void sendNoPermission(CommandSender sender) {
        send(sender, "admin.no-permission");
    }

    public void sendShopNotEnough(Player player) {
        send(player, "shop.not-enough-shards");
    }

    public void sendShopPurchased(Player player, String item, double price) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", item);
        placeholders.put("price", String.valueOf(price));
        send(player, "shop.purchased", placeholders);
    }

    public void sendShopCancelled(Player player) {
        send(player, "shop.cancelled");
    }

    public void sendShardsBalance(Player player, int amount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        send(player, "shards.balance", placeholders);
    }

    public void sendShardsEarned(Player player, int amount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        send(player, "shards.earned", placeholders);
    }

    public void sendShardsReceived(Player player, int amount, String sender) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("sender", sender);
        send(player, "shards.received", placeholders);
    }

    public void sendShardsGiven(CommandSender sender, int amount, String playerName) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("player", playerName);
        send(sender, "shards.given", placeholders);
    }

    public void sendPlayerOnly(CommandSender sender) {
        send(sender, "errors.player-only");
    }

    public void sendPlayerNotFound(CommandSender sender) {
        send(sender, "errors.player-not-found");
    }

    public void sendInvalidAmount(CommandSender sender) {
        send(sender, "errors.invalid-amount");
    }

    public void sendConsoleUsage(CommandSender sender) {
        send(sender, "errors.console-usage");
    }

    /**
     * Get prefix
     */
    public String getPrefix() {
        return prefix;
    }
}