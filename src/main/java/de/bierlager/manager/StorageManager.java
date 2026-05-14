package de.bierlager.manager;

import de.bierlager.BierLager;
import de.bierlager.model.PlotSortSystem;
import de.bierlager.model.SortTarget;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StorageManager {

    private final BierLager plugin;
    private final Map<String, PlotSortSystem> systems;
    private File dataFile;

    public StorageManager(BierLager plugin) {
        this.plugin = plugin;
        this.systems = new HashMap<>();
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("data.yml konnte nicht erstellt werden: " + e.getMessage());
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.isConfigurationSection("systems")) return;

        for (String plotKey : Objects.requireNonNull(config.getConfigurationSection("systems")).getKeys(false)) {
            PlotSortSystem system = new PlotSortSystem(plotKey);
            String base = "systems." + plotKey;

            for (String rawLoc : config.getStringList(base + ".sources")) {
                Location loc = deserializeLocation(rawLoc);
                if (loc != null) system.addSource(loc);
            }

            if (config.isConfigurationSection(base + ".targets")) {
                for (String tKey : Objects.requireNonNull(config.getConfigurationSection(base + ".targets")).getKeys(false)) {
                    String tBase = base + ".targets." + tKey;
                    Location loc = deserializeLocation(config.getString(tBase + ".location"));
                    if (loc == null) continue;
                    Set<Material> filter = new HashSet<>();
                    for (String mat : config.getStringList(tBase + ".filter")) {
                        try {
                            filter.add(Material.valueOf(mat));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    system.addTarget(new SortTarget(loc, filter));
                }
            }

            systems.put(plotKey, system);
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, PlotSortSystem> entry : systems.entrySet()) {
            String base = "systems." + entry.getKey();
            PlotSortSystem system = entry.getValue();

            List<String> sources = new ArrayList<>();
            for (Location loc : system.getSources()) {
                sources.add(serializeLocation(loc));
            }
            config.set(base + ".sources", sources);

            int i = 0;
            for (SortTarget target : system.getTargets()) {
                String tBase = base + ".targets.t" + i;
                config.set(tBase + ".location", serializeLocation(target.getLocation()));
                List<String> filter = new ArrayList<>();
                for (Material mat : target.getFilter()) {
                    filter.add(mat.name());
                }
                config.set(tBase + ".filter", filter);
                i++;
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("data.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    public Map<String, PlotSortSystem> getSystems() {
        return systems;
    }

    public PlotSortSystem getOrCreate(String plotKey) {
        return systems.computeIfAbsent(plotKey, PlotSortSystem::new);
    }

    public PlotSortSystem findBySource(Location loc) {
        for (PlotSortSystem system : systems.values()) {
            if (system.hasSource(loc)) return system;
        }
        return null;
    }

    public PlotSortSystem findByTarget(Location loc) {
        for (PlotSortSystem system : systems.values()) {
            if (system.hasTarget(loc)) return system;
        }
        return null;
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location deserializeLocation(String s) {
        if (s == null) return null;
        String[] p = s.split(",");
        if (p.length != 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
