package de.bierrang.plugin;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class ItemManager {

    private static final String ENDERCHEST_TEXTURE_ID =
            "361e5b21bf736b3eb9a259e71767102225cd71f195a549c5af26352a0f60e4b3";

    public static ItemStack getLagerTool(BierLager plugin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        try {
            profile.getTextures().setSkin(
                    new URL("https://textures.minecraft.net/texture/" + ENDERCHEST_TEXTURE_ID)
            );
        } catch (Exception ignored) {}

        meta.setPlayerProfile(profile);

        meta.setDisplayName(ChatColor.DARK_PURPLE + "Lager Werkzeug");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Rechtsklick auf Truhe:",
                ChatColor.YELLOW + " -> Verbinden",
                ChatColor.YELLOW + " -> Filter einstellen"
        ));

        meta.setPlaceableKeys(Collections.emptyList());
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(true);

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
