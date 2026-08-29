package de.Sky.executableItems.manager;

import de.Sky.executableItems.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Main plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CooldownManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if a player is currently on cooldown.
     */
    public boolean isOnCooldown(UUID playerId) {
        int cooldownSeconds = plugin.getConfigManager().getCooldown();
        if (cooldownSeconds <= 0) return false;

        if (!cooldowns.containsKey(playerId)) return false;

        long lastUse = cooldowns.get(playerId);
        long elapsed = System.currentTimeMillis() - lastUse;
        return elapsed < (long) cooldownSeconds * 1000;
    }

    /**
     * Returns the remaining cooldown in seconds (rounded up).
     */
    public long getRemainingSeconds(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) return 0;

        int cooldownSeconds = plugin.getConfigManager().getCooldown();
        long lastUse = cooldowns.get(playerId);
        long elapsed = System.currentTimeMillis() - lastUse;
        long remaining = ((long) cooldownSeconds * 1000) - elapsed;
        return Math.max(0, (long) Math.ceil(remaining / 1000.0));
    }

    /**
     * Sets the cooldown for a player to now.
     */
    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Removes a player's cooldown entry.
     */
    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
