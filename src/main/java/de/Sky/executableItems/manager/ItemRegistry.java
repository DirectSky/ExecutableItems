package de.Sky.executableItems.manager;

import de.Sky.executableItems.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;

public class ItemRegistry {

    private final Main plugin;
    private File itemsFile;
    private FileConfiguration itemsConfig;

    public ItemRegistry(Main plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            try {
                itemsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create items.yml: " + e.getMessage());
            }
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public void reload() {
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    /**
     * Saves an ItemStack (including all PDC data) under the given name.
     */
    public void saveItem(String name, ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        itemsConfig.set("items." + name, Base64.getEncoder().encodeToString(bytes));
        save();
    }

    /**
     * Returns a fresh copy of the saved item, or null if not found.
     */
    public ItemStack getItem(String name) {
        String base64 = itemsConfig.getString("items." + name);
        if (base64 == null) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(base64));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize item '" + name + "': " + e.getMessage());
            return null;
        }
    }

    public boolean exists(String name) {
        return itemsConfig.contains("items." + name);
    }

    public void removeItem(String name) {
        itemsConfig.set("items." + name, null);
        save();
    }

    public Set<String> getAllNames() {
        if (!itemsConfig.isConfigurationSection("items")) return Collections.emptySet();
        return itemsConfig.getConfigurationSection("items").getKeys(false);
    }

    private void save() {
        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save items.yml: " + e.getMessage());
        }
    }
}
