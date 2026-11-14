package de.Sky.executableItems;

import de.Sky.executableItems.commands.ExecutableItemsCommand;
import de.Sky.executableItems.listener.ItemListener;
import de.Sky.executableItems.manager.ConfigManager;
import de.Sky.executableItems.manager.CooldownManager;
import de.Sky.executableItems.manager.ItemRegistry;
import org.bstats.bukkit.Metrics;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private ItemRegistry itemRegistry;
    private NamespacedKey commandKey;
    private NamespacedKey runAsKey;
    private NamespacedKey cooldownKey;
    private NamespacedKey usePermissionKey;
    private NamespacedKey onActionKey;

    private final Map<UUID, ItemStack> copyBuffer = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("Loading Config..");
        try {
            saveDefaultConfig();
        } catch (Exception e) {
            getLogger().severe("Failed to load config.yml. This will cause the plugin to crash! Caused in: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Config loaded.");

        try {
            commandKey       = new NamespacedKey(this, "command");
            runAsKey         = new NamespacedKey(this, "run-as");
            cooldownKey      = new NamespacedKey(this, "cooldown");
            usePermissionKey = new NamespacedKey(this, "use-permission");
            onActionKey      = new NamespacedKey(this, "onaction");
        } catch (Exception e) {
            getLogger().severe("Failed to create NamespacedKeys! Caused in: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            configManager   = new ConfigManager(this);
            cooldownManager = new CooldownManager(this);
            itemRegistry    = new ItemRegistry(this);
        } catch (Exception e) {
            getLogger().severe("Failed to initialise managers! Caused in: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Registering Commands..");
        if (getCommand("executableitems") == null) {
            getLogger().severe("Command 'executableitems' is missing from plugin.yml! Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            ExecutableItemsCommand handler = new ExecutableItemsCommand(this);
            getCommand("executableitems").setExecutor(handler);
            getCommand("executableitems").setTabCompleter(handler);
        } catch (Exception e) {
            getLogger().severe("Failed to register commands! Caused in: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        try {
            getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        } catch (Exception e) {
            getLogger().severe("Failed to register ItemListener! Caused in: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Enabling bStats..");
        try {
            int pluginID = 35319;
            new Metrics(this, pluginID);
            getLogger().info("Hooked into bStats");
        } catch (Exception e) {
            getLogger().warning("Failed to hook into bStats: " + e.getMessage());
        }

        getLogger().info("ExecutableItems enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ExecutableItems disabled.");
    }

    public static Main getInstance()             { return instance; }
    public ConfigManager getConfigManager()      { return configManager; }
    public CooldownManager getCooldownManager()  { return cooldownManager; }
    public ItemRegistry getItemRegistry()        { return itemRegistry; }
    public Map<UUID, ItemStack> getCopyBuffer()  { return copyBuffer; }

    public NamespacedKey getCommandKey()         { return commandKey; }
    public NamespacedKey getRunAsKey()           { return runAsKey; }
    public NamespacedKey getCooldownKey()        { return cooldownKey; }
    public NamespacedKey getUsePermissionKey()   { return usePermissionKey; }
    public NamespacedKey getOnActionKey()        { return onActionKey; }
}