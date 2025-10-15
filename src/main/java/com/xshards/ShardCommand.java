package com.xshards;

import com.xshards.ShardManager;
import com.xshards.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for checking and managing shards
 */
public class ShardCommand implements CommandExecutor {

    private final ShardManager shardManager;
    private final MessageManager messages;

    public ShardCommand(ShardManager shardManager, MessageManager messages) {
        this.shardManager = shardManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle console commands
        if (!(sender instanceof Player)) {
            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                return handleGiveCommand(sender, args[1], args[2]);
            } else {
                messages.sendConsoleUsage(sender);
                return true;
            }
        }

        Player player = (Player) sender;

        // No args - show balance
        if (args.length == 0) {
            int shards = shardManager.getShards(player);
            messages.sendShardsBalance(player, shards);
            return true;
        }

        // Give command
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("xshards.admin")) {
                messages.sendNoPermission(player);
                return true;
            }
            return handleGiveCommand(player, args[1], args[2]);
        }

        // Invalid usage
        player.sendMessage(messages.getPrefix() + "§cKullanım: /shards veya /shards give <oyuncu> <miktar>");
        return true;
    }

    /**
     * Handle giving shards to a player
     */
    private boolean handleGiveCommand(CommandSender sender, String targetName, String amountStr) {
        // Find target player
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            messages.sendPlayerNotFound(sender);
            return true;
        }

        // Parse amount
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            messages.sendInvalidAmount(sender);
            return true;
        }

        // Give shards
        shardManager.addShards(target, amount);

        // Send messages
        String senderName = sender instanceof Player ? ((Player) sender).getName() : "Konsol";
        messages.sendShardsGiven(sender, amount, target.getName());
        messages.sendShardsReceived(target, amount, senderName);

        return true;
    }
}