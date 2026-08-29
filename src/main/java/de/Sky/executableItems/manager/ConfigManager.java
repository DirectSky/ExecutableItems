package de.Sky.executableItems.manager;

import de.Sky.executableItems.Main;
import org.bukkit.ChatColor;

public class ConfigManager {

    private final Main plugin;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public int getCooldown() {
        return plugin.getConfig().getInt("cooldown", 5);
    }

    public String getDefaultRunAs() {
        return plugin.getConfig().getString("defaults.run-as", "player").toLowerCase();
    }

    public String getMessage(String key) {
        String prefix = color(plugin.getConfig().getString("messages.prefix", "&8[&bEI&8] "));
        String msg    = plugin.getConfig().getString("messages." + key, "&cMissing message: " + key);
        return prefix + color(msg);
    }

    /** Returns a colorized message without the plugin prefix. */
    public String getMessageRaw(String key) {
        return color(plugin.getConfig().getString("messages." + key, "&cMissing message: " + key));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}