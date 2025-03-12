package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandExecutor;
import org.bukkit.ChatColor;

public class QuitAfkCommand implements CommandExecutor {
    private final AfkManager afkManager;

    public QuitAfkCommand(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ensure the command sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        // Check if the player is currently in AFK mode
        if (!afkManager.isAfk(player)) {
            player.sendMessage(ChatColor.RED + "You are not in AFK mode.");
            return true;
        }

        // Call the quit method from AfkManager
        afkManager.quitAfk(player);
        return true; // Return true to indicate successful command execution
    }
}