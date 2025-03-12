package com.xshards;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class XshardsPlaceholder extends PlaceholderExpansion {
    private final ShardManager shardManager;

    public XshardsPlaceholder(ShardManager shardManager) {
        this.shardManager = shardManager;
        register(); // Register the placeholders
    }

    @Override
    public String getIdentifier() {
        return "xshards"; // The identifier used in the placeholders
    }

    @Override
    public String getAuthor() {
        return "Akar1881"; // Your name or the author's name
    }

    @Override
    public String getVersion() {
        return "1.0"; // Version of your placeholder expansion
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return ""; // Return empty string if the player is null
        }

        // Placeholder to get the player's shards
        if (identifier.equals("playershards")) {
            return String.valueOf(shardManager.getShards(player));
        }

        return null; // Placeholder not found
    }
}