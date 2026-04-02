package de.bierrang.plugin;

import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LagerListener implements Listener {

    private final BierLager plugin;

    public LagerListener(BierLager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getState() instanceof Container)) return;

        Player p = e.getPlayer();
        ItemStack item = e.getItem();

        if (ItemManager.isRealLagerTool(item, plugin)) {
            e.setCancelled(true);

            // PLOT SQUARED CHECK
            if (Bukkit.getPluginManager().getPlugin("PlotSquared") != null) {
                PlotPlayer<?> pp = PlotPlayer.from(p);
                Plot plot = pp.getCurrentPlot();
                if (plot != null) {
                    if (!plot.isAdded(p.getUniqueId()) && !plot.isOwner(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "Du hast keine Rechte auf diesem Grundstück!");
                        return;
                    }
                } else {
                    p.sendMessage(ChatColor.RED + "Lager können nur auf Grundstücken gebaut werden.");
                    return;
                }
            }

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
        Inventory inv = Bukkit.createInventory(null, 27, "§eNeue Verbindung");

        ItemStack ausgang = new ItemStack(Material.HOPPER);
        ausgang.editMeta(m -> {
            m.setDisplayName("§aAusgang (Quelle)");
            m.setLore(List.of("§7Items werden HIER entnommen."));
        });
        inv.setItem(11, ausgang);

        ItemStack eingang = new ItemStack(Material.DISPENSER);
        eingang.editMeta(m -> {
            m.setDisplayName("§cEingang (Ziel)");
            m.setLore(List.of("§7Items kommen HIER an."));
        });
        inv.setItem(15, eingang);

        p.openInventory(inv);
    }

    private void openSettingsGUI(Player p, LagerNode node) {
        Inventory inv = Bukkit.createInventory(null, 27, "§eEinstellungen");

        ItemStack filterBtn = new ItemStack(Material.HOPPER);
        filterBtn.editMeta(m -> m.setDisplayName("§bFilter bearbeiten"));
        inv.setItem(11, filterBtn);

        Material typeMat = node.getType() == LagerNode.Type.AUSGANG ? Material.DISPENSER : Material.HOPPER;
        String nextType = node.getType() == LagerNode.Type.AUSGANG ? "Eingang" : "Ausgang";
        ItemStack switchBtn = new ItemStack(typeMat);
        switchBtn.editMeta(m -> m.setDisplayName("§eWechseln zu: " + nextType));
        inv.setItem(13, switchBtn);

        ItemStack deleteBtn = new ItemStack(Material.BARRIER);
        deleteBtn.editMeta(m -> m.setDisplayName("§cVerbindung entfernen"));
        inv.setItem(15, deleteBtn);
        
        p.openInventory(inv);
    }

    private void openFilterGUI(Player p, LagerNode node) {
        Inventory inv = Bukkit.createInventory(null, 54, "§bFilter: " + (node.isWhitelist() ? "§aWhitelist" : "§cBlacklist"));

        ItemStack mode = new ItemStack(node.isWhitelist() ? Material.WHITE_DYE : Material.RED_DYE);
        mode.editMeta(m -> {
            m.setDisplayName(node.isWhitelist() ? "§aWhitelist" : "§cBlacklist");
            m.setLore(List.of("§7Klicke zum Wechseln."));
        });
        inv.setItem(49, mode);

        for (ItemStack fi : node.getFilterItems()) inv.addItem(fi);
        
        p.openInventory(inv);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title.startsWith("§bFilter:")) {
            if (e.getRawSlots().contains(49)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (title.equals("§eNeue Verbindung")) {
            e.setCancelled(true);
            Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
            if (loc == null) return;

            if (e.getSlot() == 11) {
                plugin.getLagerManager().createNode(p.getUniqueId(), loc, LagerNode.Type.AUSGANG);
                p.sendMessage("§aAusgang erstellt!");
                p.closeInventory();
            } else if (e.getSlot() == 15) {
                plugin.getLagerManager().createNode(p.getUniqueId(), loc, LagerNode.Type.EINGANG);
                p.sendMessage("§aEingang erstellt! Filter öffnet sich...");
                LagerNode node = plugin.getLagerManager().getNodes().get(loc);
                if(node != null) openFilterGUI(p, node);
            }
        }
        else if (title.equals("§eEinstellungen")) {
            e.setCancelled(true);
            Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) return;

            if (e.getSlot() == 11) openFilterGUI(p, node);
            else if (e.getSlot() == 13) {
                plugin.getLagerManager().switchType(node);
                p.sendMessage("§eTyp geändert!");
                openSettingsGUI(p, node);
            } else if (e.getSlot() == 15) {
                plugin.getLagerManager().removeNode(loc);
                p.sendMessage("§cEntfernt.");
                p.closeInventory();
            }
        }
        else if (title.startsWith("§bFilter:")) {
            int slot = e.getRawSlot();
            
            if (slot == 49) {
                e.setCancelled(true);
                Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
                LagerNode node = plugin.getLagerManager().getNodes().get(loc);
                if (node != null) {
                    node.setWhitelist(!node.isWhitelist());
                    plugin.getLagerManager().save();
                    openFilterGUI(p, node);
                }
                return;
            }
            
            if (slot < 54) {
                e.setCancelled(false); 
            } else {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (e.getView().getTitle().startsWith("§bFilter:")) {
            // FEHLERBEHEBUNG: getUniqueId() statt Player-Objekt verwendet
            Location loc = plugin.getLagerManager().getTempLocation(e.getPlayer().getUniqueId());
            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) return;

            List<ItemStack> newFilter = new ArrayList<>();
            for (ItemStack item : e.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR && item.getType() != Material.WHITE_DYE && item.getType() != Material.RED_DYE) {
                    newFilter.add(item);
                }
            }
            node.getFilterItems().clear();
            node.getFilterItems().addAll(newFilter);
            plugin.getLagerManager().save();
            e.getPlayer().sendMessage("§aFilter gespeichert!");
        }
    }
}
