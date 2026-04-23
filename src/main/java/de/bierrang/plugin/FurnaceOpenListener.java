package de.bierrang.plugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.block.BlastFurnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class FurnaceOpenListener implements Listener {

    private final FurnaceLevelManager levelManager;

    public FurnaceOpenListener(FurnaceLevelManager levelManager) {
        this.levelManager = levelManager;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        Block block = e.getInventory().getLocation().getBlock();
        Material type = block.getType();

        if (type != Material.FURNACE &&
            type != Material.SMOKER &&
            type != Material.BLAST_FURNACE) return;

        int level = levelManager.getLevel(block.getLocation());

        String stars = StarUtil.getStars(level);
        e.getView().setTitle(stars + " BierOfen");
    }
}
