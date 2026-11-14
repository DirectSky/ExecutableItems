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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutableItemsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public ExecutableItemsCommand(Main plugin) {
        this.plugin = plugin;
    }

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
            case "rename"     -> handleRename(player, args);
            case "copy"       -> handleCopy(player);
            case "paste"      -> handlePaste(player);
            default           -> player.sendMessage(plugin.getConfigManager().getMessage("invalid-subcommand"));
        }
        return true;
    }


    private void handleLink(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei link add <command> [player|op]"));
            player.sendMessage(colorize("&7       /ei link remove <index>"));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "add"    -> handleLinkAdd(player, args);
            case "remove" -> handleLinkRemove(player, args);
            default -> {
                player.sendMessage(colorize("&7Usage: /ei link add <command> [player|op]"));
                player.sendMessage(colorize("&7       /ei link remove <index>"));
            }
        }
    }

    private void handleLinkAdd(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(colorize("&7Usage: /ei link add <command> [player|op]"));
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
        if (commandEnd < 3) {
            player.sendMessage(colorize("&7Usage: /ei link add <command> [player|op]"));
            return;
        }

        if (runAs.equals("op")) {
            if (!plugin.getConfigManager().isOpExecutionAllowed()) {
                player.sendMessage(colorize(
                        "&c'op' mode is disabled in config.yml (allow-op-execution: false)."));
                return;
            }
            player.sendMessage(colorize(
                    "&e&l\u26a0 WARNING: &e'op' mode runs the command as the &c&lCONSOLE&e. " +
                            "Only use on items you fully trust!"));
        }

        String newCommand = String.join(" ", Arrays.copyOfRange(args, 2, commandEnd));
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String existingCmd   = pdc.get(plugin.getCommandKey(), PersistentDataType.STRING);
        String existingRunAs = pdc.get(plugin.getRunAsKey(), PersistentDataType.STRING);

        List<String> cmds = (existingCmd == null || existingCmd.isEmpty())
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(existingCmd.split("\n")));

        List<String> runAsList = new ArrayList<>();
        if (existingRunAs != null && !existingRunAs.isEmpty()) {
            String[] parts = existingRunAs.split("\n");
            if (parts.length == 1) {
                for (int i = 0; i < cmds.size(); i++) runAsList.add(parts[0]);
            } else {
                runAsList.addAll(Arrays.asList(parts));
                while (runAsList.size() < cmds.size()) runAsList.add("player");
                while (runAsList.size() > cmds.size()) runAsList.remove(runAsList.size() - 1);
            }
        } else {
            for (int i = 0; i < cmds.size(); i++) runAsList.add("player");
        }

        cmds.add(newCommand);
        runAsList.add(runAs);

        pdc.set(plugin.getCommandKey(), PersistentDataType.STRING, String.join("\n", cmds));
        pdc.set(plugin.getRunAsKey(),  PersistentDataType.STRING, String.join("\n", runAsList));
        item.setItemMeta(meta);

        player.sendMessage(plugin.getConfigManager().getMessage("linked"));
    }

    private void handleLinkRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(colorize("&7Usage: /ei link remove <index>"));
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

        String raw = pdc.get(plugin.getCommandKey(), PersistentDataType.STRING);
        List<String> commands = new ArrayList<>(Arrays.asList(raw.split("\n")));

        String runAsRaw = pdc.get(plugin.getRunAsKey(), PersistentDataType.STRING);
        List<String> runAsList = new ArrayList<>();
        if (runAsRaw != null && !runAsRaw.isEmpty()) {
            String[] parts = runAsRaw.split("\n");
            if (parts.length == 1) {
                for (int i = 0; i < commands.size(); i++) runAsList.add(parts[0]);
            } else {
                runAsList.addAll(Arrays.asList(parts));
                while (runAsList.size() < commands.size()) runAsList.add("player");
                while (runAsList.size() > commands.size()) runAsList.remove(runAsList.size() - 1);
            }
        } else {
            for (int i = 0; i < commands.size(); i++) runAsList.add("player");
        }

        int index;
        try {
            index = Integer.parseInt(args[2]) - 1; 
        } catch (NumberFormatException e) {
            player.sendMessage(colorize("&cInvalid index."));
            return;
        }

        if (index < 0 || index >= commands.size()) {
            player.sendMessage(colorize("&cIndex out of range. Use &f/ei info &cto see current commands."));
            return;
        }

        commands.remove(index);
        if (index < runAsList.size()) runAsList.remove(index);

        if (commands.isEmpty()) {
            pdc.remove(plugin.getCommandKey());
            pdc.remove(plugin.getRunAsKey());
            pdc.remove(plugin.getCooldownKey());
            pdc.remove(plugin.getUsePermissionKey());
            pdc.remove(plugin.getOnActionKey());
            player.sendMessage(plugin.getConfigManager().getMessage("unlinked"));
        } else {
            pdc.set(plugin.getCommandKey(), PersistentDataType.STRING, String.join("\n", commands));
            pdc.set(plugin.getRunAsKey(),  PersistentDataType.STRING, String.join("\n", runAsList));
            player.sendMessage(plugin.getConfigManager().getMessage("link-removed")
                    .replace("%index%", String.valueOf(index + 1)));
        }
        item.setItemMeta(meta);
    }


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
        pdc.remove(plugin.getCommandKey());
        pdc.remove(plugin.getRunAsKey());
        pdc.remove(plugin.getCooldownKey());
        pdc.remove(plugin.getUsePermissionKey());
        pdc.remove(plugin.getOnActionKey());
        item.setItemMeta(meta);
        player.sendMessage(plugin.getConfigManager().getMessage("unlinked"));
    }

    private void handleOnAction(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei onaction <clear <single|slot> | damage <n> | count <n> | drop | remove>"));
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
                if (args.length < 3 || !isPositiveInt(args[2])) {
                    player.sendMessage(colorize("&7Usage: /ei onaction damage <amount>"));
                    return;
                }
                storedValue = "damage:" + args[2];
            }
            case "count" -> {
                if (args.length < 3 || !isPositiveInt(args[2])) {
                    player.sendMessage(colorize("&7Usage: /ei onaction count <amount>"));
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
        player.sendMessage(plugin.getConfigManager().getMessage("onaction-set").replace("%action%", storedValue));
    }

    private void handleCooldown(Player player, String[] args) {
        if (!player.hasPermission("executableitems.link")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(colorize("&7Usage: /ei cooldown <seconds>  (-1 = use global default)"));
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
            player.sendMessage(colorize("&cInvalid number."));
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (seconds < 0) {
            meta.getPersistentDataContainer().remove(plugin.getCooldownKey());
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-reset"));
        } else {
            meta.getPersistentDataContainer().set(plugin.getCooldownKey(), PersistentDataType.INTEGER, seconds);
            item.setItemMeta(meta);
            player.sendMessage(plugin.getConfigManager().getMessage("cooldown-set").replace("%seconds%", String.valueOf(seconds)));
        }
    }

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
            player.sendMessage(plugin.getConfigManager().getMessage("permission-set").replace("%permission%", args[1]));
        }
    }

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
        player.sendMessage(plugin.getConfigManager().getMessage("item-saved").replace("%name%", name));
    }

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
            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found").replace("%player%", args[1]));
            return;
        }
        String name = args[2].toLowerCase();
        if (!plugin.getItemRegistry().exists(name)) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-not-found").replace("%name%", name));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Math.min(64, Integer.parseInt(args[3]))); }
            catch (NumberFormatException e) { player.sendMessage(colorize("&cInvalid amount.")); return; }
        }
        ItemStack item = plugin.getItemRegistry().getItem(name);
        if (item == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-not-found").replace("%name%", name));
            return;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        player.sendMessage(plugin.getConfigManager().getMessage("item-given")
                .replace("%name%", name)
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(amount)));
    }

    private void handleRename(Player player, String[] args) {
        if (!player.hasPermission("executableitems.save")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(colorize("&7Usage: /ei rename <old-name> <new-name>"));
            return;
        }
        String oldName = args[1].toLowerCase();
        String newName = args[2].toLowerCase();
        if (!plugin.getItemRegistry().exists(oldName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-not-found").replace("%name%", oldName));
            return;
        }
        if (plugin.getItemRegistry().exists(newName)) {
            player.sendMessage(plugin.getConfigManager().getMessage("item-name-taken").replace("%name%", newName));
            return;
        }
        plugin.getItemRegistry().renameItem(oldName, newName);
        player.sendMessage(plugin.getConfigManager().getMessage("item-renamed")
                .replace("%old%", oldName).replace("%new%", newName));
    }

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

        String raw      = pdc.get(plugin.getCommandKey(), PersistentDataType.STRING);
        String runAsRaw = pdc.get(plugin.getRunAsKey(), PersistentDataType.STRING);
        String action   = pdc.get(plugin.getOnActionKey(), PersistentDataType.STRING);
        String perm     = pdc.get(plugin.getUsePermissionKey(), PersistentDataType.STRING);
        Integer cdItem  = pdc.get(plugin.getCooldownKey(), PersistentDataType.INTEGER);

        String[] commands = raw.split("\n");
        String[] runAsArr = (runAsRaw == null || runAsRaw.isEmpty())
                ? new String[]{"player"}
                : runAsRaw.split("\n");

        player.sendMessage(colorize("&8&m              &r &bExecutableItems Info &8&m              "));
        player.sendMessage(colorize("&7Command(s):"));
        boolean hasOp = false;
        for (int i = 0; i < commands.length; i++) {
            String cmdRunAs = runAsArr.length == 1
                    ? runAsArr[0]
                    : (i < runAsArr.length ? runAsArr[i] : "player");
            String color = cmdRunAs.equalsIgnoreCase("op") ? "&c" : "&a";
            if (cmdRunAs.equalsIgnoreCase("op")) hasOp = true;
            player.sendMessage(colorize("  &8[&e" + (i + 1) + "&8] &f" + commands[i]
                    + " &8(" + color + cmdRunAs + "&8)"));
        }
        player.sendMessage(colorize("&7Cooldown:   &f" + (cdItem != null
                ? cdItem + "s" : "global (" + plugin.getConfigManager().getCooldown() + "s)")));
        player.sendMessage(colorize("&7Permission: &f" + (perm != null && !perm.isEmpty() ? perm : "none")));
        player.sendMessage(colorize("&7On action:  &f" + (action != null ? action : "none")));
        if (hasOp) {
            player.sendMessage(colorize("&e&l\u26a0 &eThis item contains 'op' commands — they run as &c&lCONSOLE&e!"));
        }
        player.sendMessage(colorize("&8&m                                              "));
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("executableitems.reload")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        plugin.getConfigManager().reload();
        plugin.getItemRegistry().reload();
        player.sendMessage(plugin.getConfigManager().getMessage("reloaded"));
    }

    private void handleHelp(Player player) {
        player.sendMessage(colorize("&8&m              &r &bExecutableItems &7v"
                + plugin.getDescription().getVersion() + " &8by &7"
                + String.join(", ", plugin.getDescription().getAuthors()) + " &8&m              "));
        player.sendMessage(colorize("&e/ei link add <command> [player|op]   &8- &7Add a command to the held item"));
        player.sendMessage(colorize("  &c&l\u26a0 &c'op' mode runs commands as console — only use with trusted items!"));
        player.sendMessage(colorize("&e/ei link remove <index>              &8- &7Remove a command by its index"));
        player.sendMessage(colorize("&e/ei unlink                           &8- &7Remove all EI data from the held item"));
        player.sendMessage(colorize("&e/ei onaction <type> [args]           &8- &7Set what happens to the item on use"));
        player.sendMessage(colorize("  &8> &7clear <single|slot>, damage <n>, count <n>, drop, remove"));
        player.sendMessage(colorize("&e/ei cooldown <seconds>               &8- &7Per-item cooldown (-1 = global)"));
        player.sendMessage(colorize("&e/ei permission <node|remove>         &8- &7Restrict item use to a permission"));
        player.sendMessage(colorize("&e/ei info                             &8- &7Show all linked data of the held item"));
        player.sendMessage(colorize("&e/ei save <name>                      &8- &7Save held item to the registry"));
        player.sendMessage(colorize("&e/ei give <player> <name> [amount]    &8- &7Give a saved item to a player"));
        player.sendMessage(colorize("&e/ei rename <old> <new>               &8- &7Rename a saved item"));
        player.sendMessage(colorize("&e/ei copy                             &8- &7Copy EI data from held item"));
        player.sendMessage(colorize("&e/ei paste                            &8- &7Paste copied EI data onto held item"));
        player.sendMessage(colorize("&e/ei reload                           &8- &7Reload config and items.yml"));
        player.sendMessage(colorize("&8&m                                              "));
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        String current = args[args.length - 1].toLowerCase();

        if (args.length == 1) {
            return filter(List.of("link", "unlink", "info", "help", "reload",
                    "onaction", "cooldown", "permission", "save", "give", "rename", "copy", "paste"), current);
        }

        switch (args[0].toLowerCase()) {
            case "link" -> {
                if (args.length == 2) return filter(List.of("add", "remove"), current);
                if (args.length == 3 && args[1].equalsIgnoreCase("add"))    return filter(List.of("<command>"), current);
                if (args.length == 3 && args[1].equalsIgnoreCase("remove")) return filter(List.of("<index>"), current);
                if (args.length >= 4 && args[1].equalsIgnoreCase("add"))    return filter(List.of("player", "op"), current);
            }
            case "onaction" -> {
                if (args.length == 2) return filter(List.of("clear", "damage", "count", "drop", "remove"), current);
                if (args.length == 3 && args[1].equalsIgnoreCase("clear"))  return filter(List.of("single", "slot"), current);
                if (args.length == 3 && (args[1].equalsIgnoreCase("damage")
                        || args[1].equalsIgnoreCase("count")))              return filter(List.of("<amount>"), current);
            }
            case "cooldown" -> {
                if (args.length == 2) return filter(List.of("0", "5", "10", "30", "60", "-1"), current);
            }
            case "permission" -> {
                if (args.length == 2) return filter(List.of("<permission.node>", "remove"), current);
            }
            case "save" -> {
                if (args.length == 2) return filter(List.of("<name>"), current);
            }
            case "give" -> {
                if (args.length == 2) return filter(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).collect(Collectors.toList()), current);
                if (args.length == 3) {
                    Set<String> names = plugin.getItemRegistry().getAllNames();
                    return filter(names.isEmpty() ? List.of("<name>") : new ArrayList<>(names), current);
                }
                if (args.length == 4) return filter(List.of("1", "16", "32", "64"), current);
            }
            case "rename" -> {
                if (args.length == 2) {
                    Set<String> names = plugin.getItemRegistry().getAllNames();
                    return filter(names.isEmpty() ? List.of("<old-name>") : new ArrayList<>(names), current);
                }
                if (args.length == 3) return filter(List.of("<new-name>"), current);
            }
        }
        return List.of();
    }


    private List<String> filter(List<String> options, String current) {
        if (current.isEmpty()) return options;
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }

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