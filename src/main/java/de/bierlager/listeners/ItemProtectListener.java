package de.bierlager.listeners;

import de.bierlager.util.ItemUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class ItemProtectListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (ItemUtil.isBierLagerItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        boolean cursorIsItem = ItemUtil.isBierLagerItem(cursor);
        boolean currentIsItem = ItemUtil.isBierLagerItem(current);

        if (!cursorIsItem && !currentIsItem) return;

        InventoryType.SlotType slotType = event.getSlotType();
        if (slotType == InventoryType.SlotType.CRAFTING || slotType == InventoryType.SlotType.RESULT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (ItemUtil.isBierLagerItem(ingredient)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
