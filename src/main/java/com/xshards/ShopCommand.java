package com.xshards;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final ShopManager shopManager;

    public ShopCommand(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            shopManager.openShopGUI(player);
            return true;
        }

        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("edit") && player.hasPermission("xshards.admin")) {
                if (args.length != 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /store edit <slot> <price>");
                    return true;
                }
                try {
                    int slot = Integer.parseInt(args[1]);
                    double price = Double.parseDouble(args[2]);
                    
                    if (shopManager.getItemInShop(slot) == null) {
                        player.sendMessage(ChatColor.RED + "No item exists in slot " + slot);
                        return true;
                    }
                    
                    shopManager.editItemPrice(slot, price);
                    player.sendMessage(ChatColor.GREEN + "Price updated for item in slot " + slot + " to " + price + " shards.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid slot or price number.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("add") && player.hasPermission("xshards.admin")) {
                if (args.length != 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /store add <slot> <price>");
                    return true;
                }
                try {
                    int slot = Integer.parseInt(args[1]);
                    double price = Double.parseDouble(args[2]);

                    ItemStack item = player.getInventory().getItemInMainHand();
                    if (item.getType() == Material.AIR) {
                        player.sendMessage(ChatColor.RED + "You must hold an item to add to the shop.");
                        return true;
                    }

                    shopManager.addItemToShop(slot, item, price);
                    player.sendMessage(ChatColor.GREEN + "Item added to shop in slot " + slot + " for " + price + " shards.");
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid slot or price number.");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("remove") && player.hasPermission("xshards.admin")) {
                if (args.length != 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /store remove <slot>");
                    return true;
                }
                try {
                    int slot = Integer.parseInt(args[1]);
                    shopManager.removeItemFromShop(slot);
                    player.sendMessage(ChatColor.GREEN + "Item removed from slot " + slot);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid slot number.");
                }
                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Usage: /store or /store edit <slot> <price> or /store add <slot> <price> or /store remove <slot>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("xshards.admin")) {
                completions.addAll(Arrays.asList("edit", "add", "remove"));
            }
        } else if (args.length == 2) {
            if (sender.hasPermission("xshards.admin")) {
                // Suggest slots 0-53 for the second argument
                for (int i = 0; i < 54; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        } else if (args.length == 3) {
            if (sender.hasPermission("xshards.admin") && 
                (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("add"))) {
                // Suggest some common prices
                completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
            }
        }
        
        return completions;
    }
}