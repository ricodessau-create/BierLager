package de.bierrang.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class LagerManager {

    private final BierLager plugin;
    private final File dataFile;
    private final FileConfiguration dataConfig;

    private final Map<Location, LagerNode> nodes = new HashMap<>();
    private final List<LagerNode> inputs  = new ArrayList<>();
    private final List<LagerNode> outputs = new ArrayList<>();

    private final Map<UUID, Location> tempLocations = new HashMap<>();
    private boolean needsSave = false;

    public LagerManager(BierLager plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "lager.yml");

        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Konnte lager.yml nicht erstellen", e);
            }
        }

        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    tryTransfer();
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Fehler beim Lager-Transfer", ex);
                }
                if (needsSave) {
                    save();
                    needsSave = false;
                }
            }
        }.runTaskTimer(plugin, 200L, 200L);
    }

    // -------------------------------------------------------------------------
    // Laden & Speichern
    // -------------------------------------------------------------------------

    public void load() {
        nodes.clear();
        inputs.clear();
        outputs.clear();

        ConfigurationSection section = dataConfig.getConfigurationSection("nodes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection nodeSec = section.getConfigurationSection(key);
            if (nodeSec == null) continue;

            String worldName  = nodeSec.getString("world");
            int x             = nodeSec.getInt("x");
            int y             = nodeSec.getInt("y");
            int z             = nodeSec.getInt("z");
            String typeStr    = nodeSec.getString("type", "AUSGANG");
            boolean whitelist = nodeSec.getBoolean("whitelist", true);
            List<String> mats = nodeSec.getStringList("materials");

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Location loc = new Location(world, x, y, z);

            LagerNode.Type type;
            try {
                type = LagerNode.Type.valueOf(typeStr);
            } catch (Exception e) {
                type = LagerNode.Type.AUSGANG;
            }

            LagerNode node = new LagerNode(loc, type);
            node.setWhitelist(whitelist);

            for (String m : mats) {
                try {
                    node.getFilterMaterials().add(Material.valueOf(m));
                } catch (Exception ignored) {}
            }

            nodes.put(loc, node);
            if (type == LagerNode.Type.EINGANG) inputs.add(node);
            else outputs.add(node);
        }
    }

    public void save() {
        dataConfig.set("nodes", null);
        ConfigurationSection section = dataConfig.createSection("nodes");

        int index = 0;
        for (LagerNode node : nodes.values()) {
            Location loc = node.getLocation();
            ConfigurationSection nodeSec = section.createSection("node_" + index++);

            nodeSec.set("world",     loc.getWorld().getName());
            nodeSec.set("x",         loc.getBlockX());
            nodeSec.set("y",         loc.getBlockY());
            nodeSec.set("z",         loc.getBlockZ());
            nodeSec.set("type",      node.getType().name());
            nodeSec.set("whitelist", node.isWhitelist());

            List<String> mats = new ArrayList<>();
            for (Material m : node.getFilterMaterials()) mats.add(m.name());
            nodeSec.set("materials", mats);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte lager.yml nicht speichern", e);
        }
    }

    // -------------------------------------------------------------------------
    // Node-Verwaltung
    // -------------------------------------------------------------------------

    public Map<Location, LagerNode> getNodes() {
        return nodes;
    }

    public Location getNormalizedLocation(Block block) {
        return getNormalizedLocation(block.getLocation());
    }

    public Location getNormalizedLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public void setTempLocation(UUID uuid, Location loc) {
        if (loc == null) tempLocations.remove(uuid);
        else tempLocations.put(uuid, getNormalizedLocation(loc));
    }

    public Location getTempLocation(UUID uuid) {
        return tempLocations.get(uuid);
    }

    public void createNode(UUID owner, Location loc, LagerNode.Type type) {
        Location normalized = getNormalizedLocation(loc);
        if (normalized == null) return;

        LagerNode node = nodes.get(normalized);
        if (node != null) {
            if (node.getType() == LagerNode.Type.EINGANG) inputs.remove(node);
            else outputs.remove(node);

            node.setType(type);

            if (type == LagerNode.Type.EINGANG) inputs.add(node);
            else outputs.add(node);

            needsSave = true;
            return;
        }

        node = new LagerNode(normalized, type);
        nodes.put(normalized, node);

        if (type == LagerNode.Type.EINGANG) inputs.add(node);
        else outputs.add(node);

        needsSave = true;
    }

    public void removeNode(Location loc) {
        Location normalized = getNormalizedLocation(loc);
        if (normalized == null) return;

        LagerNode node = nodes.remove(normalized);
        if (node != null) {
            inputs.remove(node);
            outputs.remove(node);
            needsSave = true;
        }
    }

    public void switchType(LagerNode node) {
        if (node == null) return;

        if (node.getType() == LagerNode.Type.AUSGANG) {
            outputs.remove(node);
            node.setType(LagerNode.Type.EINGANG);
            inputs.add(node);
        } else {
            inputs.remove(node);
            node.setType(LagerNode.Type.AUSGANG);
            outputs.add(node);
        }

        needsSave = true;
    }

    // -------------------------------------------------------------------------
    // Transfer-Logik
    //
    // Filter sitzt auf dem EINGANG:
    //   - Leerer Filter  → nimmt NICHTS an (kein Filter = kein Transport)
    //   - Whitelist      → nur eingetragene Items werden angenommen
    //   - Blacklist      → alles außer eingetragenen Items wird angenommen
    //
    // Für jedes Item im AUSGANG werden alle EINGANG-Nodes geprüft.
    // Findet sich kein passender EINGANG, bleibt das Item im AUSGANG.
    // -------------------------------------------------------------------------

    private void tryTransfer() {
        if (inputs.isEmpty() || outputs.isEmpty()) return;

        List<LagerNode> outCopy = new ArrayList<>(outputs);
        List<LagerNode> inCopy  = new ArrayList<>(inputs);

        for (LagerNode outNode : outCopy) {
            Container outContainer = getContainerAt(outNode.getLocation());
            if (outContainer == null) continue;

            Inventory outInv = outContainer.getInventory();

            for (int slot = 0; slot < outInv.getSize(); slot++) {
                ItemStack stack = outInv.getItem(slot);
                if (stack == null || stack.getType().isAir()) continue;

                // Jeden EINGANG prüfen – Filter sitzt hier
                for (LagerNode inNode : inCopy) {
                    if (!passesFilter(inNode, stack.getType())) continue;

                    Container inContainer = getContainerAt(inNode.getLocation());
                    if (inContainer == null) continue;

                    Inventory inInv = inContainer.getInventory();
                    Map<Integer, ItemStack> leftover = inInv.addItem(stack.clone());

                    if (leftover.isEmpty()) {
                        outInv.setItem(slot, null);
                        needsSave = true;
                        break; // Item wurde verschoben, nächster Slot
                    }
                }
            }
        }
    }

    private Container getContainerAt(Location loc) {
        if (loc == null) return null;
        World world = loc.getWorld();
        if (world == null) return null;

        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return null;
        if (!(loc.getBlock().getState() instanceof Container c)) return null;
        return c;
    }

    /**
     * Prüft ob ein Item den Filter eines EINGANG-Nodes passiert.
     *
     * Kein Filter (leere Liste) = NICHTS wird angenommen → false
     * Whitelist               = nur gelistete Materials → contains == true
     * Blacklist               = alles außer gelistete   → contains == false
     */
    private boolean passesFilter(LagerNode node, Material mat) {
        if (node.getFilterMaterials().isEmpty()) return false;
        boolean contains = node.getFilterMaterials().contains(mat);
        return node.isWhitelist() == contains;
    }
}
