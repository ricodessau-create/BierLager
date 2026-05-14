package de.bierlager.listeners;

import de.bierlager.BierLager;
import de.bierlager.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final BierLager plugin;

    public PlayerJoinListener(BierLager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("give-on-join", true)) return;

        Player player = event.getPlayer();

        if (!player.hasPermission("bierlager.use")) return;

        if (ItemUtil.playerHasBierLagerItem(player)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!ItemUtil.playerHasBierLagerItem(player)) {
                player.getInventory().addItem(ItemUtil.createBierLagerItem());
                plugin.getMessageManager().send(player, "item-received");
            }
        }, 20L);
    }
}
