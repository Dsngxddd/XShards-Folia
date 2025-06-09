package com.xshards;

import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.ChatColor;

public class AfkCommand implements CommandExecutor {
    private final AfkManager afkManager;

    public AfkCommand(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Check if player has permission
        if (!player.hasPermission("xshards.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Check if AFK earning is enabled
        if (!afkManager.getPlugin().getConfig().getBoolean("earning.afk.enabled", true)) {
            player.sendMessage(ChatColor.RED + "AFK mode is currently disabled.");
            return true;
        }

        if (afkManager.isAfk(player)) {
            player.sendMessage(ChatColor.RED + "You are already in AFK mode. Use /quitafk to exit.");
            return true;
        }
        
        if (afkManager.isPendingAfk(player)) {
            player.sendMessage(ChatColor.RED + "You are already in the process of entering AFK mode.");
            return true;
        }

        // Start the AFK process with the improved system
        afkManager.startAfkProcess(player);
        
        return true;
    }
}