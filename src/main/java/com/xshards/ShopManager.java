package com.xshards;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

public class ShopManager {
    private final Xshards plugin;
    private final Map<Integer, ShopItem> shopItems;
    private final File shopDataFile;
    private FileConfiguration shopDataConfig;

    public ShopManager(Xshards plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        this.shopDataFile = new File(plugin.getDataFolder(), "shopdata.yml");
        loadShopData();
    }

    public void addItemToShop(int slot, ItemStack item, double price) {
        // Create a copy of the item to prevent modifying the original
        ItemStack shopItem = item.clone();
        ItemMeta meta = shopItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add("§ePrice: " + price + " Shards");
            meta.setLore(lore);
            shopItem.setItemMeta(meta);
        }
        shopItems.put(slot, new ShopItem(item.clone(), price)); // Store original item
        saveShopData();
    }

    public void editItemPrice(int slot, double newPrice) {
        ShopItem existingItem = shopItems.get(slot);
        if (existingItem != null) {
            shopItems.put(slot, new ShopItem(existingItem.getItem(), newPrice));
            saveShopData();
        }
    }

    public void removeItemFromShop(int slot) {
        shopItems.remove(slot);
        shopDataConfig.set(String.valueOf(slot), null);
        saveShopData();
    }

    public ShopItem getItemInShop(int slot) {
        return shopItems.get(slot);
    }

    public void loadShopData() {
        if (!shopDataFile.exists()) {
            plugin.saveResource("shopdata.yml", false);
        }
        shopDataConfig = YamlConfiguration.loadConfiguration(shopDataFile);
        
        for (String key : shopDataConfig.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ItemStack item = shopDataConfig.getItemStack(key + ".item");
                double price = shopDataConfig.getDouble(key + ".price");
                if (item != null) {
                    shopItems.put(slot, new ShopItem(item, price));
                }
            } catch (NumberFormatException ignored) {
                // Skip invalid entries
            }
        }
    }

    public void saveShopData() {
        shopDataConfig = new YamlConfiguration();
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            shopDataConfig.set(entry.getKey() + ".item", entry.getValue().getItem());
            shopDataConfig.set(entry.getKey() + ".price", entry.getValue().getPrice());
        }
        try {
            shopDataConfig.save(shopDataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shop data: " + e.getMessage());
        }
    }

    public void openShopGUI(Player player) {
        int size = plugin.getConfig().getInt("store.size", 54);
        size = Math.min(54, Math.max(9, (size / 9) * 9)); // Ensure valid size
        
        org.bukkit.inventory.Inventory shopInventory = Bukkit.createInventory(null, size, "Shard Shop");
        
        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            int slot = entry.getKey();
            if (slot < size) {
                ShopItem shopItem = entry.getValue();
                ItemStack displayItem = shopItem.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add("§ePrice: " + shopItem.getPrice() + " Shards");
                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                }
                shopInventory.setItem(slot, displayItem);
            }
        }
        player.openInventory(shopInventory);
    }
}