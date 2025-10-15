package com.xshards;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.logging.Level;

/**
 * Manages database connections and operations
 */
public class DatabaseManager {

    private final Plugin plugin;
    private Connection connection;
    private final String storageType;

    // MySQL settings
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    // SQLite settings
    private String sqliteFile;

    private boolean connected = false;
    private boolean connectionLogged = false;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
        this.storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        connect();
        createTables();
    }

    /**
     * Load configuration
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();

        // MySQL settings
        host = config.getString("storage.mysql.host", "localhost");
        port = config.getInt("storage.mysql.port", 3306);
        database = config.getString("storage.mysql.database", "xshards");
        username = config.getString("storage.mysql.user", "root");
        password = config.getString("storage.mysql.password", "password");

        // SQLite settings
        sqliteFile = config.getString("storage.sqlite.file", "plugins/XShards/storage/xshards.db");
    }

    /**
     * Connect to database
     */
    public void connect() {
        try {
            if (storageType.equals("mysql")) {
                connectToMySQL();
            } else {
                connectToSQLite();
            }
            connected = true;

            if (!connectionLogged) {
                plugin.getLogger().info("Successfully connected to " + storageType.toUpperCase() + " database");
                connectionLogged = true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Connect to MySQL
     */
    private void connectToMySQL() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8";
                connection = DriverManager.getConnection(url, username, password);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC driver not found!");
            }
        }
    }

    /**
     * Connect to SQLite
     */
    private void connectToSQLite() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            try {
                Class.forName("org.sqlite.JDBC");

                // Ensure directory exists
                File dbFile = new File(sqliteFile);
                File parentDir = dbFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);

                // Enable foreign keys
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON;");
                }
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found!");
            }
        }
    }

    /**
     * Get database connection
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
            return connection;
        }

        // Test connection validity
        try {
            if (!connection.isValid(1)) {
                plugin.getLogger().info("Database connection lost. Reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            connect();
        }

        return connection;
    }

    /**
     * Create all required tables
     */
    private void createTables() {
        try {
            if (storageType.equals("mysql")) {
                createMySQLTables();
            } else {
                createSQLiteTables();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create MySQL tables
     */
    private void createMySQLTables() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            // Player shards table
            stmt.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "shards INT NOT NULL DEFAULT 0, " +
                    "INDEX idx_shards (shards)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Shop items table
            stmt.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "slot INT PRIMARY KEY, " +
                    "item_data MEDIUMBLOB NOT NULL, " +
                    "price DOUBLE NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // AFK location table
            stmt.execute("CREATE TABLE IF NOT EXISTS afk_location (" +
                    "id INT PRIMARY KEY, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Player locations table
            stmt.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // AFK status table
            stmt.execute("CREATE TABLE IF NOT EXISTS afk_status (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "is_afk BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "start_time BIGINT, " +
                    "INDEX idx_afk (is_afk)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    /**
     * Create SQLite tables
     */
    private void createSQLiteTables() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            // Player shards table
            stmt.execute("CREATE TABLE IF NOT EXISTS player_shards (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "player_name TEXT NOT NULL, " +
                    "shards INTEGER NOT NULL DEFAULT 0" +
                    ")");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shards ON player_shards(shards)");

            // Shop items table
            stmt.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "slot INTEGER PRIMARY KEY, " +
                    "item_data BLOB NOT NULL, " +
                    "price REAL NOT NULL" +
                    ")");

            // AFK location table
            stmt.execute("CREATE TABLE IF NOT EXISTS afk_location (" +
                    "id INTEGER PRIMARY KEY, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL" +
                    ")");

            // Player locations table
            stmt.execute("CREATE TABLE IF NOT EXISTS player_locations (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL" +
                    ")");

            // AFK status table
            stmt.execute("CREATE TABLE IF NOT EXISTS afk_status (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "is_afk INTEGER NOT NULL DEFAULT 0, " +
                    "start_time INTEGER" +
                    ")");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_afk ON afk_status(is_afk)");
        }
    }

    /**
     * Serialize ItemStack to bytes
     */
    public static byte[] serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream);
            dataOutput.writeObject(item.serialize());
            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to serialize ItemStack: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Deserialize ItemStack from bytes
     */
    public static ItemStack deserializeItemStack(byte[] data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            ObjectInputStream dataInput = new ObjectInputStream(inputStream);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> serializedItem =
                    (java.util.Map<String, Object>) dataInput.readObject();
            dataInput.close();
            return ItemStack.deserialize(serializedItem);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to deserialize ItemStack: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connected = false;
                plugin.getLogger().info("Database connection closed");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing database connection", e);
        }
    }

    /**
     * Get storage type
     */
    public String getStorageType() {
        return storageType;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected;
    }
}