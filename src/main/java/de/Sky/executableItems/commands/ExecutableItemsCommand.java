package de.Sky.executableItems.commands;

import de.Sky.executableItems.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutableItemsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public ExecutableItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // Dispatch
    // =========================================================================

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("invalid-subcommand"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "link"       -> handleLink(player, args);
            case "unlink"     -> handleUnlink(player);
            case "info"       -> handleInfo(player);
            case "help"       -> handleHelp(player);
            case "reload"     -> handleReload(player);
            case "onaction"   -> handleOnAction(player, args);
            case "cooldown"   -> handleCooldown(player, args);
            case "permission" -> handlePermission(player, args);
            case "save"       -> handleSave(player, args);
            case "give"       -> handleGive(player, args);
            case "copy"       -> handleCopy(player);
            case "paste"      -> handlePaste(player);
            default           -> player.sendMessage(plugin.getConfigManager().getMessage("invalid-subcommand"));
        }

        return true;
    }

    // =========================================================================
    // Subcommands
    // =========================================================================

    // --- /ei link <command> [player|op] --------------------------------------

    private void handleLink(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei link <command> [player|op]"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }

        String runAs = plugin.getConfigManager().getDefaultRunAs();
        int commandEnd = args.length;
        String lastArg = args[args.length - 1].toLowerCase();
        if (lastArg.equals("player") || lastArg.equals("op")) {
            runAs = lastArg;
            commandEnd = args.length - 1;
        }
        if (commandEnd < 2) {
            player.sendMessage(colorize("&7Usage: /ei link <command> [player|op]"));
            return;
        }

        String linkedCommand = String.join(" ", Arrays.copyOfRange(args, 1, commandEnd));

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(plugin.getCommandKey(), PersistentDataType.STRING, linkedCommand);
        meta.getPersistentDataContainer().set(plugin.getRunAsKey(), PersistentDataType.STRING, runAs);
        item.setItemMeta(meta);

        player.sendMessage(plugin.getConfigManager().getMessage("linked"));
    }

    // --- /ei unlink ----------------------------------------------------------

    private void handleUnlink(Player player) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        // Remove all EI data
        pdc.remove(plugin.getCommandKey());
        pdc.remove(plugin.getRunAsKey());
        pdc.remove(plugin.getCooldownKey());
        pdc.remove(plugin.getUsePermissionKey());
        pdc.remove(plugin.getOnActionKey());
        item.setItemMeta(meta);

        player.sendMessage(plugin.getConfigManager().getMessage("unlinked"));
    }

    // --- /ei onaction <clear <single|slot> | damage <amount> | count <amount> | drop | remove> ---

    private void handleOnAction(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei onaction <clear <single|slot> | damage <amount> | count <amount> | drop | remove>"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        String actionType = args[1].toLowerCase();
        ItemMeta meta = item.getItemMeta();

        if (actionType.equals("remove")) {
            meta.getPersistentDataContainer().remove(plugin.getOnActionKey());
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("onaction-removed"));
            return;
        }

        String storedValue;
        switch (actionType) {
            case "clear" -> {
                String mode = args.length >= 3 ? args[2].toLowerCase() : "single";
                if (!mode.equals("single") && !mode.equals("slot")) {
                    player.sendMessage(colorize("&7Usage: /ei onaction clear <single|slot>"));
                    return;
                }
                storedValue = "clear:" + mode;
            }
            case "damage" -> {
                if (args.length < 3) {
                    player.sendMessage(colorize("&7Usage: /ei onaction damage <amount>"));
                    return;
                }
                if (!isPositiveInt(args[2])) {
                    player.sendMessage(colorize("&cAmount must be a positive number."));
                    return;
                }
                storedValue = "damage:" + args[2];
            }
            case "count" -> {
                if (args.length < 3) {
                    player.sendMessage(colorize("&7Usage: /ei onaction count <amount>"));
                    return;
                }
                if (!isPositiveInt(args[2])) {
                    player.sendMessage(colorize("&cAmount must be a positive number."));
                    return;
                }
                storedValue = "count:" + args[2];
            }
            case "drop" -> storedValue = "drop";
            default -> {
                player.sendMessage(colorize("&cUnknown action. Valid: clear, damage, count, drop, remove"));
                return;
            }
        }

        meta.getPersistentDataContainer().set(plugin.getOnActionKey(), PersistentDataType.STRING, storedValue);
        item.setItemMeta(meta);
        player.sendMessage(plugin.getConfigManager().getMessage("onaction-set")
                .replace("%action%", storedValue));
    }

    // --- /ei cooldown <seconds> ----------------------------------------------

    private void handleCooldown(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei cooldown <seconds>  (&7-1 = use global default)"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&cInvalid number. Use -1 to reset to global default."));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (seconds < 0) {
            // -1 = remove override, fall back to global
            meta.getPersistentDataContainer().remove(plugin.getCooldownKey());
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-reset"));
        } else {
            meta.getPersistentDataContainer().set(plugin.getCooldownKey(), PersistentDataType.INTEGER, seconds);
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-set")
                    .replace("%seconds%", String.valueOf(seconds)));
        }
    }

    // --- /ei permission <node|remove> ----------------------------------------

    private void handlePermission(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei permission <node>  (or 'remove' to clear)"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (args[1].equalsIgnoreCase("remove")) {
            meta.getPersistentDataContainer().remove(plugin.getUsePermissionKey());
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("permission-removed"));
        } else {
            meta.getPersistentDataContainer().set(plugin.getUsePermissionKey(), PersistentDataType.STRING, args[1]);
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("permission-set")
                    .replace("%permission%", args[1]));
        }
    }

    // --- /ei save <name> -----------------------------------------------------

    private void handleSave(Player player, String[] args) {
        if (!player.hasPermission("executableitems.save")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei save <name>"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        String name = args[1].toLowerCase();
        plugin.getItemRegistry().saveItem(name, item);
        player.sendMessage(plugin.getConfigManager().getMessage("item-saved")
                .replace("%name%", name));
    }

    // --- /ei give <player> <name> [amount] -----------------------------------

    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("executableitems.give")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(colorize("&7Usage: /ei give <player> <name> [amount]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found")
                    .replace("%player%", args[1]));
            return;
        }

        String name = args[2].toLowerCase();
        if (!plugin.getItemRegistry().exists(name)) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-not-found")
                    .replace("%name%", name));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                player.sendMessage(colorize("&cInvalid amount."));
                return;
            }
        }

        ItemStack item = plugin.getItemRegistry().getItem(name);
        item.setAmount(amount);
        target.getInventory().addItem(item);

        player.sendMessage(plugin.getConfigManager().getMessage("item-given")
                .replace("%name%", name)
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(amount)));
    }

    // --- /ei copy ------------------------------------------------------------

    private void handleCopy(Player player) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        plugin.getCopyBuffer().put(player.getUniqueId(), item.clone());
        player.sendMessage(plugin.getConfigManager().getMessage("copied"));
    }

    // --- /ei paste -----------------------------------------------------------

    private void handlePaste(Player player) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        ItemStack source = plugin.getCopyBuffer().get(player.getUniqueId());
        if (source == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("nothing-copied"));
            return;
        }

        ItemStack target = player.getInventory().getItemInMainHand();
        if (target.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }

        // Transfer all EI PDC keys from source to target
        PersistentDataContainer sourcePdc = source.getItemMeta().getPersistentDataContainer();
        ItemMeta targetMeta = target.getItemMeta();
        PersistentDataContainer targetPdc = targetMeta.getPersistentDataContainer();

        copyPdcString(sourcePdc, targetPdc, plugin.getCommandKey());
        copyPdcString(sourcePdc, targetPdc, plugin.getRunAsKey());
        copyPdcString(sourcePdc, targetPdc, plugin.getOnActionKey());
        copyPdcString(sourcePdc, targetPdc, plugin.getUsePermissionKey());
        copyPdcInt(sourcePdc, targetPdc, plugin.getCooldownKey());

        target.setItemMeta(targetMeta);
        player.sendMessage(plugin.getConfigManager().getMessage("pasted"));
    }

    // --- /ei info ------------------------------------------------------------

    private void handleInfo(Player player) {
        if (!player.hasPermission("executableitems.info")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-item-in-hand"));
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(plugin.getCommandKey(), PersistentDataType.STRING)) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-linked"));
            return;
        }

        String cmd     = pdc.get(plugin.getCommandKey(), PersistentDataType.STRING);
        String runAs   = pdc.getOrDefault(plugin.getRunAsKey(), PersistentDataType.STRING, "player");
        String action  = pdc.get(plugin.getOnActionKey(), PersistentDataType.STRING);
        String perm    = pdc.get(plugin.getUsePermissionKey(), PersistentDataType.STRING);
        Integer cdItem = pdc.get(plugin.getCooldownKey(), PersistentDataType.INTEGER);

        player.sendMessage(colorize("&8&m              &r &bExecutableItems Info &8&m              "));
        player.sendMessage(colorize("&7Command:    &f" + cmd));
        player.sendMessage(colorize("&7Run as:     &f" + runAs));
        player.sendMessage(colorize("&7Cooldown:   &f" + (cdItem != null ? cdItem + "s" : "global (" + plugin.getConfigManager().getCooldown() + "s)")));
        player.sendMessage(colorize("&7Permission: &f" + (perm != null && !perm.isEmpty() ? perm : "none")));
        player.sendMessage(colorize("&7On action:  &f" + (action != null ? action : "none")));
        player.sendMessage(colorize("&8&m                                              "));
    }

    // --- /ei reload ----------------------------------------------------------

    private void handleReload(Player player) {
        if (!player.hasPermission("executableitems.reload")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        plugin.getConfigManager().reload();
        plugin.getItemRegistry().reload();
        player.sendMessage(plugin.getConfigManager().getMessage("reloaded"));
    }

    // --- /ei help ------------------------------------------------------------

    private void handleHelp(Player player) {
        player.sendMessage(colorize("&8&m              &r &bExecutableItems &7v"
                + plugin.getDescription().getVersion() + " &8by &7"
                + String.join(", ", plugin.getDescription().getAuthors()) + " &8&m              "));
        player.sendMessage(colorize("&e/ei link <command> [player|op]  &8- &7Link a command to the held item"));
        player.sendMessage(colorize("&e/ei unlink  &8- &7Remove all EI data from the held item"));
        player.sendMessage(colorize("&e/ei onaction <type> [args]  &8- &7Set what happens to the item on use"));
        player.sendMessage(colorize("  &8> &7clear <single|slot>, damage <n>, count <n>, drop, remove"));
        player.sendMessage(colorize("&e/ei cooldown <seconds>  &8- &7Per-item cooldown (-1 = global default)"));
        player.sendMessage(colorize("&e/ei permission <node|remove>  &8- &7Restrict item use to a permission"));
        player.sendMessage(colorize("&e/ei info  &8- &7Show all linked data of the held item"));
        player.sendMessage(colorize("&e/ei save <name>  &8- &7Save held item to the registry"));
        player.sendMessage(colorize("&e/ei give <player> <name> [amount]  &8- &7Give a saved item to a player"));
        player.sendMessage(colorize("&e/ei copy  &8- &7Copy EI data from held item"));
        player.sendMessage(colorize("&e/ei paste  &8- &7Paste copied EI data onto held item"));
        player.sendMessage(colorize("&e/ei reload  &8- &7Reload config and items.yml"));
        player.sendMessage(colorize("&8&m                                              "));
    }

    // =========================================================================
    // Tab completion
    // =========================================================================

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("link", "unlink", "info", "help", "reload",
                    "onaction", "cooldown", "permission", "save", "give", "copy", "paste");
        }

        switch (args[0].toLowerCase()) {
            case "link" -> {
                if (args.length == 2) return List.of("<command>");
                return List.of("player", "op");
            }
            case "onaction" -> {
                if (args.length == 2) return List.of("clear", "damage", "count", "drop", "remove");
                if (args.length == 3 && args[1].equalsIgnoreCase("clear")) return List.of("single", "slot");
                if (args.length == 3 && (args[1].equalsIgnoreCase("damage")
                        || args[1].equalsIgnoreCase("count"))) return List.of("<amount>");
            }
            case "cooldown" -> {
                if (args.length == 2) return List.of("0", "5", "10", "30", "60", "-1");
            }
            case "permission" -> {
                if (args.length == 2) return List.of("<permission.node>", "remove");
            }
            case "save" -> {
                if (args.length == 2) return List.of("<name>");
            }
            case "give" -> {
                if (args.length == 2) return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).collect(Collectors.toList());
                if (args.length == 3) {
                    Set<String> names = plugin.getItemRegistry().getAllNames();
                    return names.isEmpty() ? List.of("<name>") : new ArrayList<>(names);
                }
                if (args.length == 4) return List.of("1", "16", "32", "64");
            }
        }

        return List.of();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void copyPdcString(PersistentDataContainer src, PersistentDataContainer dst,
                               org.bukkit.NamespacedKey key) {
        String val = src.get(key, PersistentDataType.STRING);
        if (val != null) dst.set(key, PersistentDataType.STRING, val);
        else dst.remove(key);
    }

    private void copyPdcInt(PersistentDataContainer src, PersistentDataContainer dst,
                            org.bukkit.NamespacedKey key) {
        Integer val = src.get(key, PersistentDataType.INTEGER);
        if (val != null) dst.set(key, PersistentDataType.INTEGER, val);
        else dst.remove(key);
    }

    private boolean isPositiveInt(String s) {
        try { return Integer.parseInt(s) > 0; } catch (NumberFormatException e) { return false; }
    }

    private String colorize(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}