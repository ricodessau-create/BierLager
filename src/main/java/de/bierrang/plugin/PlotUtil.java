package de.bierrang.plugin;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.plot.Plot;
import org.bukkit.entity.Player;

/**
 * Hilfsmethoden für PlotSquared-Berechtigungsprüfungen.
 */
public class PlotUtil {

    /**
     * Gibt true zurück, wenn der Spieler der Owner des Plots ist,
     * auf dem sich der angegebene Block befindet.
     *
     * Gibt false zurück wenn:
     * - der Block nicht auf einem Plot liegt
     * - der Plot keinen Owner hat
     * - der Spieler nur Trusted/Member, aber nicht Owner ist
     */
    public static boolean isPlotOwner(Player player, org.bukkit.Location loc) {
        if (loc.getWorld() == null) return false;

        com.plotsquared.core.location.Location psLoc = BukkitUtil.adapt(loc);

        Plot plot = psLoc.getOwnedPlot();
        if (plot == null) return false;

        return plot.isOwner(player.getUniqueId());
    }
}
