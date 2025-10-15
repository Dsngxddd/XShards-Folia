package com.xshards;

import com.xshards.Xshards;
import com.xshards.utils.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Main plugin command for admin functions
 */
public class XshardsCommand implements CommandExecutor {

    private final Xshards plugin;
    private final MessageManager messages;

    public XshardsCommand(Xshards plugin, MessageManager messages) {
        this.plugin = plugin;
        this.messages = messages;
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
                    messages.sendNoPermission(sender);
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(messages.getPrefix() + ChatColor.GREEN + "Yapılandırma yeniden yüklendi!");
                break;

            case "help":
                sendHelp(sender);
                break;

            case "version":
            case "ver":
                sendVersion(sender);
                break;

            default:
                sender.sendMessage(messages.getPrefix() + ChatColor.RED + "Bilinmeyen komut. /xshards help kullan.");
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "═══════════ XShards Yardım ═══════════");

        // Genel komutlar
        sender.sendMessage(ChatColor.YELLOW + "Genel Komutlar:");
        sender.sendMessage(ChatColor.YELLOW + "/shards " + ChatColor.WHITE + "- Shard bakiyeni kontrol et");
        sender.sendMessage(ChatColor.YELLOW + "/store " + ChatColor.WHITE + "- Shard mağazasını aç");
        sender.sendMessage(ChatColor.YELLOW + "/afk " + ChatColor.WHITE + "- AFK moduna gir");
        sender.sendMessage(ChatColor.YELLOW + "/quitafk " + ChatColor.WHITE + "- AFK modundan çık");

        // Mağaza komutları
        if (sender.hasPermission("xshards.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "\nMağaza Komutları:");
            sender.sendMessage(ChatColor.YELLOW + "/store edit <slot> <fiyat> " + ChatColor.WHITE + "- Eşya fiyatını düzenle");
            sender.sendMessage(ChatColor.YELLOW + "/store add <slot> <fiyat> " + ChatColor.WHITE + "- Eldeki eşyayı mağazaya ekle");
            sender.sendMessage(ChatColor.YELLOW + "/store remove <slot> " + ChatColor.WHITE + "- Eşyayı mağazadan kaldır");
        }

        // Admin komutları
        if (sender.hasPermission("xshards.admin")) {
            sender.sendMessage(ChatColor.GOLD + "\nAdmin Komutları:");
            sender.sendMessage(ChatColor.YELLOW + "/setafk " + ChatColor.WHITE + "- AFK konumunu ayarla");
            sender.sendMessage(ChatColor.YELLOW + "/afkremove " + ChatColor.WHITE + "- AFK konumunu kaldır");
            sender.sendMessage(ChatColor.YELLOW + "/xshards reload " + ChatColor.WHITE + "- Yapılandırmayı yeniden yükle");
            sender.sendMessage(ChatColor.YELLOW + "/xshards version " + ChatColor.WHITE + "- Eklenti sürümünü göster");
            sender.sendMessage(ChatColor.YELLOW + "/shards give <oyuncu> <miktar> " + ChatColor.WHITE + "- Oyuncuya shard ver");
        }

        // Kazanma yöntemleri
        sender.sendMessage(ChatColor.GOLD + "\nShard Kazanma Yöntemleri:");
        sender.sendMessage(ChatColor.WHITE + "• Oyunda Kalarak: Çevrimiçi kalarak shard kazan");
        sender.sendMessage(ChatColor.WHITE + "• PvP: Oyuncu öldürerek shard kazan");
        sender.sendMessage(ChatColor.WHITE + "• AFK: AFK modunda iken shard kazan");

        // Sistem bilgisi
        sender.sendMessage(ChatColor.GOLD + "\nSistem Bilgisi:");
        sender.sendMessage(ChatColor.WHITE + "• Veritabanı: " + ChatColor.YELLOW +
                plugin.getDatabaseManager().getStorageType().toUpperCase());
        sender.sendMessage(ChatColor.WHITE + "• Folia Desteği: " +
                (plugin.getScheduler().isFolia() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        sender.sendMessage(ChatColor.WHITE + "• WorldGuard: " +
                (plugin.getWorldGuardManager().isEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        sender.sendMessage(ChatColor.WHITE + "• Cross-Server: " +
                (plugin.getProxyManager().isEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));

        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
    }

    private void sendVersion(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
        sender.sendMessage(ChatColor.DARK_PURPLE + "  XShards " + ChatColor.LIGHT_PURPLE + "v2.0.0");
        sender.sendMessage(ChatColor.GRAY + "  Geliştirici: " + ChatColor.WHITE + "Akar1881");
        sender.sendMessage(ChatColor.GRAY + "  Sürüm: " + ChatColor.WHITE + "2.0.0-SNAPSHOT");
        sender.sendMessage(ChatColor.GRAY + "  Özellikler:");
        sender.sendMessage(ChatColor.WHITE + "    • Folia Desteği");
        sender.sendMessage(ChatColor.WHITE + "    • WorldGuard Entegrasyonu");
        sender.sendMessage(ChatColor.WHITE + "    • Cross-Server (BungeeCord/Velocity)");
        sender.sendMessage(ChatColor.WHITE + "    • MySQL & SQLite");
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════");
    }
}