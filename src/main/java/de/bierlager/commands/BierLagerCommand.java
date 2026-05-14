package de.bierlager.commands;

import de.bierlager.BierLager;
import de.bierlager.util.ItemUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BierLagerCommand implements CommandExecutor {

    private final BierLager plugin;

    public BierLagerCommand(BierLager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler.");
            return true;
        }

        if (!player.hasPermission("bierlager.use")) {
            plugin.getMessageManager().send(player, "no-permission");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("get")) {
            if (!ItemUtil.playerHasBierLagerItem(player)) {
                player.getInventory().addItem(ItemUtil.createBierLagerItem());
                plugin.getMessageManager().send(player, "item-received");
            } else {
                player.sendMessage("§6[BierLager] §eDu hast das Item bereits.");
            }
            return true;
        }

        player.sendMessage("§6[BierLager] §7Befehle:");
        player.sendMessage("§e/bierlager get §7- Item erhalten");
        player.sendMessage("§7Rechtsklick auf Truhe §e(Item in Hand)§7 = Quelle");
        player.sendMessage("§7Shift+Rechtsklick §7= Ziel + Filter");
        return true;
    }
}
