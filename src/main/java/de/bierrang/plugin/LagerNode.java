package de.bierrang.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LagerNode {
    public enum Type { AUSGANG, EINGANG }

    private Type type;
    private Location location;
    private UUID ownerId;
    private Set<Material> filterMaterials; // GEÄNDERT: Set<Material>
    private boolean isWhitelist;
    
    private transient long lastFailTime = 0;

    public LagerNode(Type type, Location location, UUID ownerId) {
        this.type = type;
        this.location = location;
        this.ownerId = ownerId;
        this.filterMaterials = new HashSet<>();
        this.isWhitelist = true;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Location getLocation() { return location; }
    public UUID getOwnerId() { return ownerId; }
    public Set<Material> getFilterMaterials() { return filterMaterials; }
    public boolean isWhitelist() { return isWhitelist; }
    public void setWhitelist(boolean whitelist) { isWhitelist = whitelist; }
    
    public long getLastFailTime() { return lastFailTime; }
    public void setLastFailTime(long time) { this.lastFailTime = time; }
}
