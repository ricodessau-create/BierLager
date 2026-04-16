package de.bierrang.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

    // --- ITEM INTERACTION (GUI ÖFFNEN) ---
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getState() instanceof Container)) return;

        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (ItemManager.isLagerTool(item, plugin)) {
            e.setCancelled(true);

            Location loc = plugin.getLagerManager().getNormalizedLocation(e.getClickedBlock());
            plugin.getLagerManager().setTempLocation(p.getUniqueId(), loc);
            
            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) {
                openSetupGUI(p);
            } else {
                openSettingsGUI(p, node);
            }
        }
    }

    private void openSetupGUI(Player p) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "Lager Verbindung");

        ItemStack ausgang = new ItemStack(Material.ORANGE_CANDLE);
        ausgang.editMeta(m -> {
            m.setDisplayName("§6Ausgang (Quelle)");
            m.setLore(Arrays.asList("§7Items gehen von hier weg"));
        });
        inv.setItem(11, ausgang);

        ItemStack eingang = new ItemStack(Material.GREEN_CANDLE);
        eingang.editMeta(m -> {
            m.setDisplayName("§aEingang (Ziel)");
            m.setLore(Arrays.asList("§7Items kommen hier an", "§cFilter ist erforderlich"));
        });
        inv.setItem(15, eingang);

        p.openInventory(inv);
    }

    private void openSettingsGUI(Player p, LagerNode node) {
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 27, "Lager Einstellungen");

        ItemStack filterBtn = new ItemStack(Material.HOPPER);
        filterBtn.editMeta(m -> m.setDisplayName("§bFilter bearbeiten"));
        inv.setItem(11, filterBtn);

        Material typeMat = node.getType() == LagerNode.Type.AUSGANG ? Material.ORANGE_CANDLE : Material.GREEN_CANDLE;
        String nextType = node.getType() == LagerNode.Type.AUSGANG ? "Eingang" : "Ausgang";
        ItemStack switchBtn = new ItemStack(typeMat);
        switchBtn.editMeta(m -> m.setDisplayName("§eWechseln zu: " + nextType));
        inv.setItem(13, switchBtn);

        ItemStack deleteBtn = new ItemStack(Material.BARRIER);
        deleteBtn.editMeta(m -> m.setDisplayName("§cVerbindung entfernen"));
        inv.setItem(15, deleteBtn);
        
        p.openInventory(inv);
    }

    // --- GUI HANDLING ---
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title.contains("Filter:") || title.equals("Lager Verbindung") || title.equals("Lager Einstellungen")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        // --- SETUP MENÜ ---
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
                p.sendMessage(ChatColor.GREEN + "Eingang erstellt! Filter öffnet sich...");
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

        // --- SETTINGS MENÜ ---
        if (title.equals("Lager Einstellungen")) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
            if (loc == null) return;
            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) return;

            if (e.getSlot() == 11) {
                openFilterGUI(p, node, 0);
            } else if (e.getSlot() == 13) {
                plugin.getLagerManager().switchType(node);
                p.sendMessage(ChatColor.YELLOW + "Typ geändert!");
                openSettingsGUI(p, node);
            } else if (e.getSlot() == 15) {
                plugin.getLagerManager().removeNode(loc);
                p.sendMessage(ChatColor.RED + "Entfernt.");
                p.closeInventory();
            }
            return;
        }

        // --- FILTER MENÜ ---
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

            if (slot == 45) { // Zurück
                int newPage = Math.max(0, currentPage - 1);
                this.playerPage.put(p.getUniqueId(), newPage);
                new BukkitRunnable() { @Override public void run() { openFilterGUI(p, node, newPage); } }.runTaskLater(plugin, 1L);
                return;
            }
            if (slot == 53) { // Weiter
                int newPage = currentPage + 1;
                this.playerPage.put(p.getUniqueId(), newPage);
                new BukkitRunnable() { @Override public void run() { openFilterGUI(p, node, newPage); } }.runTaskLater(plugin, 1L);
                return;
            }
            if (slot == 49) { // Modus Wechsel
                node.setWhitelist(!node.isWhitelist());
                plugin.getLagerManager().save();
                new BukkitRunnable() { @Override public void run() { openFilterGUI(p, node, currentPage); } }.runTaskLater(plugin, 1L);
                return;
            }

            // Item Klick
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
                new BukkitRunnable() { @Override public void run() { openFilterGUI(p, node, currentPage); } }.runTaskLater(plugin, 1L);
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

        if (page > 0) {
            ItemStack back = new ItemStack(Material.ARROW);
            back.editMeta(m -> m.setDisplayName("§eVorherige Seite"));
            inv.setItem(45, back);
        }

        ItemStack modeItem = new ItemStack(node.isWhitelist() ? Material.WHITE_DYE : Material.RED_DYE);
        modeItem.editMeta(m -> {
            m.setDisplayName(node.isWhitelist() ? "§aWhitelist" : "§cBlacklist");
            m.setLore(Arrays.asList("§7Klicke zum Wechseln."));
        });
        inv.setItem(49, modeItem);

        if (endIndex < materials.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            next.editMeta(m -> m.setDisplayName("§eNächste Seite"));
            inv.setItem(53, next);
        }
        p.openInventory(inv);
    }
}
