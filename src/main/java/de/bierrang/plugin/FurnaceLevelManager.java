package de.bierrang.plugin;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class FurnaceLevelManager {

    private final FileConfiguration config;
    private final Map<Location, Integer> levels = new HashMap<>();

    public FurnaceLevelManager(FileConfiguration config) {
        this.config = config;
    }

    public int getLevel(Location loc) {
        return levels.getOrDefault(loc, 1);
    }

    public void setLevel(Location loc, int level) {
        levels.put(loc, Math.max(1, Math.min(5, level)));
    }

    public void remove(Location loc) {
        levels.remove(loc);
    }
}
