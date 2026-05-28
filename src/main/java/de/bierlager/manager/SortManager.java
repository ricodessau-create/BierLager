package de.bierlager.manager;

import de.bierlager.BierLager;
import de.bierlager.model.PlotSortSystem;
import de.bierlager.model.SortTarget;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SortManager {

    private final BierLager plugin;

    public SortManager(BierLager plugin) {
        this.plugin = plugin;
    }

    public void runSort() {
        for (PlotSortSystem system : plugin.getStorageManager().getSystems().values()) {
            for (Location sourceLoc : system.getSources()) {
                Inventory sourceInv = getInventory(sourceLoc);
                if (sourceInv == null) continue;

                ItemStack[] contents = sourceInv.getContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item == null || item.getType().isAir()) continue;

                    SortTarget target = findTarget(system, item);
                    if (target == null) continue;

                    Inventory targetInv = getInventory(target.getLocation());
                    if (targetInv == null) continue;

                    if (sourceInv.equals(targetInv)) continue;

                    ItemStack toMove = item.clone();
                    Map<Integer, ItemStack> leftover = targetInv.addItem(toMove);

                    if (leftover.isEmpty()) {
                        sourceInv.setItem(i, null);
                        contents[i] = null;
                    } else {
                        ItemStack remaining = leftover.get(0);
                        if (remaining.getAmount() < item.getAmount()) {
                            item.setAmount(remaining.getAmount());
                            sourceInv.setItem(i, item);
                            contents[i] = item;
                        }
                    }
                }
            }
        }
    }

    /**
     * Ziele mit passendem Filter haben Vorrang.
     * Ein Ziel ohne Filter gilt als Catch-all und wird nur als Fallback genutzt.
     */
    private SortTarget findTarget(PlotSortSystem system, ItemStack item) {
        SortTarget catchAll = null;
        for (SortTarget target : system.getTargets()) {
            if (target.getFilter().isEmpty()) {
                if (catchAll == null) catchAll = target;
            } else if (target.hasFilter(item.getType())) {
                return target;
            }
        }
        return catchAll;
    }

    private Inventory getInventory(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        Block block = loc.getWorld().getBlockAt(loc);
        if (!(block.getState() instanceof InventoryHolder holder)) return null;
        return holder.getInventory();
    }
}
