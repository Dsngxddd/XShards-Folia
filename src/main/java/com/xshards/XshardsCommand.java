package com.xshards;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XshardsCommand implements CommandExecutor {
    private final Xshards plugin;

    public XshardsCommand(Xshards plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("xshards.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                plugin.reloadPlugin(); // Use the reloadPlugin method which handles database reconnection
                sender.sendMessage(ChatColor.GREEN + "Xshards configuration reloaded!");
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Use /xshards help for commands.");
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== Xshards Help ==========");
        
        // General Commands
        sender.sendMessage(ChatColor.YELLOW + "General Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/shards " + ChatColor.WHITE + "- Check your shard balance");
        sender.sendMessage(ChatColor.YELLOW + "/store " + ChatColor.WHITE + "- Open the shard shop");
        sender.sendMessage(ChatColor.YELLOW + "/afk " + ChatColor.WHITE + "- Enter AFK mode");
        sender.sendMessage(ChatColor.YELLOW + "/quitafk " + ChatColor.WHITE + "- Exit AFK mode");

        // Shop Commands
        sender.sendMessage(ChatColor.YELLOW + "\nShop Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/store edit <slot> <price> " + ChatColor.WHITE + "- Edit item price");
        sender.sendMessage(ChatColor.YELLOW + "/store add <slot> <price> " + ChatColor.WHITE + "- Add held item to shop");
        sender.sendMessage(ChatColor.YELLOW + "/store remove <slot> " + ChatColor.WHITE + "- Remove item from shop");

        // Admin Commands
        if (sender.hasPermission("xshards.admin")) {
            sender.sendMessage(ChatColor.GOLD + "\nAdmin Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/setafk " + ChatColor.WHITE + "- Set AFK location");
            sender.sendMessage(ChatColor.YELLOW + "/afkremove " + ChatColor.WHITE + "- Remove AFK location");
            sender.sendMessage(ChatColor.YELLOW + "/xshards reload " + ChatColor.WHITE + "- Reload configuration");
            sender.sendMessage(ChatColor.YELLOW + "/shards give <player> <amount> " + ChatColor.WHITE + "- Give shards to player");
        }

        // Earning Methods Info
        sender.sendMessage(ChatColor.GOLD + "\nEarning Methods:");
        sender.sendMessage(ChatColor.WHITE + "• Playtime: Earn shards by staying online");
        sender.sendMessage(ChatColor.WHITE + "• PvP: Earn shards from player kills");
        sender.sendMessage(ChatColor.WHITE + "• AFK: Earn shards while in AFK mode");
        
        // Storage Info
        sender.sendMessage(ChatColor.GOLD + "\nStorage System:");
        sender.sendMessage(ChatColor.WHITE + "• Current storage: " + 
            ChatColor.YELLOW + plugin.getDatabaseManager().getStorageType().toUpperCase());
        sender.sendMessage(ChatColor.WHITE + "• Configure in config.yml to use SQLite or MySQL");

        sender.sendMessage(ChatColor.GOLD + "================================");
    }
}