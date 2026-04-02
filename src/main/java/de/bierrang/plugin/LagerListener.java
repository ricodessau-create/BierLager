package de.bierrang.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LagerListener implements Listener {

    private final BierLager plugin;
    private final Map<UUID, Integer> playerFilterPage = new HashMap<>();

    public LagerListener(BierLager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (ItemManager.isRealLagerTool(item, plugin)) {
            // WICHTIG: Event canceln, damit der Kopf nicht platziert wird
            e.setCancelled(true);

            if (!(e.getClickedBlock().getState() instanceof Container)) {
                p.sendMessage(ChatColor.RED + "Ziele auf eine Truhe oder Container!");
                return;
            }

            // PLOT CHECK (Bleibt erhalten)
            boolean allowed = false;
            if (Bukkit.getPluginManager().getPlugin("PlotSquared") != null) {
                try {
                    com.plotsquared.core.player.PlotPlayer<?> pp = com.plotsquared.core.player.PlotPlayer.from(p);
                    com.plotsquared.core.plot.Plot plot = pp.getCurrentPlot();
                    if (plot != null && (plot.isAdded(p.getUniqueId()) || plot.isOwner(p.getUniqueId()))) allowed = true;
                } catch (Exception ignored) {}
            } else {
                if (p.isOp() || p.hasPermission("bierlager.admin")) allowed = true;
            }

            if (!allowed) {
                p.sendMessage(ChatColor.RED + "Keine Rechte hier.");
                return;
            }

            Location loc = plugin.getLagerManager().getNormalizedLocation(e.getClickedBlock());
            plugin.getLagerManager().setTempLocation(p.getUniqueId(), loc);
            playerFilterPage.remove(p.getUniqueId()); // Seite zurücksetzen
            
            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) {
                openSetupGUI(p);
            } else {
                openSettingsGUI(p, node);
            }
        }
    }

    // --- GUIs ---

    private void openSetupGUI(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, "§eNeue Verbindung");

        // Orange Candle
        ItemStack ausgang = new ItemStack(Material.ORANGE_CANDLE);
        ausgang.editMeta(m -> {
            m.setDisplayName("§6Ausgang");
            m.setLore(List.of("§7Items gehen von hier weg"));
        });
        inv.setItem(11, ausgang);

        // Green Candle
        ItemStack eingang = new ItemStack(Material.GREEN_CANDLE);
        eingang.editMeta(m -> {
            m.setDisplayName("§aEingang");
            m.setLore(List.of("§7Items kommen hier an", "§cFilter ist erforderlich"));
        });
        inv.setItem(15, eingang);

        p.openInventory(inv);
    }

    private void openSettingsGUI(Player p, LagerNode node) {
        Inventory inv = Bukkit.createInventory(null, 27, "§eEinstellungen");

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

    // --- FILTER GUI (NEU) ---

    private void openFilterGUI(Player p, LagerNode node, int page) {
        playerFilterPage.put(p.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, "§bFilter: " + (node.isWhitelist() ? "§aWhitelist" : "§cBlacklist"));

        // Material Liste erstellen und filtern
        List<Material> materials = Arrays.stream(Material.values())
                .filter(m -> !m.isAir() && m.isItem()) // Nur gültige Items
                .toList();

        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());

        // Items einfügen
        for (int i = startIndex; i < endIndex; i++) {
            Material mat = materials.get(i);
            ItemStack displayItem = new ItemStack(mat);
            ItemMeta meta = displayItem.getItemMeta();
            
            // Wenn im Filter, Glow Effekt
            if (node.getFilterMaterials().contains(mat)) {
                meta.addEnchant(Enchantment.LURE, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName("§a" + mat.name());
            } else {
                meta.setDisplayName("§7" + mat.name());
            }
            
            displayItem.setItemMeta(meta);
            inv.addItem(displayItem);
        }

        // Steuerung (Untere Reihe)
        // Zurück Button
        if (page > 0) {
            ItemStack back = new ItemStack(Material.ARROW);
            back.editMeta(m -> m.setDisplayName("§eVorherige Seite"));
            inv.setItem(45, back);
        }

        // Mitte: Modus Umschalten / Info
        ItemStack mode = new ItemStack(node.isWhitelist() ? Material.WHITE_DYE : Material.RED_DYE);
        mode.editMeta(m -> {
            m.setDisplayName(node.isWhitelist() ? "§aWhitelist" : "§cBlacklist");
            m.setLore(List.of("§7Klicke zum Wechseln."));
        });
        inv.setItem(49, mode);

        // Weiter Button
        if (endIndex < materials.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            next.editMeta(m -> m.setDisplayName("§eNächste Seite"));
            inv.setItem(53, next);
        }
        
        // Info bei leerem Filter
        if (node.getFilterMaterials().isEmpty()) {
            ItemStack warn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            warn.editMeta(m -> m.setDisplayName("§cKein Filter gesetzt!"));
            inv.setItem(48, warn);
        }

        p.openInventory(inv);
    }

    // --- CLICK HANDLING ---

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        // Komplettes Deaktivieren von Drag im GUI
        String title = e.getView().getTitle();
        if (title.contains("§e") || title.contains("§b")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        
        // SECURITY: Immer canceln in unseren GUIs
        if (title.contains("§e") || title.contains("§b")) {
            e.setCancelled(true);
        } else {
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
        if (loc == null) return;
        LagerNode node = plugin.getLagerManager().getNodes().get(loc);

        // SETUP GUI
        if (title.equals("§eNeue Verbindung")) {
            if (e.getSlot() == 11) {
                plugin.getLagerManager().createNode(p.getUniqueId(), loc, LagerNode.Type.AUSGANG);
                p.sendMessage("§aAusgang erstellt!");
                p.closeInventory();
            } else if (e.getSlot() == 15) {
                plugin.getLagerManager().createNode(p.getUniqueId(), loc, LagerNode.Type.EINGANG);
                p.sendMessage("§aEingang erstellt! Filter öffnet sich...");
                node = plugin.getLagerManager().getNodes().get(loc); // Node neu laden
                if(node != null) openFilterGUI(p, node, 0);
            }
        }
        // SETTINGS GUI
        else if (title.equals("§eEinstellungen")) {
            if (node == null) return;
            if (e.getSlot() == 11) {
                openFilterGUI(p, node, 0);
            } else if (e.getSlot() == 13) {
                plugin.getLagerManager().switchType(node);
                p.sendMessage("§eTyp geändert!");
                openSettingsGUI(p, node);
            } else if (e.getSlot() == 15) {
                plugin.getLagerManager().removeNode(loc);
                p.sendMessage("§cEntfernt.");
                p.closeInventory();
            }
        }
        // FILTER GUI
        else if (title.startsWith("§bFilter:")) {
            if (node == null) return;
            int slot = e.getRawSlot();

            // Navigation
            if (slot == 45) { // Zurück
                int newPage = playerFilterPage.getOrDefault(p.getUniqueId(), 0) - 1;
                if (newPage >= 0) openFilterGUI(p, node, newPage);
                return;
            }
            if (slot == 53) { // Weiter
                int newPage = playerFilterPage.getOrDefault(p.getUniqueId(), 0) + 1;
                openFilterGUI(p, node, newPage);
                return;
            }
            
            // Modus Wechsel
            if (slot == 49) {
                node.setWhitelist(!node.isWhitelist());
                plugin.getLagerManager().save();
                openFilterGUI(p, node, playerFilterPage.getOrDefault(p.getUniqueId(), 0));
                return;
            }

            // Item Klick (Material Toggle)
            if (slot < 45 && clicked.getType() != Material.AIR) {
                Material mat = clicked.getType();
                
                if (node.getFilterMaterials().contains(mat)) {
                    node.getFilterMaterials().remove(mat);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                } else {
                    node.getFilterMaterials().add(mat);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                }
                
                plugin.getLagerManager().save();
                // GUI aktualisieren
                openFilterGUI(p, node, playerFilterPage.getOrDefault(p.getUniqueId(), 0));
            }
        }
    }
}
