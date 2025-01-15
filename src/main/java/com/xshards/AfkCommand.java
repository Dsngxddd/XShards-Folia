package com.xshards;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

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

        // Check if AFK earning is enabled
        if (!afkManager.getPlugin().getConfig().getBoolean("earning.afk.enabled", true)) {
            player.sendMessage(ChatColor.RED + "AFK mode is currently disabled.");
            return true;
        }

        if (afkManager.isAfk(player)) {
            player.sendMessage(ChatColor.RED + "You are already in AFK mode. Use /quitafk to exit.");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "You will be teleported to the AFK location in 5 seconds. Don't move!");

        Location initialLocation = player.getLocation();

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendTitle(ChatColor.GREEN + "Teleporting in " + countdown + " seconds", 
                                   ChatColor.YELLOW + "Don't move!", 10, 20, 10);
                    countdown--;
                } else {
                    if (afkManager.hasPlayerMoved(player, initialLocation)) {
                        player.sendMessage(ChatColor.RED + "Teleportation canceled because you moved.");
                        cancel();
                    } else {
                        if (afkManager.getAfkLocation() != null) {
                            afkManager.setAfk(player);
                            player.sendMessage(ChatColor.GREEN + "You are now in AFK mode.");
                        } else {
                            player.sendMessage(ChatColor.RED + "AFK location is not set. Please set it using /setafk.");
                        }
                        cancel();
                    }
                }
            }
        }.runTaskTimer(afkManager.getPlugin(), 0, 20);

        return true;
    }
}