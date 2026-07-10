package net.licory.slimejumps.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named flight route: an ordered list of waypoints a player flies
 * through after stepping on a pad linked to the route.
 */
public final class Route {

    /** A single point of a route, independent of world load state. */
    public record Waypoint(String world, double x, double y, double z) {

        /** Resolves this waypoint, or {@code null} if the world is not loaded. */
        public Location toLocation() {
            World resolved = Bukkit.getWorld(world);
            if (resolved == null) {
                return null;
            }
            return new Location(resolved, x, y, z);
        }

        /** Serializes this waypoint to its storage format. */
        public String serialize() {
            return world + ";" + x + ";" + y + ";" + z;
        }

        /**
         * Parses a waypoint from its storage format.
         *
         * @return the waypoint, or {@code null} if the input is malformed
         */
        public static Waypoint deserialize(String input) {
            String[] parts = input.split(";");
            if (parts.length != 4) {
                return null;
            }
            try {
                return new Waypoint(parts[0],
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /** Creates a waypoint from a live location. */
        public static Waypoint of(Location location) {
            return new Waypoint(location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ());
        }
    }

    private final String name;
    private final List<Waypoint> waypoints = new ArrayList<>();

    public Route(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** The waypoints of this route, in flight order (read-only). */
    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
    }

    /**
     * Removes the waypoint at the given zero-based index.
     *
     * @return {@code true} if the index was valid
     */
    public boolean removeWaypoint(int index) {
        if (index < 0 || index >= waypoints.size()) {
            return false;
        }
        waypoints.remove(index);
        return true;
    }

    /**
     * Resolves all waypoints whose worlds are currently loaded.
     * Order is preserved; unresolvable waypoints are skipped.
     */
    public List<Location> resolveWaypoints() {
        List<Location> resolved = new ArrayList<>(waypoints.size());
        for (Waypoint waypoint : waypoints) {
            Location location = waypoint.toLocation();
            if (location != null) {
                resolved.add(location);
            }
        }
        return resolved;
    }
}
