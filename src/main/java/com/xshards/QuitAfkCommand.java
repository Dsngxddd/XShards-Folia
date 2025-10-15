package com.xshards;

import com.xshards.AfkManager;
import com.xshards.utils.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to exit AFK mode
 */
public class QuitAfkCommand implements CommandExecutor {

    private final AfkManager afkManager;
    private final MessageManager messages;

    public QuitAfkCommand(AfkManager afkManager, MessageManager messages) {
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

        // Quit AFK
        afkManager.quitAfk(player);

        return true;
    }
}