package de.Sky.executableItems;

import de.Sky.executableItems.commands.ExecutableItemsCommand;
import de.Sky.executableItems.listener.ItemListener;
import de.Sky.executableItems.manager.ConfigManager;
import de.Sky.executableItems.manager.CooldownManager;
import de.Sky.executableItems.manager.ItemRegistry;
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

    // PDC Keys
    private NamespacedKey commandKey;
    private NamespacedKey runAsKey;
    private NamespacedKey cooldownKey;
    private NamespacedKey usePermissionKey;
    private NamespacedKey onActionKey;

    // In-memory copy/paste buffer per player
    private final Map<UUID, ItemStack> copyBuffer = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        commandKey       = new NamespacedKey(this, "command");
        runAsKey         = new NamespacedKey(this, "run-as");
        cooldownKey      = new NamespacedKey(this, "cooldown");
        usePermissionKey = new NamespacedKey(this, "use-permission");
        onActionKey      = new NamespacedKey(this, "onaction");

        configManager   = new ConfigManager(this);
        cooldownManager = new CooldownManager(this);
        itemRegistry    = new ItemRegistry(this);

        ExecutableItemsCommand handler = new ExecutableItemsCommand(this);
        getCommand("executableitems").setExecutor(handler);
        getCommand("executableitems").setTabCompleter(handler);

        getServer().getPluginManager().registerEvents(new ItemListener(this), this);

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