package com.xshards;

import com.xshards.AfkManager;
import com.xshards.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to set AFK location
 */
class SetAfkCommand implements CommandExecutor {

    private final AfkManager afkManager;
    private final MessageManager messages;

    public SetAfkCommand(AfkManager afkManager, MessageManager messages) {
        this.afkManager = afkManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Must be a player
        if (!(sender instanceof Player)) {
            messages.sendPlayerOnly(sender);
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("xshards.admin")) {
            messages.sendNoPermission(player);
            return true;
        }

        // Set AFK location
        afkManager.setAfkLocation(player);

        return true;
    }
}

/**
 * Command to remove AFK location
 */
class AfkRemoveCommand implements CommandExecutor {

    private final AfkManager afkManager;
    private final MessageManager messages;

    public AfkRemoveCommand(AfkManager afkManager, MessageManager messages) {
        this.afkManager = afkManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("xshards.admin")) {
                messages.sendNoPermission(player);
                return true;
            }
        }

        // Remove AFK location
        afkManager.removeAfkLocation();
        messages.send(sender, "admin.location-removed");

        return true;
    }
}