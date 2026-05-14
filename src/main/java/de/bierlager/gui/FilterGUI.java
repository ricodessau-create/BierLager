package de.bierlager.gui;

import de.bierlager.model.SortTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class FilterGUI {

    public static final String TITLE_BASE = "BierLager-Filter";
    public static final int PAGE_SIZE = 45;

    private static final List<Material> ALL_ITEMS = Arrays.stream(Material.values())
            .filter(m -> !m.isAir() && m.isItem())
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toList();

    public static Inventory open(SortTarget target, int page) {
        int totalPages = getTotalPages();
        Component title = Component.text(TITLE_BASE + " [" + (page + 1) + "/" + totalPages + "]");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, ALL_ITEMS.size());

        for (int i = start; i < end; i++) {
            Material mat = ALL_ITEMS.get(i);
            inv.setItem(i - start, buildMaterialItem(mat, target.hasFilter(mat)));
        }

        inv.setItem(45, buildControl(Material.BARRIER, "§cSchließen", "§7Klick zum Schließen"));
        inv.setItem(46, buildControl(Material.HOPPER, "§eFilter leeren", "§7Entfernt alle Filter"));
        inv.setItem(48, page > 0
                ? buildControl(Material.ARROW, "§eVorherige Seite", "§7Seite " + page)
                : buildFiller());
        inv.setItem(49, buildInfoItem(target, page, totalPages));
        inv.setItem(50, page < totalPages - 1
                ? buildControl(Material.ARROW, "§eNächste Seite", "§7Seite " + (page + 2))
                : buildFiller());

        return inv;
    }

    private static ItemStack buildMaterialItem(Material mat, boolean active) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        Component name = Component.text(formatName(mat.name()),
                active ? NamedTextColor.GREEN : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
        meta.displayName(name);

        if (active) {
            meta.lore(List.of(
                    Component.text("✔ Aktiv im Filter", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                    Component.text("Klick zum Entfernen", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.lore(List.of(
                    Component.text("✘ Nicht im Filter", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Klick zum Hinzufügen", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
            ));
        }

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildControl(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildInfoItem(SortTarget target, int page, int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6Filter-Übersicht").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("§7Aktive Filter: §e" + target.getFilter().size()).decoration(TextDecoration.ITALIC, false),
                Component.text("§7Seite §e" + (page + 1) + " §7/ §e" + total).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    public static int getTotalPages() {
        return (int) Math.ceil((double) ALL_ITEMS.size() / PAGE_SIZE);
    }

    public static List<Material> getAllItems() {
        return ALL_ITEMS;
    }

    public static int getPageSize() {
        return PAGE_SIZE;
    }

    private static String formatName(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
