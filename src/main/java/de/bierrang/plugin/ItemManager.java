package de.bierrang.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.Arrays;
import java.util.UUID;

public class ItemManager {

    private static NamespacedKey key;
    // EnderChest Textur für Player Head
    private static final String ENDER_CHEST_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYxZTViMjFiZjczNmIzZWI5YTI1OWU3MTc2NzEwMjIyNWNkNzFmMTk1YTU0OWM1YWYyNjM1MmEwZjYwZTRiMyJ9fX0=";

    public static ItemStack getLagerTool(BierLager plugin) {
        if (key == null) key = new NamespacedKey(plugin, "lager_tool_auth");
        
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        // Textur setzen (Paper API)
        PlayerProfile profile = plugin.getServer().createProfile(UUID.randomUUID(), null);
        profile.setProperty(new ProfileProperty("textures", ENDER_CHEST_TEXTURE));
        meta.setPlayerProfile(profile);
        
        meta.setDisplayName("§dLager Werkzeug");
        meta.setLore(Arrays.asList(
            "§7Rechtsklick auf Truhe:",
            "§e -> Verbindungen erstellen",
            "§e -> Filter einstellen"
        ));
        
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
