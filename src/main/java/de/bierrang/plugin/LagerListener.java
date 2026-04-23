package de.bierrang.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LagerListener implements Listener {

    private final BierLager plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>();

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
        var inv = Bukkit.createInventory(null, 27, "Lager Verbindung");

        var ausgang = new ItemStack(Material.ORANGE_CANDLE);
        ausgang.editMeta(m -> {
            m.setDisplayName("§6Ausgang (Quelle)");
            m.setLore(List.of("§7Items gehen von hier weg"));
        });
        inv.setItem(11, ausgang);

        var eingang = new ItemStack(Material.GREEN_CANDLE);
        eingang.editMeta(m -> {
            m.setDisplayName("§aEingang (Ziel)");
            m.setLore(List.of("§7Items kommen hier an", "§cFilter ist erforderlich"));
        });
        inv.setItem(15, eingang);

        p.openInventory(inv);
    }

    private void openSettingsGUI(Player p, LagerNode node) {
        var inv = Bukkit.createInventory(null, 27, "Lager Einstellungen");

        var filterBtn = new ItemStack(Material.HOPPER);
        filterBtn.editMeta(m -> m.setDisplayName("§bFilter bearbeiten"));
        inv.setItem(11, filterBtn);

        Material typeMat = node.getType() == LagerNode.Type.AUSGANG ? Material.ORANGE_CANDLE : Material.GREEN_CANDLE;
        String nextType = node.getType() == LagerNode.Type.AUSGANG ? "Eingang" : "Ausgang";

        var switchBtn = new ItemStack(typeMat);
        switchBtn.editMeta(m -> m.setDisplayName("§eWechseln zu: " + nextType));
        inv.setItem(13, switchBtn);

        var deleteBtn = new ItemStack(Material.BARRIER);
        deleteBtn.editMeta(m -> m.setDisplayName("§cVerbindung entfernen"));
        inv.setItem(15, deleteBtn);

        p.openInventory(inv);
    }

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
        int raw = e.getRawSlot();
        int topSize = e.getView().getTopInventory().getSize();

        if (raw >= topSize) return;

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
                        if (node != null) openFilterGUI(p, node, 0);
                    }
                }.runTaskLater(plugin, 1L);
            }
            return;
        }

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

        if (title.startsWith("Filter:")) {
            e.setCancelled(true);

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Location loc = plugin.getLagerManager().getTempLocation(p.getUniqueId());
            if (loc == null) return;

            LagerNode node = plugin.getLagerManager().getNodes().get(loc);
            if (node == null) return;

            int page = playerPage.getOrDefault(p.getUniqueId(), 0);

            if (e.getSlot() == 45) {
                int newPage = Math.max(0, page - 1);
                playerPage.put(p.getUniqueId(), newPage);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, newPage);
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }

            if (e.getSlot() == 53) {
                int newPage = page + 1;
                playerPage.put(p.getUniqueId(), newPage);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, newPage);
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }

            if (e.getSlot() == 49) {
                node.setWhitelist(!node.isWhitelist());
                plugin.getLagerManager().save();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, page);
                    }
                }.runTaskLater(plugin, 1L);
                return;
            }

            if (e.getSlot() < 45) {
                Material mat = clicked.getType();

                if (node.getFilterMaterials().contains(mat)) {
                    node.getFilterMaterials().remove(mat);
                } else {
                    node.getFilterMaterials().add(mat);
                }

                plugin.getLagerManager().save();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openFilterGUI(p, node, page);
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playerPage.remove(uuid);
        plugin.getLagerManager().setTempLocation(uuid, null);
    }

    private void openFilterGUI(Player p, LagerNode node, int page) {
        String mode = node.isWhitelist() ? "§aWhitelist" : "§cBlacklist";
        var inv = Bukkit.createInventory(null, 54, "Filter: " + mode);

        List<Material> materials = new ArrayList<>();
        for (Material m : Material.values()) {
            if (!m.isAir() && m.isItem()) materials.add(m);
        }
        materials.sort(Comparator.comparing(Enum::name));

        int itemsPerPage = 45;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, materials.size());

        for (int i = start; i < end; i++) {
            Material mat = materials.get(i);
            ItemStack item = new ItemStack(mat);

            item.editMeta(m -> {
                if (node.getFilterMaterials().contains(mat)) {
                    m.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                    m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    m.setDisplayName("§a" + mat.name());
