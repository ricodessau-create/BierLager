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
    private final List<LagerNode> inputs = new ArrayList<>();  // Quellen (Ausgang)
    private final List<LagerNode> outputs = new ArrayList<>(); // Ziele (Eingang)
    
    private final Map<UUID, Location> lastInteracted = new HashMap<>();
    private boolean needsSave = false;

    public LagerManager(BierLager plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        startTransferTask();
        startAutoSaveTask();
    }

    private void startTransferTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                transferAllItems();
            }
        }.runTaskTimer(plugin, 20L, 10L);
    }
    
    private void startAutoSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (needsSave) {
                    performSave();
                    needsSave = false;
                }
            }
        }.runTaskTimer(plugin, 6000L, 6000L);
    }

    private void transferAllItems() {
        // Iteriere durch alle QUELLEN (Inputs/Ausgang)
        for (LagerNode sourceNode : inputs) {
            if (System.currentTimeMillis() - sourceNode.getLastFailTime() < 2000) continue;
            if (!isChunkLoaded(sourceNode.getLocation())) continue;

            Block sourceBlock = sourceNode.getLocation().getBlock();
            if (!(sourceBlock.getState() instanceof Container sourceContainer)) continue;

            Inventory sourceInv = sourceContainer.getInventory();
            
            boolean anyItemMoved = false;
            
            for (int i = 0; i < sourceInv.getSize(); i++) {
                ItemStack item = sourceInv.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;

                boolean movedThisSlot = false;
                
                // Iteriere durch alle ZIELE (Outputs/Eingang)
                for (LagerNode targetNode : outputs) {
                    // 1. Owner Check
                    if (!Objects.equals(sourceNode.getOwnerId(), targetNode.getOwnerId())) continue;
                    
                    // 2. Chunk Check Ziel
                    if (!isChunkLoaded(targetNode.getLocation())) continue;

                    // 3. FILTER CHECK (HIER IST DIE LÖSUNG!)
                    // Wir prüfen den Filter am ZIEL (targetNode), nicht an der Quelle.
                    if (targetNode.getFilterMaterials().isEmpty()) continue; // Ziel ohne Filter ist deaktiviert
                    if (!matchesFilter(item, targetNode)) continue;

                    if (moveItem(sourceInv, i, targetNode)) {
                        movedThisSlot = true;
                        anyItemMoved = true;
                        break; // Item wurde bewegt, nächster Slot
                    }
                }
            }
            
            if (!anyItemMoved) {
                sourceNode.setLastFailTime(System.currentTimeMillis());
            }
        }
    }

    private boolean moveItem(Inventory sourceInv, int slot, LagerNode targetNode) {
        if (!isChunkLoaded(targetNode.getLocation())) return false;
        
        Block targetBlock = targetNode.getLocation().getBlock();
        if (!(targetBlock.getState() instanceof Container targetContainer)) return false;
        
        Inventory targetInv = targetContainer.getInventory();
        ItemStack itemToMove = sourceInv.getItem(slot);
        if (itemToMove == null) return false;

        HashMap<Integer, ItemStack> leftover = targetInv.addItem(itemToMove);
        
        if (leftover.isEmpty()) {
            sourceInv.setItem(slot, null);
            return true;
        } else {
            ItemStack remaining = leftover.values().iterator().next();
            if (remaining != null) {
                sourceInv.setItem(slot, remaining);
                return true;
            }
        }
        return false;
    }
    
    private boolean isChunkLoaded(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        return world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private boolean matchesFilter(ItemStack item, LagerNode node) {
        if (node.getFilterMaterials().isEmpty()) return false; // Sicherheit
        boolean isContained = node.getFilterMaterials().contains(item.getType());
        return node.isWhitelist() == isContained;
    }
    
    public Location getNormalizedLocation(Block block) {
        if (block.getState() instanceof Container container) {
             Inventory inv = container.getInventory();
             if (inv.getLocation() != null) {
                 return inv.getLocation();
             }
        }
        return block.getLocation();
    }

    public void createNode(UUID owner, Location loc, LagerNode.Type type) {
        Location normLoc = getNormalizedLocation(loc.getBlock());
        if (nodes.containsKey(normLoc)) return;

        LagerNode node = new LagerNode(type, normLoc, owner);
        registerNode(node);
        markSave();
    }

    private void registerNode(LagerNode node) {
        nodes.put(node.getLocation(), node);
        // Verwirrende Benennung in der Variable, aber logisch korrekt:
        // Type.AUSGANG = Items gehen WEG -> ist die QUELLE (inputs Liste)
        // Type.EINGANG = Items kommen AN -> ist das ZIEL (outputs Liste)
        if (node.getType() == LagerNode.Type.AUSGANG) inputs.add(node);
        else outputs.add(node);
    }
    
    public void switchType(LagerNode node) {
        if (node.getType() == LagerNode.Type.AUSGANG) inputs.remove(node);
        else outputs.remove(node);

        node.setType(node.getType() == LagerNode.Type.AUSGANG ? LagerNode.Type.EINGANG : LagerNode.Type.AUSGANG);

        if (node.getType() == LagerNode.Type.AUSGANG) inputs.add(node);
        else outputs.add(node);
        markSave();
    }
    
    public void removeNode(Location loc) {
        Location normLoc = getNormalizedLocation(loc.getBlock());
        LagerNode node = nodes.remove(normLoc);
        if (node != null) {
            inputs.remove(node);
            outputs.remove(node);
            markSave();
        }
    }

    private void markSave() { needsSave = true; }
    public void save() { performSave(); }

    private void performSave() {
        dataConfig.set("nodes", null);
        for (Map.Entry<Location, LagerNode> entry : nodes.entrySet()) {
            String key = serializeLoc(entry.getKey());
            LagerNode node = entry.getValue();
            
            dataConfig.set("nodes." + key + ".type", node.getType().name());
            dataConfig.set("nodes." + key + ".owner", node.getOwnerId() != null ? node.getOwnerId().toString() : null);
            dataConfig.set("nodes." + key + ".whitelist", node.isWhitelist());
            
            List<String> mats = new ArrayList<>();
            for (Material m : node.getFilterMaterials()) mats.add(m.name());
            dataConfig.set("nodes." + key + ".filter", mats);
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void load() {
        inputs.clear();
        outputs.clear();
        nodes.clear();
        
        ConfigurationSection section = dataConfig.getConfigurationSection("nodes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                Location loc = deserializeLoc(key);
                if (loc == null) continue;
                
                String typeStr = section.getString(key + ".type", "AUSGANG");
                LagerNode.Type type = LagerNode.Type.valueOf(typeStr);
                
                String ownerStr = section.getString(key + ".owner");
                UUID owner = ownerStr != null ? UUID.fromString(ownerStr) : null;

                LagerNode node = new LagerNode(type, loc, owner);
                node.setWhitelist(section.getBoolean(key + ".whitelist", true));
                
                List<String> mats = section.getStringList(key + ".filter");
                for (String s : mats) {
                    try { node.getFilterMaterials().add(Material.valueOf(s)); } catch (Exception ignored) {}
                }
                registerNode(node);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Fehler beim Laden Node: " + key, e);
            }
        }
    }

    public Map<Location, LagerNode> getNodes() { return nodes; }
    public void setTempLocation(UUID uuid, Location loc) { lastInteracted.put(uuid, loc); }
    public Location getTempLocation(UUID uuid) { return lastInteracted.get(uuid); }

    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }
    private Location deserializeLoc(String s) {
        String[] p = s.split(";");
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }
}
