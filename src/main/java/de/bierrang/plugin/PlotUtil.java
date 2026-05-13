package de.bierrang.plugin;

import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import org.bukkit.entity.Player;

public class PlotUtil {

    /**
     * Gibt true zurück, wenn der Spieler der Owner des Plots ist,
     * auf dem sich der angegebene Block befindet.
     * Gibt false zurück wenn kein Plot, kein Owner, oder nur Trusted/Member.
     */
    public static boolean isPlotOwner(Player player, org.bukkit.Location loc) {
        if (loc.getWorld() == null) return false;

        Location psLoc = Location.at(
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );

        Plot plot = psLoc.getOwnedPlot();
        if (plot == null) return false;

        return plot.isOwner(player.getUniqueId());
    }
}
