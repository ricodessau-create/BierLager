package de.bierlager.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class PlotSortSystem {

    private final String plotKey;
    private final List<Location> sources;
    private final List<SortTarget> targets;

    public PlotSortSystem(String plotKey) {
        this.plotKey = plotKey;
        this.sources = new ArrayList<>();
        this.targets = new ArrayList<>();
    }

    public String getPlotKey() {
        return plotKey;
    }

    public List<Location> getSources() {
        return sources;
    }

    public List<SortTarget> getTargets() {
        return targets;
    }

    public void addSource(Location loc) {
        sources.add(loc);
    }

    public void removeSource(Location loc) {
        sources.removeIf(l -> locEquals(l, loc));
    }

    public boolean hasSource(Location loc) {
        return sources.stream().anyMatch(l -> locEquals(l, loc));
    }

    public void addTarget(SortTarget target) {
        targets.add(target);
    }

    public void removeTarget(Location loc) {
        targets.removeIf(t -> locEquals(t.getLocation(), loc));
    }

    public boolean hasTarget(Location loc) {
        return targets.stream().anyMatch(t -> locEquals(t.getLocation(), loc));
    }

    public SortTarget getTarget(Location loc) {
        return targets.stream()
                .filter(t -> locEquals(t.getLocation(), loc))
                .findFirst()
                .orElse(null);
    }

    private boolean locEquals(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
