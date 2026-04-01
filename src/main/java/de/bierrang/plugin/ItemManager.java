package de.bierrang.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;

public class ItemManager {

    private static NamespacedKey key;

    public static ItemStack getLagerTool(BierLager plugin) {
        if (key == null) key = new NamespacedKey(plugin, "lager_tool_auth");
        
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Lager Werkzeug");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Rechtsklick auf Truhe:",
            ChatColor.YELLOW + " -> Verbindungen erstellen",
            ChatColor.YELLOW + " -> Filter einstellen",
            ChatColor.DARK_GRAY + "BierLager v3.2.1"
        ));
        
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public static boolean isRealLagerTool(ItemStack item, BierLager plugin) {
        if (item == null || !item.hasItemMeta()) return false;
        if (key == null) key = new NamespacedKey(plugin, "lager_tool_auth");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
