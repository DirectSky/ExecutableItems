package de.Sky.executableItems.listener;

import de.Sky.executableItems.Main;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemListener implements Listener {

    private final Main plugin;

    public ItemListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        // Right-click only, main hand only
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir() || !item.hasItemMeta()) return;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(plugin.getCommandKey(), PersistentDataType.STRING)) return;

        String linkedCommand = pdc.get(plugin.getCommandKey(), PersistentDataType.STRING);
        String runAs         = pdc.getOrDefault(plugin.getRunAsKey(), PersistentDataType.STRING, "player");
        String onAction      = pdc.get(plugin.getOnActionKey(), PersistentDataType.STRING);

        // --- Permission checks ---
        if (!player.hasPermission("executableitems.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        String usePermission = pdc.get(plugin.getUsePermissionKey(), PersistentDataType.STRING);
        if (usePermission != null && !usePermission.isEmpty()
                && !player.hasPermission(usePermission)) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        // --- Cooldown check (FIXED) ---
        if (!player.hasPermission("executableitems.bypass.cooldown")
                && plugin.getCooldownManager().isOnCooldown(player.getUniqueId())) {

            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown")
                    .replace("%seconds%", String.valueOf(remaining)));
            return;
        }

        // --- Execute command ---
        String finalCommand = linkedCommand.replace("%player%", player.getName());

        if (runAs.equalsIgnoreCase("op")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCommand);
        } else {
            plugin.getServer().dispatchCommand(player, finalCommand);
        }

        // --- Set cooldown (FIXED) ---
        plugin.getCooldownManager().setCooldown(player.getUniqueId());

        // --- Apply onAction ---
        if (onAction != null && !onAction.isEmpty()) {
            applyOnAction(player, onAction);
        }
    }

    // -------------------------------------------------------------------------

    private void applyOnAction(Player player, String onAction) {
        String[] parts = onAction.split(":", 2);
        String type = parts[0].toLowerCase();
        String arg  = parts.length > 1 ? parts[1] : "";

        switch (type) {

            case "clear" -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (arg.equals("slot")) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    if (item.getAmount() <= 1) {
                        player.getInventory().setItemInMainHand(null);
                    } else {
                        item.setAmount(item.getAmount() - 1);
                    }
                }
            }

            case "count" -> {
                int amount = parseIntSafe(arg, 1);
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getAmount() <= amount) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(item.getAmount() - amount);
                }
            }

            case "damage" -> {
                int dmg = parseIntSafe(arg, 1);
                ItemStack item = player.getInventory().getItemInMainHand();
                ItemMeta meta = item.getItemMeta();

                if (meta instanceof Damageable damageable) {
                    int newDamage = damageable.getDamage() + dmg;

                    if (newDamage >= item.getType().getMaxDurability()) {
                        player.getInventory().setItemInMainHand(null);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    } else {
                        damageable.setDamage(newDamage);
                        item.setItemMeta(meta);
                    }
                }
            }

            case "drop" -> {
                ItemStack item = player.getInventory().getItemInMainHand().clone();
                player.getInventory().setItemInMainHand(null);
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
    }

    private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
