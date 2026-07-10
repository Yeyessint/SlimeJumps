package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import net.licory.slimejumps.model.Route;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads, stores and persists flight routes.
 * <p>
 * Routes are stored in {@code routes.yml} inside the plugin data folder.
 */
public final class RouteManager {

    private final SlimeJumpsPlugin plugin;
    private final Map<String, Route> routes = new LinkedHashMap<>();
    private final File file;

    public RouteManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "routes.yml");
    }

    /** Loads all routes from {@code routes.yml}, replacing the in-memory state. */
    public void load() {
        routes.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("routes");
        if (section == null) {
            return;
        }

        for (String name : section.getKeys(false)) {
            Route route = new Route(name);
            for (String raw : section.getStringList(name + ".waypoints")) {
                Route.Waypoint waypoint = Route.Waypoint.deserialize(raw);
                if (waypoint != null) {
                    route.addWaypoint(waypoint);
                } else {
                    plugin.getLogger().warning("Skipping malformed waypoint in route '" + name + "': " + raw);
                }
            }
            routes.put(name.toLowerCase(Locale.ROOT), route);
        }
    }

    /** Writes all routes to {@code routes.yml}. */
    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Route route : routes.values()) {
            data.set("routes." + route.getName() + ".waypoints",
                    route.getWaypoints().stream().map(Route.Waypoint::serialize).toList());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save routes.yml: " + e.getMessage());
        }
    }

    /**
     * Creates and persists a new route whose first waypoint is the given location.
     *
     * @return the created route, or {@code null} if the name is taken
     */
    public Route create(String name, Location firstWaypoint) {
        String key = name.toLowerCase(Locale.ROOT);
        if (routes.containsKey(key)) {
            return null;
        }
        Route route = new Route(name);
        route.addWaypoint(Route.Waypoint.of(firstWaypoint));
        routes.put(key, route);
        save();
        return route;
    }

    /**
     * Deletes a route by name and unlinks it from any pads that use it.
     *
     * @return {@code true} if a route was removed
     */
    public boolean delete(String name) {
        Route route = routes.remove(name.toLowerCase(Locale.ROOT));
        if (route == null) {
            return false;
        }

        boolean padsChanged = false;
        for (JumpPad pad : plugin.getPadManager().getAll()) {
            if (route.getName().equalsIgnoreCase(pad.getRouteName())) {
                pad.setRouteName(null);
                padsChanged = true;
            }
        }
        if (padsChanged) {
            plugin.getPadManager().save();
        }

        save();
        return true;
    }

    /** Looks up a route by name (case-insensitive). */
    public Route get(String name) {
        if (name == null) {
            return null;
        }
        return routes.get(name.toLowerCase(Locale.ROOT));
    }

    /** All registered routes, in creation order. */
    public Collection<Route> getAll() {
        return Collections.unmodifiableCollection(routes.values());
    }
}
