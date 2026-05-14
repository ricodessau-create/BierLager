package de.bierlager.listeners;

import com.plotsquared.core.plot.Plot;
import de.bierlager.BierLager;
import de.bierlager.gui.FilterGUI;
import de.bierlager.manager.MessageManager;
import de.bierlager.manager.StorageManager;
import de.bierlager.model.PlotSortSystem;
import de.bierlager.model.SortTarget;
import de.bierlager.util.ItemUtil;
import de.bierlager.util.PlotUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChestInteractListener implements Listener {

    private final BierLager plugin;
    private final Map<UUID, FilterSession> sessions;

    public ChestInteractListener(BierLager plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
        PlotUtil.init();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!ItemUtil.isBierLagerItem(hand)) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            plugin.getMessageManager().send(player, "not-a-chest");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        Location loc = block.getLocation();
        Plot plot = PlotUtil.getPlotAt(loc);

        if (plot == null) {
            plugin.getMessageManager().send(player, "no-plot");
            return;
        }

        if (!PlotUtil.isOwnerOrTrusted(player, plot) && !player.hasPermission("bierlager.admin")) {
            plugin.getMessageManager().send(player, "not-your-plot");
            return;
        }

        String plotKey = PlotUtil.getPlotKey(plot);

        if (player.isSneaking()) {
            handleTargetInteract(player, loc, plotKey);
        } else {
            handleSourceInteract(player, loc, plotKey);
        }
    }

    private void handleSourceInteract(Player player, Location loc, String plotKey) {
        StorageManager sm = plugin.getStorageManager();
        PlotSortSystem system = sm.getOrCreate(plotKey);

        if (system.hasSource(loc)) {
            system.removeSource(loc);
            sm.save();
            plugin.getMessageManager().send(player, "source-removed");
        } else {
            int max = plugin.getConfig().getInt("max-sources-per-plot", 10);
            if (system.getSources().size() >= max) {
                plugin.getMessageManager().send(player, "max-sources");
                return;
            }
            system.addSource(loc);
            sm.save();
            plugin.getMessageManager().send(player, "source-added");
        }
    }

    private void handleTargetInteract(Player player, Location loc, String plotKey) {
        StorageManager sm = plugin.getStorageManager();
        PlotSortSystem system = sm.getOrCreate(plotKey);

        SortTarget target = system.getTarget(loc);

        if (target == null) {
            int max = plugin.getConfig().getInt("max-targets-per-plot", 20);
            if (system.getTargets().size() >= max) {
                plugin.getMessageManager().send(player, "max-targets");
                return;
            }
            target = new SortTarget(loc);
            system.addTarget(target);
            sm.save();
            plugin.getMessageManager().send(player, "target-added");
        }

        sessions.put(player.getUniqueId(), new FilterSession(plotKey, loc, 0));
        player.openInventory(FilterGUI.open(target, 0));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String rawTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!rawTitle.startsWith(FilterGUI.TITLE_BASE)) return;

        event.setCancelled(true);

        FilterSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        PlotSortSystem system = plugin.getStorageManager().getSystems().get(session.plotKey());
        if (system == null) return;

        SortTarget target = system.getTarget(session.targetLoc());
        if (target == null) return;

        int slot = event.getRawSlot();
        int page = session.page();

        if (slot == 45) {
            player.closeInventory();
            return;
        }

        if (slot == 46) {
            target.getFilter().clear();
            plugin.getStorageManager().save();
            player.openInventory(FilterGUI.open(target, page));
            return;
        }

        if (slot == 48 && page > 0) {
            int newPage = page - 1;
            sessions.put(player.getUniqueId(), new FilterSession(session.plotKey(), session.targetLoc(), newPage));
            player.openInventory(FilterGUI.open(target, newPage));
            return;
        }

        if (slot == 50 && page < FilterGUI.getTotalPages() - 1) {
            int newPage = page + 1;
            sessions.put(player.getUniqueId(), new FilterSession(session.plotKey(), session.targetLoc(), newPage));
            player.openInventory(FilterGUI.open(target, newPage));
            return;
        }

        if (slot >= 0 && slot < FilterGUI.getPageSize()) {
            int index = page * FilterGUI.getPageSize() + slot;
            List<Material> allItems = FilterGUI.getAllItems();
            if (index >= allItems.size()) return;

            Material mat = allItems.get(index);
            MessageManager mm = plugin.getMessageManager();

            if (target.hasFilter(mat)) {
                target.removeFilter(mat);
                mm.sendReplaced(player, "filter-removed", "%item%", mat.name());
            } else {
                target.addFilter(mat);
                mm.sendReplaced(player, "filter-added", "%item%", mat.name());
            }

            plugin.getStorageManager().save();
            player.openInventory(FilterGUI.open(target, page));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String rawTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (rawTitle.startsWith(FilterGUI.TITLE_BASE)) {
            sessions.remove(player.getUniqueId());
        }
    }

    private record FilterSession(String plotKey, Location targetLoc, int page) {}
}
