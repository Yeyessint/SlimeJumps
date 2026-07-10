package net.licory.slimejumps.task;

import net.licory.slimejumps.SlimeJumpsPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;

/**
 * Repeatedly draws a previewed path with particles, visible only to
 * the player who requested it, for a configurable number of seconds.
 */
public final class PreviewTask implements Runnable {

    private static final long RENDER_INTERVAL_TICKS = 10L;

    private final Player player;
    private final List<Location> points;
    private final Particle particle;
    private int rendersLeft;
    private BukkitTask task;

    private PreviewTask(SlimeJumpsPlugin plugin, Player player, List<Location> points) {
        this.player = player;
        this.points = points;
        this.particle = parseParticle(plugin.getConfig().getString("preview.particle", "HAPPY_VILLAGER"));
        long durationSeconds = Math.max(1L, plugin.getConfig().getLong("preview.duration-seconds", 6L));
        this.rendersLeft = (int) (durationSeconds * 20L / RENDER_INTERVAL_TICKS);
    }

    /** Starts previewing a path for a player. */
    public static void start(SlimeJumpsPlugin plugin, Player player, List<Location> points) {
        PreviewTask preview = new PreviewTask(plugin, player, points);
        preview.task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, preview, 0L, RENDER_INTERVAL_TICKS);
    }

    @Override
    public void run() {
        if (rendersLeft-- <= 0 || !player.isOnline() || points.isEmpty()) {
            task.cancel();
            return;
        }
        String world = player.getWorld().getName();
        for (Location point : points) {
            if (point.getWorld() != null && point.getWorld().getName().equals(world)) {
                player.spawnParticle(particle, point.getX(), point.getY(), point.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        // Highlight the landing spot.
        Location landing = points.get(points.size() - 1);
        if (landing.getWorld() != null && landing.getWorld().getName().equals(world)) {
            player.spawnParticle(Particle.FIREWORK, landing.getX(), landing.getY(), landing.getZ(),
                    8, 0.2D, 0.2D, 0.2D, 0.02D);
        }
    }

    private static Particle parseParticle(String name) {
        if (name != null) {
            try {
                return Particle.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Fall through to the default particle.
            }
        }
        return Particle.HAPPY_VILLAGER;
    }
}
