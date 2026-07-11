package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.Route;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        int ticks;
        int stuckTicks;
        Location lastPosition;
        final long startedAt = System.currentTimeMillis();

        Flight(List<Location> waypoints) {
            this.waypoints = waypoints;
        }
    }

    private final SlimeJumpsPlugin plugin;
    private final Map<UUID, Flight> flights = new HashMap<>();
    private final Map<UUID, Long> landingProtection = new HashMap<>();
    /** Players who quit mid-flight; protected from fall damage on rejoin. */
    private final Set<UUID> protectOnRejoin = new HashSet<>();

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
                } else {
                    flight.stuckTicks = 0;
                }
                continue;
            }

            // Collision handling: if the player barely moved last tick, they
            // are pressed against geometry — steer over or around it.
            updateStuck(flight, current, speed);
            if (flight.stuckTicks > 0
                    && plugin.getConfig().getBoolean("routes.collision.enabled", true)
                    && handleCollision(player, flight, target, speed)) {
                spawnTrail(player);
                continue;
            }

            double factor = speed / distance;
            player.setVelocity(new Vector(dx * factor, dy * factor, dz * factor));
            player.setFallDistance(0.0F);
            spawnTrail(player);
            playFlightSound(player, flight);
        }
    }

    /**
     * Tracks how many consecutive ticks the player has failed to make
     * real progress, which indicates a collision with a wall or ceiling.
     */
    private void updateStuck(Flight flight, Location current, double speed) {
        Location last = flight.lastPosition;
        if (last != null && last.getWorld() == current.getWorld()) {
            double mdx = current.getX() - last.getX();
            double mdy = current.getY() - last.getY();
            double mdz = current.getZ() - last.getZ();
            double moved = Math.sqrt(mdx * mdx + mdy * mdy + mdz * mdz);
            if (moved < speed * 0.35D) {
                flight.stuckTicks++;
            } else {
                flight.stuckTicks = 0;
            }
        }
        flight.lastPosition = current.clone();
    }

    /**
     * Recovers from a collision. First tries to fly up and over the
     * obstacle; if the player stays stuck for too long (e.g. boxed in),
     * teleport-hops a short distance towards the next waypoint to keep
     * the route going.
     *
     * @return {@code true} if this method already handled the player's
     *         movement this tick
     */
    private boolean handleCollision(Player player, Flight flight, Location target, double speed) {
        int climbTicks = plugin.getConfig().getInt("routes.collision.climb-ticks", 8);
        Location current = player.getLocation();

        if (flight.stuckTicks <= climbTicks) {
            // Rise over the wall while keeping a little push towards the target.
            double climb = plugin.getConfig().getDouble("routes.collision.climb-power", 0.6D);
            double dx = target.getX() - current.getX();
            double dz = target.getZ() - current.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            Vector velocity = horizontal < 0.01D
                    ? new Vector(0.0D, climb, 0.0D)
                    : new Vector(dx / horizontal * speed * 0.35D, climb, dz / horizontal * speed * 0.35D);
            player.setVelocity(velocity);
            player.setFallDistance(0.0F);
            return true;
        }

        // Still stuck after climbing: hop towards the waypoint through the
        // obstacle, but only land in open space.
        double dx = target.getX() - current.getX();
        double dy = target.getY() - current.getY();
        double dz = target.getZ() - current.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > 0.01D) {
            double hop = Math.min(distance, Math.max(2.0D, speed * 2.0D));
            Location destination = current.clone().add(
                    dx / distance * hop, dy / distance * hop, dz / distance * hop);
            destination.setYaw(current.getYaw());
            destination.setPitch(current.getPitch());
            if (!destination.getBlock().getType().isSolid()) {
                player.teleport(destination);
                player.setFallDistance(0.0F);
            } else {
                // No safe hop: skip to the next waypoint so the route continues.
                advance(flight);
            }
        }
        flight.stuckTicks = 0;
        return true;
    }

    /** Plays the periodic whoosh while a player flies a route. */
    private void playFlightSound(Player player, Flight flight) {
        if (!plugin.getConfig().getBoolean("routes.flight-sound.enabled", true)) {
            return;
        }
        int interval = Math.max(1, plugin.getConfig().getInt("routes.flight-sound.interval-ticks", 8));
        if (flight.ticks++ % interval != 0) {
            return;
        }
        String sound = plugin.getConfig().getString("routes.flight-sound.name", "entity.phantom.flap");
        float volume = (float) plugin.getConfig().getDouble("routes.flight-sound.volume", 0.7D);
        float pitch = (float) plugin.getConfig().getDouble("routes.flight-sound.pitch", 1.0D);
        Location location = player.getLocation();
        location.getWorld().playSound(location, sound, volume, pitch);
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
        // A player who logs out mid-flight would drop from the sky and take
        // fall damage on rejoin, so remember to protect their landing.
        if (flights.remove(id) != null) {
            protectOnRejoin.add(id);
        }
        landingProtection.remove(id);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (protectOnRejoin.remove(id)) {
            long protectionMs = plugin.getConfig().getLong("routes.landing-protection-ms", 5000L);
            landingProtection.put(id, System.currentTimeMillis() + Math.max(protectionMs, 8000L));
        }
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
