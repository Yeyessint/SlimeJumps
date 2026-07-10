package net.licory.slimejumps.command;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import net.licory.slimejumps.model.Route;
import net.licory.slimejumps.util.Messages;
import org.bukkit.Location;
import org.bukkit.Particle;
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
 * Handles {@code /slimejumps} and its subcommands: pad management
 * (create, remove, list, info, tp, setpower, setvertical, setroute,
 * setdirection, setsound, setparticle), route management
 * ({@code /sj route ...}), reload and help.
 */
public final class SlimeJumpsCommand implements TabExecutor {

    private static final String ADMIN_PERMISSION = "slimejumps.admin";
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "remove", "list", "info", "tp", "near", "stats",
            "setpower", "setvertical", "setcooldown", "setcommand",
            "setroute", "setdirection", "setsound", "setparticle", "route", "reload", "help");
    private static final List<String> ROUTE_SUBCOMMANDS = Arrays.asList(
            "create", "addpoint", "delpoint", "remove", "list", "info");
    private static final List<String> PAD_NAME_SUBCOMMANDS = Arrays.asList(
            "remove", "delete", "info", "tp", "teleport", "setpower", "setvertical",
            "setcooldown", "setcommand", "setroute", "setdirection", "setsound", "setparticle");
    private static final double DEFAULT_NEAR_RADIUS = 20.0D;

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
            case "setcooldown" -> handleSetCooldown(sender, args);
            case "setcommand" -> handleSetCommand(sender, args);
            case "near" -> handleNear(sender, args);
            case "stats" -> handleStats(sender);
            case "setroute" -> handleSetRoute(sender, args);
            case "setdirection" -> handleSetDirection(sender, args);
            case "setsound" -> handleSetSound(sender, args);
            case "setparticle" -> handleSetParticle(sender, args);
            case "route" -> handleRoute(sender, args);
            case "reload" -> {
                plugin.reload();
                messages.send(sender, "reloaded");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Pad subcommands
    // ------------------------------------------------------------------

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
            plugin.getStatsManager().clearPad(args[1]);
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
            sendPadLine(sender, pad);
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
        sendPadLine(sender, pad);
        messages.send(sender, "pad-detail",
                "route", pad.getRouteName() != null ? pad.getRouteName() : "-",
                "direction", pad.getFixedYaw() != null ? "fixed (" + format(pad.getFixedYaw()) + "°)" : "look",
                "sound", pad.getSound() != null ? pad.getSound() : "default",
                "particle", pad.getParticle() != null ? pad.getParticle() : "default",
                "cooldown", pad.getCooldownMs() != null ? pad.getCooldownMs() + "ms" : "default",
                "command", pad.getCommand() != null ? pad.getCommand() : "-");
    }

    private void sendPadLine(CommandSender sender, JumpPad pad) {
        plugin.getMessages().send(sender, "list-entry",
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

    private void handleSetCooldown(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-setcooldown");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        if (args[2].equalsIgnoreCase("default")) {
            pad.setCooldownMs(null);
            plugin.getPadManager().save();
            messages.send(sender, "pad-updated",
                    "name", pad.getName(), "setting", "cooldown", "value", "default");
            return;
        }
        long cooldown;
        try {
            cooldown = Math.max(0L, Long.parseLong(args[2]));
        } catch (NumberFormatException e) {
            messages.send(sender, "invalid-number", "input", args[2]);
            return;
        }
        pad.setCooldownMs(cooldown);
        plugin.getPadManager().save();
        messages.send(sender, "pad-updated",
                "name", pad.getName(), "setting", "cooldown", "value", cooldown + "ms");
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-setcommand");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        if (args.length == 3 && args[2].equalsIgnoreCase("none")) {
            pad.setCommand(null);
            plugin.getPadManager().save();
            messages.send(sender, "pad-command-cleared", "name", pad.getName());
            return;
        }
        String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        pad.setCommand(command);
        plugin.getPadManager().save();
        messages.send(sender, "pad-command-set", "name", pad.getName(), "command", command);
    }

    private void handleNear(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        double radius = DEFAULT_NEAR_RADIUS;
        if (args.length >= 2) {
            try {
                radius = Math.max(1.0D, Double.parseDouble(args[1]));
            } catch (NumberFormatException e) {
                messages.send(sender, "invalid-number", "input", args[1]);
                return;
            }
        }

        record NearPad(JumpPad pad, double distance) {}
        Location origin = player.getLocation();
        List<NearPad> nearby = new ArrayList<>();
        for (JumpPad pad : plugin.getPadManager().getAll()) {
            if (!pad.getWorldName().equals(origin.getWorld().getName())) {
                continue;
            }
            double dx = pad.getX() + 0.5D - origin.getX();
            double dy = pad.getY() + 0.5D - origin.getY();
            double dz = pad.getZ() + 0.5D - origin.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance <= radius) {
                nearby.add(new NearPad(pad, distance));
            }
        }

        if (nearby.isEmpty()) {
            messages.send(sender, "near-empty", "radius", format(radius));
            return;
        }
        nearby.sort(java.util.Comparator.comparingDouble(NearPad::distance));
        messages.send(sender, "near-header",
                "count", String.valueOf(nearby.size()), "radius", format(radius));
        for (NearPad entry : nearby) {
            messages.send(sender, "near-entry",
                    "name", entry.pad().getName(),
                    "distance", format(entry.distance()),
                    "x", String.valueOf(entry.pad().getX()),
                    "y", String.valueOf(entry.pad().getY()),
                    "z", String.valueOf(entry.pad().getZ()));
        }
    }

    private void handleStats(CommandSender sender) {
        Messages messages = plugin.getMessages();
        var stats = plugin.getStatsManager();
        messages.send(sender, "stats-header");
        messages.send(sender, "stats-total",
                "total", String.valueOf(stats.getTotalLaunches()));
        var top = stats.getTopPads(10);
        if (top.isEmpty()) {
            messages.send(sender, "stats-empty");
            return;
        }
        int rank = 1;
        for (var entry : top) {
            messages.send(sender, "stats-entry",
                    "rank", String.valueOf(rank++),
                    "name", entry.getKey(),
                    "uses", String.valueOf(entry.getValue()));
        }
    }

    private void handleSetRoute(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-setroute");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }

        if (args[2].equalsIgnoreCase("none")) {
            pad.setRouteName(null);
            plugin.getPadManager().save();
            messages.send(sender, "pad-route-cleared", "name", pad.getName());
            return;
        }

        Route route = plugin.getRouteManager().get(args[2]);
        if (route == null) {
            messages.send(sender, "route-not-found", "name", args[2]);
            return;
        }
        pad.setRouteName(route.getName());
        plugin.getPadManager().save();
        messages.send(sender, "pad-route-set", "name", pad.getName(), "route", route.getName());
    }

    private void handleSetDirection(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-setdirection");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }

        String mode = args[2].toLowerCase(Locale.ROOT);
        if (mode.equals("look")) {
            pad.setFixedYaw(null);
            plugin.getPadManager().save();
            messages.send(sender, "pad-direction-look", "name", pad.getName());
        } else if (mode.equals("here")) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "players-only");
                return;
            }
            float yaw = player.getLocation().getYaw();
            pad.setFixedYaw(yaw);
            plugin.getPadManager().save();
            messages.send(sender, "pad-direction-fixed",
                    "name", pad.getName(), "yaw", format(yaw));
        } else {
            messages.send(sender, "usage-setdirection");
        }
    }

    private void handleSetSound(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-setsound");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        if (args[2].equalsIgnoreCase("default")) {
            pad.setSound(null);
            plugin.getPadManager().save();
            messages.send(sender, "pad-updated",
                    "name", pad.getName(), "setting", "sound", "value", "default");
            return;
        }
        pad.setSound(args[2].toLowerCase(Locale.ROOT));
        plugin.getPadManager().save();
        messages.send(sender, "pad-updated",
                "name", pad.getName(), "setting", "sound", "value", pad.getSound());
    }

    private void handleSetParticle(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-setparticle");
            return;
        }
        JumpPad pad = plugin.getPadManager().get(args[1]);
        if (pad == null) {
            messages.send(sender, "pad-not-found", "name", args[1]);
            return;
        }
        if (args[2].equalsIgnoreCase("default")) {
            pad.setParticle(null);
            plugin.getPadManager().save();
            messages.send(sender, "pad-updated",
                    "name", pad.getName(), "setting", "particle", "value", "default");
            return;
        }

        String name = args[2].toUpperCase(Locale.ROOT);
        try {
            Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            messages.send(sender, "invalid-particle", "input", args[2]);
            return;
        }
        pad.setParticle(name);
        plugin.getPadManager().save();
        messages.send(sender, "pad-updated",
                "name", pad.getName(), "setting", "particle", "value", name);
    }

    // ------------------------------------------------------------------
    // Route subcommands
    // ------------------------------------------------------------------

    private void handleRoute(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 2) {
            messages.send(sender, "usage-route");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> handleRouteCreate(sender, args);
            case "addpoint" -> handleRouteAddPoint(sender, args);
            case "delpoint" -> handleRouteDelPoint(sender, args);
            case "remove", "delete" -> handleRouteRemove(sender, args);
            case "list" -> handleRouteList(sender);
            case "info" -> handleRouteInfo(sender, args);
            default -> messages.send(sender, "usage-route");
        }
    }

    private void handleRouteCreate(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "usage-route-create");
            return;
        }
        String name = args[2];
        if (!NAME_PATTERN.matcher(name).matches()) {
            messages.send(sender, "invalid-name");
            return;
        }
        Route route = plugin.getRouteManager().create(name, player.getLocation());
        if (route == null) {
            messages.send(sender, "route-exists", "name", name);
            return;
        }
        messages.send(sender, "route-created", "name", route.getName());
    }

    private void handleRouteAddPoint(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "usage-route-addpoint");
            return;
        }
        Route route = plugin.getRouteManager().get(args[2]);
        if (route == null) {
            messages.send(sender, "route-not-found", "name", args[2]);
            return;
        }
        route.addWaypoint(Route.Waypoint.of(player.getLocation()));
        plugin.getRouteManager().save();
        messages.send(sender, "route-point-added",
                "name", route.getName(),
                "count", String.valueOf(route.getWaypoints().size()));
    }

    private void handleRouteDelPoint(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 4) {
            messages.send(sender, "usage-route-delpoint");
            return;
        }
        Route route = plugin.getRouteManager().get(args[2]);
        if (route == null) {
            messages.send(sender, "route-not-found", "name", args[2]);
            return;
        }
        int index;
        try {
            index = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            messages.send(sender, "invalid-number", "input", args[3]);
            return;
        }
        if (!route.removeWaypoint(index - 1)) {
            messages.send(sender, "route-invalid-index",
                    "name", route.getName(),
                    "count", String.valueOf(route.getWaypoints().size()));
            return;
        }
        plugin.getRouteManager().save();
        messages.send(sender, "route-point-removed",
                "name", route.getName(),
                "index", String.valueOf(index),
                "count", String.valueOf(route.getWaypoints().size()));
    }

    private void handleRouteRemove(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-route-remove");
            return;
        }
        if (plugin.getRouteManager().delete(args[2])) {
            messages.send(sender, "route-removed", "name", args[2]);
        } else {
            messages.send(sender, "route-not-found", "name", args[2]);
        }
    }

    private void handleRouteList(CommandSender sender) {
        Messages messages = plugin.getMessages();
        var routes = plugin.getRouteManager().getAll();
        if (routes.isEmpty()) {
            messages.send(sender, "route-list-empty");
            return;
        }
        messages.send(sender, "route-list-header", "count", String.valueOf(routes.size()));
        for (Route route : routes) {
            messages.send(sender, "route-list-entry",
                    "name", route.getName(),
                    "count", String.valueOf(route.getWaypoints().size()));
        }
    }

    private void handleRouteInfo(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        if (args.length < 3) {
            messages.send(sender, "usage-route-info");
            return;
        }
        Route route = plugin.getRouteManager().get(args[2]);
        if (route == null) {
            messages.send(sender, "route-not-found", "name", args[2]);
            return;
        }
        messages.send(sender, "route-info-header",
                "name", route.getName(),
                "count", String.valueOf(route.getWaypoints().size()));
        int index = 1;
        for (Route.Waypoint waypoint : route.getWaypoints()) {
            messages.send(sender, "route-info-point",
                    "index", String.valueOf(index++),
                    "world", waypoint.world(),
                    "x", format(waypoint.x()),
                    "y", format(waypoint.y()),
                    "z", format(waypoint.z()));
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Tab completion
    // ------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("route")) {
                return filter(ROUTE_SUBCOMMANDS, args[1]);
            }
            if (PAD_NAME_SUBCOMMANDS.contains(sub)) {
                return filter(padNames(), args[1]);
            }
        }
        if (args.length == 3) {
            switch (sub) {
                case "setroute" -> {
                    List<String> options = new ArrayList<>(routeNames());
                    options.add("none");
                    return filter(options, args[2]);
                }
                case "setdirection" -> {
                    return filter(Arrays.asList("look", "here"), args[2]);
                }
                case "setcooldown" -> {
                    return filter(List.of("default"), args[2]);
                }
                case "setcommand" -> {
                    return filter(List.of("none"), args[2]);
                }
                case "setparticle" -> {
                    List<String> options = new ArrayList<>();
                    options.add("default");
                    for (Particle particle : Particle.values()) {
                        options.add(particle.name());
                    }
                    return filter(options, args[2]);
                }
                case "setsound" -> {
                    return filter(Arrays.asList("default", "entity.ender_dragon.flap",
                            "entity.firework_rocket.launch", "entity.bat.takeoff",
                            "block.piston.extend", "entity.player.levelup"), args[2]);
                }
                case "route" -> {
                    String routeSub = args[1].toLowerCase(Locale.ROOT);
                    if (routeSub.equals("addpoint") || routeSub.equals("delpoint")
                            || routeSub.equals("remove") || routeSub.equals("delete")
                            || routeSub.equals("info")) {
                        return filter(routeNames(), args[2]);
                    }
                }
                default -> {
                    return List.of();
                }
            }
        }
        return List.of();
    }

    private List<String> padNames() {
        List<String> names = new ArrayList<>();
        for (JumpPad pad : plugin.getPadManager().getAll()) {
            names.add(pad.getName());
        }
        return names;
    }

    private List<String> routeNames() {
        List<String> names = new ArrayList<>();
        for (Route route : plugin.getRouteManager().getAll()) {
            names.add(route.getName());
        }
        return names;
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
