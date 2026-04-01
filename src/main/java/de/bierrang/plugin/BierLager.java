package de.bierrang.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class BierLager extends JavaPlugin {

    private static BierLager instance;
    private LagerManager lagerManager;

    @Override
    public void onEnable() {
        instance = this;
        
        lagerManager = new LagerManager(this);
        lagerManager.load();

        getCommand("bierlager").setExecutor(new LagerCommand(this));
        getServer().getPluginManager().registerEvents(new LagerListener(this), this);

        getLogger().info("BierLager v3.2.1 geladen!");
    }

    @Override
    public void onDisable() {
        if (lagerManager != null) {
            lagerManager.save();
        }
    }

    public static BierLager getInstance() {
        return instance;
    }

    public LagerManager getLagerManager() {
        return lagerManager;
    }
}
