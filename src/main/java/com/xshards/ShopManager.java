package com.xshards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

public class ShopManager {
    private final Xshards plugin;
    private final Map<Integer, ShopItem> shopItems;
    private final DatabaseManager databaseManager;

    public ShopManager(Xshards plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        this.databaseManager = plugin.getDatabaseManager();
        loadShopData();
    }

    public void addItemToShop(int slot, ItemStack item, double price) {
        ItemStack shopItem = item.clone();
        ItemMeta meta = shopItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Price: " + ChatColor.LIGHT_PURPLE + price + "$ " + ChatColor.WHITE + "Shards");
            meta.setLore(lore);
            shopItem.setItemMeta(meta);
        }
        shopItems.put(slot, new ShopItem(item.clone(), price));
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
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM shop_items WHERE slot = ?")) {
            
            stmt.setInt(1, slot);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove shop item: " + e.getMessage());
        }
    }

    public ShopItem getItemInShop(int slot) {
        return shopItems.get(slot);
    }

    public void loadShopData() {
        shopItems.clear();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT slot, item_data, price FROM shop_items");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int slot = rs.getInt("slot");
                byte[] itemData = rs.getBytes("item_data");
                double price = rs.getDouble("price");
                
                ItemStack item = DatabaseManager.deserializeItemStack(itemData);
                if (item != null) {
                    shopItems.put(slot, new ShopItem(item, price));
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load shop data: " + e.getMessage());
        }
    }

    public void saveShopData() {
        try (Connection conn = databaseManager.getConnection()) {
            String sql = databaseManager.getStorageType().equals("mysql")
                ? "INSERT INTO shop_items (slot, item_data, price) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE item_data = VALUES(item_data), price = VALUES(price)"
                : "INSERT OR REPLACE INTO shop_items (slot, item_data, price) VALUES (?, ?, ?)";
                
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
                    stmt.setInt(1, entry.getKey());
                    stmt.setBytes(2, DatabaseManager.serializeItemStack(entry.getValue().getItem()));
                    stmt.setDouble(3, entry.getValue().getPrice());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shop data: " + e.getMessage());
        }
    }

    public void openShopGUI(Player player) {
        int size = plugin.getConfig().getInt("store.size", 54);
        size = Math.min(54, Math.max(9, (size / 9) * 9));
        
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
                    lore.add(ChatColor.WHITE + "Price: " + ChatColor.LIGHT_PURPLE + shopItem.getPrice() + "$ " + ChatColor.WHITE + "Shards");
                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                }
                shopInventory.setItem(slot, displayItem);
            }
        }
        player.openInventory(shopInventory);
    }
}