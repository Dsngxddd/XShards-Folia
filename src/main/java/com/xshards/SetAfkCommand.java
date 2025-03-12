package com.xshards;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class SetAfkCommand implements CommandExecutor {
    private final AfkManager afkManager;

    public SetAfkCommand(AfkManager afkManager) {
        this.afkManager = afkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set AFK location.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("xshards.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        // Set the AFK location to the player's current location
        afkManager.setAfkLocation(player);  // Call setAfkLocation without needing a Location parameter
        player.sendMessage(ChatColor.GREEN + "Your AFK location has been set to your current position!");

        return true; // Indicate the command was successful
    }
}