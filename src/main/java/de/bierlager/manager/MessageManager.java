package de.bierlager.manager;

import de.bierlager.BierLager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {

    private final BierLager plugin;
    private FileConfiguration messages;

    public MessageManager(BierLager plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String prefix = messages.getString("prefix", "§6[BierLager] §r");
        String value = messages.getString(key, "§c[Fehlende Nachricht: " + key + "]");
        return prefix + value;
    }

    public String getRaw(String key) {
        return messages.getString(key, "§c[Fehlende Nachricht: " + key + "]");
    }

    public void send(Player player, String key) {
        player.sendMessage(get(key));
    }

    public void sendReplaced(Player player, String key, String placeholder, String value) {
        player.sendMessage(get(key).replace(placeholder, value));
    }
}
