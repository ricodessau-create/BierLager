package de.bierlager.model;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

public class SortTarget {

    private final Location location;
    private final Set<Material> filter;

    public SortTarget(Location location) {
        this.location = location;
        this.filter = new HashSet<>();
    }

    public SortTarget(Location location, Set<Material> filter) {
        this.location = location;
        this.filter = new HashSet<>(filter);
    }

    public Location getLocation() {
        return location;
    }

    public Set<Material> getFilter() {
        return filter;
    }

    public void addFilter(Material material) {
        filter.add(material);
    }

    public void removeFilter(Material material) {
        filter.remove(material);
    }

    public boolean hasFilter(Material material) {
        return filter.contains(material);
    }
}
