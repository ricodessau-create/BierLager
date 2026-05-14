package de.bierlager;

import de.bierlager.commands.BierLagerCommand;
import de.bierlager.listeners.ChestInteractListener;
import de.bierlager.listeners.ItemProtectListener;
import de.bierlager.listeners.PlayerJoinListener;
import de.bierlager.manager.MessageManager;
import de.bierlager.manager.SortManager;
import de.bierlager.manager.StorageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BierLager extends JavaPlugin {

    private static BierLager instance;
    private StorageManager storageManager;
    private SortManager sortManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("messages.yml", false);

        messageManager = new MessageManager(this);
        storageManager = new StorageManager(this);
        storageManager.load();

        sortManager = new SortManager(this);

        getServer().getPluginManager().registerEvents(new ChestInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getCommand("bierlager").setExecutor(new BierLagerCommand(this));

        long interval = getConfig().getLong("sort-interval-ticks", 40L);
        getServer().getScheduler().runTaskTimer(this, sortManager::runSort, interval, interval);

        getLogger().info("BierLager gestartet.");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) storageManager.save();
        getLogger().info("BierLager gestoppt.");
    }

    public static BierLager getInstance() {
        return instance;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public SortManager getSortManager() {
        return sortManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
