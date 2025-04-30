package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShardCommand implements CommandExecutor {
    private final ShardManager shardManager;

    public ShardCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle console commands
        if (!(sender instanceof Player)) {
            // Console can only use the give command
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                try {
                    int amount = Integer.parseInt(args[2]);
                    shardManager.addShards(targetPlayer, amount);
                    sender.sendMessage("You have given " + amount + " shards to " + targetPlayer.getName() + ".");
                    targetPlayer.sendMessage("You have received " + amount + " shards from Console!");
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid amount. Please enter a number.");
                }
                return true;
            } else {
                sender.sendMessage("Console usage: /shards give <player> <amount>");
                return true;
            }
        }

        // Handle player commands
        Player player = (Player) sender;

        // /shards command with no arguments: check the player's shard balance
        if (args.length == 0) {
            int playerShards = shardManager.getShards(player);
            player.sendMessage("You have " + playerShards + " shards.");
            return true;
        }

        // /shards give <player> <amount>
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && player.hasPermission("xshards.admin")) {
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                player.sendMessage("Player not found.");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[2]);
                shardManager.addShards(targetPlayer, amount);
                player.sendMessage("You have given " + amount + " shards to " + targetPlayer.getName() + ".");
                targetPlayer.sendMessage("You have received " + amount + " shards from " + player.getName() + "!");
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid amount. Please enter a number.");
            }
            return true;
        }

        player.sendMessage("Usage: /shards or /shards give <player> <amount>");
        return true;
    }
}