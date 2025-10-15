package com.xshards;

import com.xshards.Xshards;
import com.xshards.AfkManager;
import com.xshards.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to enter AFK mode
 */
public class AfkCommand implements CommandExecutor {

    private final AfkManager afkManager;
    private final MessageManager messages;
    private final Xshards plugin;

    public AfkCommand(AfkManager afkManager, MessageManager messages) {
        this.afkManager = afkManager;
        this.messages = messages;
        this.plugin = (Xshards) afkManager.getPlugin();
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
        if (!player.hasPermission("xshards.use")) {
            messages.sendNoPermission(player);
            return true;
        }

        // Check if AFK is enabled
        if (!plugin.getConfig().getBoolean("earning.afk.enabled", true)) {
            messages.sendAfkDisabled(player);
            return true;
        }

        // Check if already AFK
        if (afkManager.isAfk(player)) {
            messages.sendAfkAlready(player);
            return true;
        }

        // Check if already pending
        if (afkManager.isPendingAfk(player)) {
            messages.send(player, "afk.cancelled-movement");
            return true;
        }

        // Start AFK process
        afkManager.startAfkProcess(player);

        return true;
    }
}