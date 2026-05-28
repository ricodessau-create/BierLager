package de.bierlager.manager;

import de.bierlager.BierLager;
import de.bierlager.model.PlotSortSystem;
import de.bierlager.model.SortTarget;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
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
                    if (item == null) continue;

                    SortTarget target = findTarget(system, item);
                    if (target == null) continue;

                    Inventory targetInv = getInventory(target.getLocation());
                    if (targetInv == null) continue;

                    if (isSameInventory(sourceInv, targetInv)) continue;
                    
                    ItemStack toMove = item.clone();
                    Map<Integer, ItemStack> leftover = targetInv.addItem(item.clone());
                    
                    if (leftover.isEmpty()) {
                        
                        sourceInv.setItem(i, null);
                        contents[i] = null;
                    } else {
                        ItemStack remaining = leftover.get(0);
                        if (remaining.getAmount() < item.getAmount()) {
                            sourceInv.setItem(i, remaining);
                        }
                    }
                }
            }
        }
    }

    private SortTarget findTarget(PlotSortSystem system, ItemStack item) {
        for (SortTarget target : system.getTargets()) {
            if (target.hasFilter(item.getType())) return target;
        }
        return null;
    }

    private Inventory getInventory(Location loc) {
        if (loc.getWorld() == null) return null;
        Block block = loc.getWorld().getBlockAt(loc);
        if (block.getState() instanceof Chest chest) {
            return chest.getInventory();
        }
        return null;
    }
}
