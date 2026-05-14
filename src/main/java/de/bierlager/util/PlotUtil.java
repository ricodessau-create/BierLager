package de.bierlager.util;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlotUtil {

    public static void init() {}

    public static Plot getPlotAt(org.bukkit.Location loc) {
        if (loc.getWorld() == null) return null;
        com.plotsquared.core.location.Location psLoc = BukkitUtil.adapt(loc);
        return psLoc.getPlot();
    }

    public static boolean isOwnerOrTrusted(Player player, Plot plot) {
        if (plot == null) return false;
        UUID uuid = player.getUniqueId();
        return plot.isOwner(uuid)
                || plot.getTrusted().contains(uuid)
                || plot.getMembers().contains(uuid);
    }

    public static String getPlotKey(Plot plot) {
        PlotId id = plot.getId();
        return plot.getWorldName() + ";" + id.getX() + ";" + id.getY();
    }
}
