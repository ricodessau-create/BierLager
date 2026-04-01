package de.bierrang.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LagerCommand implements CommandExecutor {

    private final BierLager plugin;

    public LagerCommand(BierLager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur für Spieler!");
            return true;
        }

        // Check auf das neue Recht "bierlager.use"
        if (!p.hasPermission("bierlager.use")) {
            p.sendMessage(ChatColor.RED + "Keine Rechte.");
            return true;
        }

        ItemStack tool = ItemManager.getLagerTool(plugin);
        p.getInventory().addItem(tool);
        p.sendMessage(ChatColor.GREEN + "Du hast das Lager-Werkzeug erhalten!");
        return true;
    }
}
