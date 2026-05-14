package de.bierlager.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemUtil {

    public static final NamespacedKey BIERLAGER_KEY = new NamespacedKey("bierlager", "bierlager_item");

    public static ItemStack createBierLagerItem() {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.text("BierLager", NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false)
                        .decoration(TextDecoration.BOLD, true)
        );

        meta.lore(List.of(
                Component.text("» Rechtsklick auf Truhe: Quelle", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("» Shift+Rechtsklick: Ziel & Filter", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("» Nur auf deinem eigenen Plot", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_ATTRIBUTES
        );
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(BIERLAGER_KEY, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBierLagerItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_CHEST) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(BIERLAGER_KEY, PersistentDataType.BYTE);
    }

    public static boolean playerHasBierLagerItem(org.bukkit.entity.Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isBierLagerItem(stack)) return true;
        }
        return false;
    }
}
