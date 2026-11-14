package de.Sky.executableItems.manager;

import de.Sky.executableItems.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Main plugin;
    private final NamespacedKey cooldownIdKey;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CooldownManager(Main plugin) {
        this.plugin = plugin;
        this.cooldownIdKey = new NamespacedKey(plugin, "cooldown-id");
    }

    private String getItemId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return "global";
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(cooldownIdKey, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            pdc.set(cooldownIdKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return id;
    }

    public int getEffectiveCooldown(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            Integer perItem = item.getItemMeta().getPersistentDataContainer()
                    .get(plugin.getCooldownKey(), PersistentDataType.INTEGER);
            if (perItem != null) return perItem;
        }
        return plugin.getConfigManager().getCooldown();
    }

    public boolean isOnCooldown(UUID playerId, ItemStack item) {
        int seconds = getEffectiveCooldown(item);
        if (seconds <= 0) return false;
        Map<String, Long> map = cooldowns.get(playerId);
        if (map == null) return false;
        Long last = map.get(getItemId(item));
        if (last == null) return false;
        return System.currentTimeMillis() - last < seconds * 1000L;
    }

    public long getRemainingSeconds(UUID playerId, ItemStack item) {
        Map<String, Long> map = cooldowns.get(playerId);
        if (map == null) return 0;
        Long last = map.get(getItemId(item));
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = (long) getEffectiveCooldown(item) * 1000L - elapsed;
        return Math.max(0, (long) Math.ceil(remaining / 1000.0));
    }

    public void setCooldown(UUID playerId, ItemStack item) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(getItemId(item), System.currentTimeMillis());
    }

    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}