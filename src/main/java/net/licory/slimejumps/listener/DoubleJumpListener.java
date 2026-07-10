package net.licory.slimejumps.listener;

import net.licory.slimejumps.SlimeJumpsPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lobby-style double jump: pressing the jump key in mid-air boosts the
 * player in the direction they are looking.
 * <p>
 * The classic trick is used: flight is "armed" (allow-flight) while the
 * player stands on the ground, and the flight-toggle sent by the client
 * on the second jump press is intercepted and converted into a boost.
 * Only players this listener armed itself are affected, so real flight
 * granted by other plugins (e.g. {@code /fly}) keeps working.
 */
public final class DoubleJumpListener implements Listener {

    private static final String PERMISSION = "slimejumps.doublejump";

    private final SlimeJumpsPlugin plugin;
    /** Players whose allow-flight was armed by this listener. */
    private final Set<UUID> armed = new HashSet<>();
    private final Map<UUID, Long> lastJump = new HashMap<>();
    private final Map<UUID, Long> fallProtected = new HashMap<>();

    public DoubleJumpListener(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!armed.remove(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        if (!isEligible(player)) {
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("double-jump.cooldown-ms", 500L);
        long now = System.currentTimeMillis();
        Long last = lastJump.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return;
        }
        lastJump.put(player.getUniqueId(), now);

        double power = plugin.getConfig().getDouble("double-jump.power", 0.9D);
        double vertical = plugin.getConfig().getDouble("double-jump.vertical", 0.9D);
        double yaw = Math.toRadians(player.getLocation().getYaw());
        player.setVelocity(new Vector(-Math.sin(yaw), 0.0D, Math.cos(yaw))
                .multiply(power)
                .setY(vertical));
        player.setFallDistance(0.0F);

        if (plugin.getConfig().getBoolean("double-jump.prevent-fall-damage", true)) {
            fallProtected.put(player.getUniqueId(), now);
        }

        playEffects(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isEligible(player)) {
            return;
        }
        // Re-arm the double jump once the player is back on the ground.
        if (player.isOnGround() && !player.getAllowFlight()) {
            player.setAllowFlight(true);
            armed.add(player.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Long jumpedAt = fallProtected.remove(player.getUniqueId());
        if (jumpedAt == null) {
            return;
        }
        long protectionMs = plugin.getConfig().getLong("pads.fall-protection-ms", 10000L);
        if (System.currentTimeMillis() - jumpedAt <= protectionMs) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (armed.remove(id)) {
            player.setAllowFlight(false);
        }
        lastJump.remove(id);
        fallProtected.remove(id);
    }

    private boolean isEligible(Player player) {
        if (!plugin.getConfig().getBoolean("double-jump.enabled", false)) {
            return false;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return false;
        }
        return player.hasPermission(PERMISSION);
    }

    private void playEffects(Player player) {
        Location location = player.getLocation();

        if (plugin.getConfig().getBoolean("double-jump.sound.enabled", true)) {
            String sound = plugin.getConfig().getString("double-jump.sound.name", "entity.bat.takeoff");
            float volume = (float) plugin.getConfig().getDouble("double-jump.sound.volume", 1.0D);
            float pitch = (float) plugin.getConfig().getDouble("double-jump.sound.pitch", 1.0D);
            location.getWorld().playSound(location, sound, volume, pitch);
        }

        if (plugin.getConfig().getBoolean("double-jump.particles.enabled", true)) {
            Particle particle;
            try {
                particle = Particle.valueOf(plugin.getConfig()
                        .getString("double-jump.particles.name", "CLOUD").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                particle = Particle.CLOUD;
            }
            int count = plugin.getConfig().getInt("double-jump.particles.count", 15);
            location.getWorld().spawnParticle(particle, location, count, 0.3D, 0.1D, 0.3D, 0.05D);
        }
    }
}
