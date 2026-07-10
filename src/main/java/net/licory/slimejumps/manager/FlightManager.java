package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.Route;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Flies players along routes.
 * <p>
 * When a player steps on a pad linked to a route, a flight is started:
 * every tick the player is steered towards the next waypoint at the
 * configured speed, leaving a particle trail. While flying (and for a
 * short grace period after landing) the player is protected from damage.
 */
public final class FlightManager implements Listener, Runnable {

    /** State of one player's active flight. */
    private static final class Flight {
        final List<Location> waypoints;
        int index;
        final long startedAt = System.currentTimeMillis();

        Flight(List<Location> waypoints) {
            this.waypoints = waypoints;
        }
    }

    private final SlimeJumpsPlugin plugin;
    private final Map<UUID, Flight> flights = new HashMap<>();
    private final Map<UUID, Long> landingProtection = new HashMap<>();

    public FlightManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts flying a player along a route.
     *
     * @return {@code true} if the flight was started (the route has at
     *         least one waypoint in a loaded world)
     */
    public boolean start(Player player, Route route) {
        List<Location> waypoints = route.resolveWaypoints();
        if (waypoints.isEmpty()) {
            return false;
        }
        flights.put(player.getUniqueId(), new Flight(waypoints));
        return true;
    }

    /** Whether the player is currently flying along a route. */
    public boolean isFlying(UUID playerId) {
        return flights.containsKey(playerId);
    }

    @Override
    public void run() {
        if (flights.isEmpty()) {
            return;
        }

        double speed = Math.max(0.1D, plugin.getConfig().getDouble("routes.speed", 1.2D));
        long timeoutMs = plugin.getConfig().getLong("routes.timeout-seconds", 30L) * 1000L;

        Iterator<Map.Entry<UUID, Flight>> iterator = flights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Flight> entry = iterator.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Flight flight = entry.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }
            if (System.currentTimeMillis() - flight.startedAt > timeoutMs) {
                iterator.remove();
                release(player, false);
                continue;
            }

            Location target = flight.waypoints.get(flight.index);
            Location current = player.getLocation();

            // Waypoint in a different world: jump there and continue the route.
            if (!current.getWorld().getName().equals(target.getWorld().getName())) {
                player.teleport(target);
                if (!advance(flight)) {
                    iterator.remove();
                    release(player, true);
                }
                continue;
            }

            double dx = target.getX() - current.getX();
            double dy = target.getY() - current.getY();
            double dz = target.getZ() - current.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance <= Math.max(1.0D, speed * 1.5D)) {
                if (!advance(flight)) {
                    iterator.remove();
                    release(player, true);
                }
                continue;
            }

            double factor = speed / distance;
            player.setVelocity(new Vector(dx * factor, dy * factor, dz * factor));
            player.setFallDistance(0.0F);
            spawnTrail(player);
        }
    }

    /** Moves the flight to its next waypoint; returns {@code false} when finished. */
    private boolean advance(Flight flight) {
        flight.index++;
        return flight.index < flight.waypoints.size();
    }

    /** Ends a flight, granting landing protection and playing arrival effects. */
    private void release(Player player, boolean arrived) {
        player.setFallDistance(0.0F);
        long protectionMs = plugin.getConfig().getLong("routes.landing-protection-ms", 5000L);
        landingProtection.put(player.getUniqueId(), System.currentTimeMillis() + protectionMs);

        if (!arrived) {
            return;
        }

        Location location = player.getLocation();
        if (plugin.getConfig().getBoolean("routes.arrival.sound.enabled", true)) {
            String sound = plugin.getConfig().getString("routes.arrival.sound.name", "entity.player.levelup");
            float volume = (float) plugin.getConfig().getDouble("routes.arrival.sound.volume", 1.0D);
            float pitch = (float) plugin.getConfig().getDouble("routes.arrival.sound.pitch", 1.2D);
            location.getWorld().playSound(location, sound, volume, pitch);
        }
        if (plugin.getConfig().getBoolean("routes.arrival.particles.enabled", true)) {
            Particle particle = parseParticle(
                    plugin.getConfig().getString("routes.arrival.particles.name", "FIREWORK"), Particle.FIREWORK);
            int count = plugin.getConfig().getInt("routes.arrival.particles.count", 30);
            location.getWorld().spawnParticle(particle, location, count, 0.4D, 0.6D, 0.4D, 0.1D);
        }
    }

    private void spawnTrail(Player player) {
        if (!plugin.getConfig().getBoolean("routes.trail.enabled", true)) {
            return;
        }
        Particle particle = parseParticle(
                plugin.getConfig().getString("routes.trail.name", "END_ROD"), Particle.END_ROD);
        int count = plugin.getConfig().getInt("routes.trail.count", 3);
        Location location = player.getLocation();
        location.getWorld().spawnParticle(particle, location, count, 0.15D, 0.15D, 0.15D, 0.0D);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();

        // Immune to everything while flying a route.
        if (flights.containsKey(id)
                && plugin.getConfig().getBoolean("routes.protect-during-flight", true)) {
            event.setCancelled(true);
            return;
        }

        // Fall damage grace period right after landing.
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long until = landingProtection.remove(id);
            if (until != null && System.currentTimeMillis() <= until) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        flights.remove(id);
        landingProtection.remove(id);
    }

    private static Particle parseParticle(String name, Particle fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
