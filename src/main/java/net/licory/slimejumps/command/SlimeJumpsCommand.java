package net.licory.slimejumps.command;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import net.licory.slimejumps.util.Messages;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Handles {@code /slimejumps} and its subcommands:
 * create, remove, list, info, tp, setpower, setvertical, reload and help.
 */
public final class SlimeJumpsCommand implements TabExecutor {

    private static final String ADMIN_PERMISSION = "slimejumps.admin";
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "remove", "list", "info", "tp", "setpower", "setvertical", "reload", "help");

    private final SlimeJumpsPlugin plugin;

    public SlimeJumpsCommand(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();

        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(sender, args);
            case "remove", "delete" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "setpower" -> handleSetValue(sender, args, true);
            case "setvertical" -> handleSetValue(sender, args, false);
            case "reload" -> {
                plugin.reload();
                messages.send(sender, "reloaded");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "usage-create");
            return;
        }

        String name = args[1];
        if (!NAME_PATTERN.matcher(name).matches()) {
            messages.send(sender, "invalid-name");
            return;
        }

        double power = plugin.getConfig().getDouble("pads.default-power", 1.6D);
        double vertical = plugin.getConfig().getDouble("pads.default-vertical", 1.0D);
        if (args.length >= 3) {
            try {
                power = clampPower(Double.parseDouble(args[2]));
            } catch (NumberFormatException e) {
                messages.send(sender, "invalid-number", "input", args[2]);
                return;
            }
        }
        if (args.length >= 4) {
            try {
                vertical = clampPower(Double.parseDouble(args[3]));
            } catch (NumberFormatException e) {
                messages.send(sender, "invalid-number", "input", args[3]);
                return;
            }
        }

        // The pad is anchored to the block the player is standing on.
        Location padBlock = player.getLocation().subtract(0.0D, 1.0D, 0.0D);
        JumpPad pad = plugin.getPadManager().create(name, padBlock, power, vertical);
        if (pad == null) {
            messages.send(sender, "pad-exists", "name", name);
            return;
        }
        messages.send(sender, "pad-created",
                "name", pad.getName(),
                "power", format(pad.getPower()),
                "vertical", format(pad.getVertical()));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 2) {
            messages.send(sender, "usage-remove");
            return;
        }
        if (plugin.getPadManager().delete(args[1])) {
            messages.send(sender, "pad-removed", "name", args[1]);
        } else {
            messages.send(sender, "pad-not-found", "name", args[1]);
        }
    }

    private void handleList(CommandSender sender) {
        Messages messages = plugin.getMessages();
        var pads = plugin.getPadManager().getAll();
        if (pads.isEmpty()) {
            messages.send(sender, "list-empty");
            return;
        }
        messages.send(sender, "list-header", "count", String.valueOf(pads.size()));
        for (JumpPad pad : pads) {
            messages.send(sender, "list-entry",
                    "name", pad.getName(),
                    "world", pad.getWorldName(),
                    "x", String.valueOf(pad.getX()),
                    "y", String.valueOf(pad.getY()),
                    "z", String.valueOf(pad.getZ()),
                    "power", format(pad.getPower()),
                    "vertical", format(pad.getVertical()));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 2) {
            messages.send(sender, "usage-info");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        messages.send(sender, "list-entry",
                "name", pad.getName(),
                "world", pad.getWorldName(),
                "x", String.valueOf(pad.getX()),
                "y", String.valueOf(pad.getY()),
                "z", String.valueOf(pad.getZ()),
                "power", format(pad.getPower()),
                "vertical", format(pad.getVertical()));
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "usage-tp");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        Location location = pad.toLocation();
        if (location == null) {
            messages.send(sender, "pad-world-unloaded", "name", pad.getName());
            return;
        }
        player.teleport(location.add(0.5D, 1.0D, 0.5D));
        messages.send(sender, "pad-teleported", "name", pad.getName());
    }

    private void handleSetValue(CommandSender sender, String[] args, boolean isPower) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, isPower ? "usage-setpower" : "usage-setvertical");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        double value;
        try {
            value = clampPower(Double.parseDouble(args[2]));
        } catch (NumberFormatException e) {
            messages.send(sender, "invalid-number", "input", args[2]);
            return;
        }
        if (isPower) {
            pad.setPower(value);
        } else {
            pad.setVertical(value);
        }
        plugin.getPadManager().save();
        messages.send(sender, "pad-updated",
                "name", pad.getName(),
                "setting", isPower ? "power" : "vertical",
                "value", format(value));
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessages().getList("help").forEach(sender::sendMessage);
    }

    private double clampPower(double value) {
        double max = plugin.getConfig().getDouble("pads.max-power", 10.0D);
        return Math.max(-max, Math.min(max, value));
    }

    private static String format(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("remove") || sub.equals("delete") || sub.equals("info")
                    || sub.equals("tp") || sub.equals("teleport")
                    || sub.equals("setpower") || sub.equals("setvertical")) {
                List<String> names = new ArrayList<>();
                for (JumpPad pad : plugin.getPadManager().getAll()) {
                    names.add(pad.getName());
                }
                return filter(names, args[1]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}
