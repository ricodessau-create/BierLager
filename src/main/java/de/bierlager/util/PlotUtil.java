package de.bierlager.util;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlotUtil {

    private static PlotAPI api;

    public static void init() {
        api = new PlotAPI();
    }

    public static Plot getPlotAt(org.bukkit.Location loc) {
        if (loc.getWorld() == null) return null;
        Location psLoc = Location.at(
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );
        return psLoc.getPlot();
    }

    public static boolean isOwnerOrTrusted(Player player, Plot plot) {
        if (plot == null) return false;
        UUID uuid = player.getUniqueId();
        return plot.isOwner(uuid) || plot.getTrusted().contains(uuid) || plot.getMembers().contains(uuid);
    }

    public static String getPlotKey(Plot plot) {
        PlotId id = plot.getId();
        return plot.getWorldName() + ";" + id.getX() + ";" + id.getY();
    }
}
