package de.bierrang.plugin;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

public class LagerNode {

    public enum Type {
        AUSGANG,
        EINGANG
    }

    private final Location location;
    private Type type;
    private boolean whitelist;
    private final Set<Material> filterMaterials = new HashSet<>();

    public LagerNode(Location location, Type type) {
        this.location = location;
        this.type = type;
        this.whitelist = true;
    }

    public Location getLocation() {
        return location;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
    }

    public Set<Material> getFilterMaterials() {
        return filterMaterials;
    }
}
