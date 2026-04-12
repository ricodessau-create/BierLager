package de.bierrang.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LagerListener implements Listener {

    private final BierLager plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    public LagerListener(BierLager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent e) {
        if (e.getView().getTitle().contains("Filter:")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        // --- Verbindungs-Menü ---
        if (title.equals("Lager Verbindung")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
            if (loc == null) return;

            if (e.getSlot() == 11) {
                plugin.getLagerManager().createNode(p.getUniqueId(), loc, LagerNode.Type.AUSGANG);
                p.sendMessage(ChatColor.GREEN + "Ausgang erstellt!");
                p.closeInventory();
            } else if (e.getSlot() == 15) {
                plugin.getLagerManager().createNode(p.getUniqueId(), loc, LagerNode.Type.EINGANG);
                p.sendMessage(ChatColor.GREEN + "Eingang erstellt!");
                p.closeInventory();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        LagerNode node = plugin.getLagerManager().getNodes().get(loc);
                        if(node != null) openFilterGUI(p, node, 0);
                    }
                }.runTaskLater(plugin, 1L);
            }
            return;
        }

        // --- Filter-Menü ---
        if (title.startsWith("Filter:")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
            if (loc == null) return;
            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) return;

            int slot = e.getRawSlot();
            int currentPage = this.playerPage.getOrDefault(p.getUniqueId(), 0);

            // ZURÜCK (Slot 45)
            if (slot == 45) {
                int newPage = Math.max(0, currentPage - 1);
                this.playerPage.put(p.getUniqueId(), newPage);
                // WICHTIG: Einen Tick warten!
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, newPage);
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }

            // WEITER (Slot 53)
            if (slot == 53) {
                int newPage = currentPage + 1;
                this.playerPage.put(p.getUniqueId(), newPage);
                // WICHTIG: Einen Tick warten!
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, newPage);
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }

            // MODUS WECHSELN (Slot 49)
            if (slot == 49) {
                node.setWhitelist(!node.isWhitelist());
                plugin.getLagerManager().save();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, currentPage);
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }

            // ITEM KLICKEN (Toggle)
            if (slot < 45) {
                Material mat = clicked.getType();

                if (node.getFilterMaterials().contains(mat)) {
                    node.getFilterMaterials().remove(mat);
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                } else {
                    node.getFilterMaterials().add(mat);
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                }

                plugin.getLagerManager().save();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, currentPage);
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    private void openFilterGUI(Player p, LagerNode node, int page) {
        String mode = node.isWhitelist() ? "§aWhitelist" : "§cBlacklist";
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 54, "Filter: " + mode);

        java.util.List<Material> materials = new java.util.ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isAir() && m.isItem()) materials.add(m);
        }
        materials.sort((m1, m2) -> m1.name().compareTo(m2.name()));

        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());

        for (int i = startIndex; i < endIndex; i++) {
            Material mat = materials.get(i);
            ItemStack displayItem = new ItemStack(mat);
            org.bukkit.inventory.meta.ItemMeta meta = displayItem.getItemMeta();

            if (node.getFilterMaterials().contains(mat)) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName("§a" + mat.name());
            } else {
                meta.setDisplayName("§7" + mat.name());
            }

            displayItem.setItemMeta(meta);
            inv.addItem(displayItem);
        }

        // Pfeil Zurück
        if (page > 0) {
            ItemStack back = new ItemStack(Material.ARROW);
            back.editMeta(m -> m.setDisplayName("§eVorherige Seite"));
            inv.setItem(45, back);
        }

        // Modus Item
        ItemStack modeItem = new ItemStack(node.isWhitelist() ? Material.WHITE_DYE : Material.RED_DYE);
        modeItem.editMeta(m -> {
            m.setDisplayName(node.isWhitelist() ? "§aWhitelist" : "§cBlacklist");
            m.setLore(Arrays.asList("§7Klicke zum Wechseln."));
        });
        inv.setItem(49, modeItem);

        // Pfeil Weiter
        if (endIndex < materials.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            next.editMeta(m -> m.setDisplayName("§eNächste Seite"));
            inv.setItem(53, next);
        }

        p.openInventory(inv);
    }
}
