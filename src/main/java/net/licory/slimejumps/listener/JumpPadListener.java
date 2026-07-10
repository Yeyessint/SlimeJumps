package net.licory.slimejumps.listener;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.manager.CooldownManager;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Detects players stepping on jump pads and launches them.
 * <p>
 * Two detection modes are supported:
 * <ul>
 *   <li><b>Registered pads</b> — named pads created with
 *       {@code /slimejumps create}, launched with their own power.</li>
 *   <li><b>Slime block mode</b> — optionally, every slime block in the
 *       world acts as a pad (legacy 1.x behaviour).</li>
 * </ul>
 */
public final class JumpPadListener implements Listener {

    private static final String USE_PERMISSION = "slimejumps.use";

    private final SlimeJumpsPlugin plugin;
    private final CooldownManager cooldowns = new CooldownManager();
    private final Map<UUID, Long> fallProtected = new HashMap<>();

    public JumpPadListener(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block below = to.getWorld().getBlockAt(
                to.getBlockX(),
                (int) Math.floor(to.getY() - 0.0625D),
                to.getBlockZ()
        );

        double power;
        double vertical;

        JumpPad pad = plugin.getPadManager().getAt(below);
        if (pad != null) {
            power = pad.getPower();
            vertical = pad.getVertical();
        } else if (isSlimeBlockPad(below)) {
            power = plugin.getConfig().getDouble("slime-block-mode.power", 1.6D);
            vertical = plugin.getConfig().getDouble("slime-block-mode.vertical", 1.0D);
        } else {
            return;
        }

        if (!player.hasPermission(USE_PERMISSION)) {
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("pads.cooldown-ms", 500L);
        if (!cooldowns.tryUse(player.getUniqueId(), cooldownMs)) {
            return;
        }

        launch(player, power, vertical);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL
                || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Long launchedAt = fallProtected.remove(player.getUniqueId());
        if (launchedAt == null) {
            return;
        }
        long protectionMs = plugin.getConfig().getLong("pads.fall-protection-ms", 10000L);
        if (System.currentTimeMillis() - launchedAt <= protectionMs) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        cooldowns.clear(id);
        fallProtected.remove(id);
    }

    private boolean isSlimeBlockPad(Block block) {
        return plugin.getConfig().getBoolean("slime-block-mode.enabled", false)
                && block.getType() == Material.SLIME_BLOCK;
    }

    /**
     * Launches the player in the direction they are facing.
     * The horizontal direction is derived from the yaw only, so looking
     * straight up or down still produces a valid launch.
     */
    private void launch(Player player, double power, double vertical) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Vector velocity = new Vector(-Math.sin(yaw), 0.0D, Math.cos(yaw))
                .multiply(power)
                .setY(vertical);
        player.setVelocity(velocity);
        player.setFallDistance(0.0F);

        if (plugin.getConfig().getBoolean("pads.prevent-fall-damage", true)) {
            fallProtected.put(player.getUniqueId(), System.currentTimeMillis());
        }

        playLaunchEffects(player);
    }

    private void playLaunchEffects(Player player) {
        Location location = player.getLocation();

        if (plugin.getConfig().getBoolean("launch.sound.enabled", true)) {
            String sound = plugin.getConfig().getString("launch.sound.name", "entity.ender_dragon.flap");
            float volume = (float) plugin.getConfig().getDouble("launch.sound.volume", 1.0D);
            float pitch = (float) plugin.getConfig().getDouble("launch.sound.pitch", 1.0D);
            location.getWorld().playSound(location, sound, volume, pitch);
        }

        if (plugin.getConfig().getBoolean("launch.particles.enabled", true)) {
            Particle particle = parseParticle(
                    plugin.getConfig().getString("launch.particles.name", "CLOUD"), Particle.CLOUD);
            int count = plugin.getConfig().getInt("launch.particles.count", 20);
            location.getWorld().spawnParticle(particle, location, count, 0.3D, 0.2D, 0.3D, 0.05D);
        }
    }

    private Particle parseParticle(String name, Particle fallback) {
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
