package net.licory.slimejumps.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates the ballistic path of a launched player so it can be
 * previewed with particles ({@code /sj preview}).
 * <p>
 * Uses the vanilla per-tick approximation for airborne players:
 * vertical velocity {@code (v - 0.08) * 0.98} (gravity + drag) and
 * horizontal velocity {@code v * 0.91} (air friction).
 */
public final class TrajectorySimulator {

    private static final int MAX_TICKS = 120;
    private static final double MAX_DROP = 40.0D;

    private TrajectorySimulator() {
    }

    /**
     * Simulates a launch and returns one position per tick, ending
     * early when the path hits a solid block or falls too far.
     */
    public static List<Location> simulate(Location start, float yawDegrees, double power, double vertical) {
        World world = start.getWorld();
        double yaw = Math.toRadians(yawDegrees);
        double vx = -Math.sin(yaw) * power;
        double vz = Math.cos(yaw) * power;
        double vy = vertical;
        double x = start.getX();
        double y = start.getY();
        double z = start.getZ();

        List<Location> points = new ArrayList<>();
        for (int tick = 0; tick < MAX_TICKS; tick++) {
            x += vx;
            y += vy;
            z += vz;
            vy = (vy - 0.08D) * 0.98D;
            vx *= 0.91D;
            vz *= 0.91D;

            points.add(new Location(world, x, y, z));

            if (world.getBlockAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z))
                    .getType().isSolid()) {
                break;
            }
            if (y < start.getY() - MAX_DROP) {
                break;
            }
        }
        return points;
    }

    /**
     * Interpolates extra positions between consecutive waypoints so a
     * route can be previewed as a continuous particle line.
     */
    public static List<Location> interpolate(List<Location> waypoints, double step) {
        List<Location> points = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            Location point = waypoints.get(i);
            points.add(point);
            if (i + 1 >= waypoints.size()) {
                break;
            }
            Location next = waypoints.get(i + 1);
            if (!next.getWorld().getName().equals(point.getWorld().getName())) {
                continue;
            }
            double dx = next.getX() - point.getX();
            double dy = next.getY() - point.getY();
            double dz = next.getZ() - point.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int steps = (int) (distance / step);
            for (int s = 1; s < steps; s++) {
                double t = s / (double) steps;
                points.add(new Location(point.getWorld(),
                        point.getX() + dx * t,
                        point.getY() + dy * t,
                        point.getZ() + dz * t));
            }
        }
        return points;
    }
}
