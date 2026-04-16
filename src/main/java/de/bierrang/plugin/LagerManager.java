package de.bierrang.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

public class ItemManager {

    private static final String ENDER_CHEST_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYxZTViMjFiZjczNmIzZWI5YTI1OWU3MTc2NzEwMjIyNWNkNzFmMTk1YTU0OWM1YWYyNjM1MmEwZjYwZTRiMyJ9fX0=";

    public static ItemStack getLagerTool(BierLager plugin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        // Paper API Profile Erstellung
        // Wir nutzen das Bukkit Profile Interface, Paper kümmert sich um den Rest.
        org.bukkit.profile.PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        
        try {
            // Textur setzen
            profile.getTextures().setSkin(new URL("https://textures.minecraft.net/texture/" + ENDER_CHEST_TEXTURE));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Setzen ohne Cast
        meta.setPlayerProfile(profile);
        
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Lager Werkzeug");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Rechtsklick auf Truhe:",
            ChatColor.YELLOW + " -> Verbinden",
            ChatColor.YELLOW + " -> Filter einstellen"
        ));
        
        NamespacedKey key = new NamespacedKey(plugin, "lager_tool_auth");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isLagerTool(ItemStack item, BierLager plugin) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "lager_tool_auth");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
