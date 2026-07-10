package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.Route;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The in-game visual route editor ({@code /sj route edit}).
 * <p>
 * While a player edits a route, its waypoints are shown as numbered
 * text displays and the flight path is drawn with particles (visible
 * only to the editor). The wand changes behaviour in edit mode: left
 * click appends a waypoint at the player's position, right click
 * removes the nearest waypoint.
 */
public final class RouteEditManager implements Listener, Runnable {

    /** Maximum distance (blocks) to a waypoint for right-click removal. */
    private static final double REMOVE_RANGE = 3.0D;
    /** Distance between path particles, in blocks. */
    private static final double LINE_STEP = 0.6D;
    /** Safety cap of particles rendered per player per tick. */
    private static final int MAX_LINE_PARTICLES = 400;

    private final SlimeJumpsPlugin plugin;
    private final Map<UUID, String> editors = new HashMap<>();
    private final Map<String, List<TextDisplay>> markers = new HashMap<>();

    public RouteEditManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Enters edit mode for a route, or leaves it when already editing it. */
    public void toggle(Player player, Route route) {
        UUID id = player.getUniqueId();
        String current = editors.get(id);
        if (current != null && current.equalsIgnoreCase(route.getName())) {
            exit(player);
            plugin.getMessages().send(player, "route-edit-exit", "name", route.getName());
            return;
        }
        if (current != null) {
            exit(player);
        }
        editors.put(id, route.getName());
        refreshMarkers(route);
        plugin.getMessages().send(player, "route-edit-enter", "name", route.getName());
    }

    /** Leaves edit mode, cleaning up markers no one is looking at anymore. */
    public void exit(Player player) {
        String routeName = editors.remove(player.getUniqueId());
        if (routeName != null && !editors.containsValue(routeName)) {
            removeMarkers(routeName);
        }
    }

    /** Whether the player currently edits a route. */
    public boolean isEditing(Player player) {
        return editors.containsKey(player.getUniqueId());
    }

    /**
     * Applies a wand click while in edit mode: left click appends a
     * waypoint at the player's position, right click removes the
     * nearest one within range.
     */
    public void handleWandClick(Player player, boolean leftClick) {
        Route route = plugin.getRouteManager().get(editors.get(player.getUniqueId()));
        if (route == null) {
            exit(player);
            return;
        }

        if (leftClick) {
            route.addWaypoint(Route.Waypoint.of(player.getLocation()));
            plugin.getRouteManager().save();
            refreshMarkers(route);
            plugin.getMessages().send(player, "route-point-added",
                    "name", route.getName(),
                    "count", String.valueOf(route.getWaypoints().size()));
            return;
        }

        int nearest = findNearestWaypoint(player, route);
        if (nearest < 0) {
            plugin.getMessages().send(player, "route-edit-no-point");
            return;
        }
        route.removeWaypoint(nearest);
        plugin.getRouteManager().save();
        refreshMarkers(route);
        plugin.getMessages().send(player, "route-point-removed",
                "name", route.getName(),
                "index", String.valueOf(nearest + 1),
                "count", String.valueOf(route.getWaypoints().size()));
    }

    /** Called when a route is deleted so its editors and markers go away. */
    public void onRouteRemoved(String routeName) {
        editors.values().removeIf(name -> name.equalsIgnoreCase(routeName));
        removeMarkers(routeName);
    }

    /** Ends every edit session and removes all markers (reload/shutdown). */
    public void reset() {
        editors.clear();
        for (String routeName : new ArrayList<>(markers.keySet())) {
            removeMarkers(routeName);
        }
    }

    @Override
    public void run() {
        if (editors.isEmpty()) {
            return;
        }
        Particle particle = parseLineParticle();
        for (Map.Entry<UUID, String> entry : editors.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Route route = plugin.getRouteManager().get(entry.getValue());
            if (player == null || !player.isOnline() || route == null) {
                continue;
            }
            drawPath(player, route.resolveWaypoints(), particle);
        }
    }

    /** Draws the route path with particles visible only to the editor. */
    private void drawPath(Player player, List<Location> points, Particle particle) {
        String world = player.getWorld().getName();
        int budget = MAX_LINE_PARTICLES;

        for (int i = 0; i < points.size() && budget > 0; i++) {
            Location point = points.get(i);
            if (!point.getWorld().getName().equals(world)) {
                continue;
            }
            player.spawnParticle(particle, point.getX(), point.getY() + 0.2D, point.getZ(),
                    3, 0.05D, 0.05D, 0.05D, 0.0D);
            budget -= 3;

            if (i + 1 >= points.size()) {
                continue;
            }
            Location next = points.get(i + 1);
            if (!next.getWorld().getName().equals(world)) {
                continue;
            }
            double dx = next.getX() - point.getX();
            double dy = next.getY() - point.getY();
            double dz = next.getZ() - point.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int steps = (int) (distance / LINE_STEP);
            for (int s = 1; s < steps && budget > 0; s++) {
                double t = s / (double) steps;
                player.spawnParticle(particle,
                        point.getX() + dx * t,
                        point.getY() + 0.2D + dy * t,
                        point.getZ() + dz * t,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                budget--;
            }
        }
    }

    /** Re-creates the numbered waypoint markers of a route. */
    public void refreshMarkers(Route route) {
        removeMarkers(route.getName());
        List<TextDisplay> displays = new ArrayList<>();
        List<Route.Waypoint> waypoints = route.getWaypoints();

        for (int i = 0; i < waypoints.size(); i++) {
            Location location = waypoints.get(i).toLocation();
            if (location == null || !location.getWorld().isChunkLoaded(
                    location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                continue;
            }
            location.add(0.0D, 0.6D, 0.0D);
            TextDisplay display = location.getWorld().spawn(location, TextDisplay.class);
            display.setText("§a§l#" + (i + 1));
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
            displays.add(display);
        }
        markers.put(key(route.getName()), displays);
    }

    private void removeMarkers(String routeName) {
        List<TextDisplay> displays = markers.remove(key(routeName));
        if (displays == null) {
            return;
        }
        for (TextDisplay display : displays) {
            if (display.isValid()) {
                display.remove();
            }
        }
    }

    private int findNearestWaypoint(Player player, Route route) {
        Location origin = player.getLocation();
        List<Route.Waypoint> waypoints = route.getWaypoints();
        int nearest = -1;
        double best = REMOVE_RANGE * REMOVE_RANGE;

        for (int i = 0; i < waypoints.size(); i++) {
            Route.Waypoint waypoint = waypoints.get(i);
            if (!waypoint.world().equals(origin.getWorld().getName())) {
                continue;
            }
            double dx = waypoint.x() - origin.getX();
            double dy = waypoint.y() - origin.getY();
            double dz = waypoint.z() - origin.getZ();
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq <= best) {
                best = distanceSq;
                nearest = i;
            }
        }
        return nearest;
    }

    private Particle parseLineParticle() {
        String name = plugin.getConfig().getString("route-editor.line-particle", "END_ROD");
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Particle.END_ROD;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        exit(event.getPlayer());
    }

    private static String key(String routeName) {
        return routeName.toLowerCase(Locale.ROOT);
    }
}
