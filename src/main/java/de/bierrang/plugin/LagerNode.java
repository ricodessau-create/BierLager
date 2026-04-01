package de.bierrang.plugin;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LagerNode {
    public enum Type { AUSGANG, EINGANG }

    private Type type;
    private Location location;
    private UUID ownerId;
    private List<ItemStack> filterItems;
    private boolean isWhitelist;
    
    private transient long lastFailTime = 0;

    public LagerNode(Type type, Location location, UUID ownerId) {
        this.type = type;
        this.location = location;
        this.ownerId = ownerId;
        this.filterItems = new ArrayList<>();
        this.isWhitelist = true;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Location getLocation() { return location; }
    public UUID getOwnerId() { return ownerId; }
    public List<ItemStack> getFilterItems() { return filterItems; }
    public boolean isWhitelist() { return isWhitelist; }
    public void setWhitelist(boolean whitelist) { isWhitelist = whitelist; }
    
    public long getLastFailTime() { return lastFailTime; }
    public void setLastFailTime(long time) { this.lastFailTime = time; }
}
