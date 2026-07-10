package net.licory.slimejumps.listener;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.manager.CooldownManager;
import net.licory.slimejumps.model.JumpPad;
import net.licory.slimejumps.model.Route;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
 * Three behaviours are supported:
 * <ul>
 *   <li><b>Directional pads</b> — launch the player towards where they
 *       are looking (or along the pad's fixed direction).</li>
 *   <li><b>Route pads</b> — pads linked to a route hand the player over
 *       to the {@link net.licory.slimejumps.manager.FlightManager}, which
 *       flies them along the route's waypoints.</li>
 *   <li><b>Slime block mode</b> — optionally, every slime block in the
 *       world acts as a directional pad (legacy 1.x behaviour).</li>
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
        if (plugin.getConfig().getStringList("disabled-worlds").contains(to.getWorld().getName())) {
            return;
        }
        if (plugin.getFlightManager().isFlying(player.getUniqueId())) {
            return;
        }
        if (player.isSneaking() && plugin.getConfig().getBoolean("pads.ignore-sneaking", false)) {
            return;
        }

        Block below = to.getWorld().getBlockAt(
                to.getBlockX(),
                (int) Math.floor(to.getY() - 0.0625D),
                to.getBlockZ()
        );

        JumpPad pad = plugin.getPadManager().getAt(below);
        if (pad != null && !pad.isEnabled()) {
            return;
        }
        if (pad == null && !isSlimeBlockPad(below)) {
            return;
        }
        if (!player.hasPermission(USE_PERMISSION)) {
            return;
        }

        long cooldownMs = pad != null && pad.getCooldownMs() != null
                ? pad.getCooldownMs()
                : plugin.getConfig().getLong("pads.cooldown-ms", 500L);
        if (!cooldowns.tryUse(player.getUniqueId(), cooldownMs)) {
            return;
        }

        // Route pads fly the player along their route instead of launching.
        if (pad != null && pad.getRouteName() != null) {
            Route route = plugin.getRouteManager().get(pad.getRouteName());
            if (route != null && plugin.getFlightManager().start(player, route)) {
                playLaunchEffects(player, pad);
                finishUse(player, pad);
                return;
            }
            // Route missing or empty: fall back to a directional launch.
        }

        if (pad != null) {
            launch(player, pad.getPower(), pad.getVertical(), pad.getFixedYaw());
        } else {
            launch(player,
                    plugin.getConfig().getDouble("slime-block-mode.power", 1.6D),
                    plugin.getConfig().getDouble("slime-block-mode.vertical", 1.0D),
                    null);
        }
        playLaunchEffects(player, pad);
        finishUse(player, pad);
    }

    /**
     * Records the launch and applies the pad's extras: console command,
     * potion effect and action bar message.
     */
    private void finishUse(Player player, JumpPad pad) {
        plugin.getStatsManager().recordLaunch(pad);
        if (pad == null) {
            return;
        }

        if (pad.getCommand() != null && !pad.getCommand().isBlank()) {
            String command = pad.getCommand().replace("%player%", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }

        if (pad.getEffect() != null) {
            PotionEffectType type = PotionEffectType.getByName(pad.getEffect());
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type,
                        pad.getEffectDuration() * 20, pad.getEffectAmplifier()));
            }
        }

        if (pad.getMessage() != null && !pad.getMessage().isBlank()) {
            String text = ChatColor.translateAlternateColorCodes('&',
                    pad.getMessage().replace("%player%", player.getName()));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
        }
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
     * Launches the player. The horizontal direction comes from the pad's
     * fixed yaw when set, otherwise from the player's own yaw, so looking
     * straight up or down still produces a valid launch.
     */
    private void launch(Player player, double power, double vertical, Float fixedYaw) {
        double yaw = Math.toRadians(fixedYaw != null ? fixedYaw : player.getLocation().getYaw());
        Vector velocity = new Vector(-Math.sin(yaw), 0.0D, Math.cos(yaw))
                .multiply(power)
                .setY(vertical);
        player.setVelocity(velocity);
        player.setFallDistance(0.0F);

        if (plugin.getConfig().getBoolean("pads.prevent-fall-damage", true)) {
            fallProtected.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    /** Plays launch effects, honouring per-pad sound/particle overrides. */
    private void playLaunchEffects(Player player, JumpPad pad) {
        Location location = player.getLocation();

        if (plugin.getConfig().getBoolean("launch.sound.enabled", true)) {
            String sound = pad != null && pad.getSound() != null
                    ? pad.getSound()
                    : plugin.getConfig().getString("launch.sound.name", "entity.ender_dragon.flap");
            float volume = (float) plugin.getConfig().getDouble("launch.sound.volume", 1.0D);
            float pitch = (float) plugin.getConfig().getDouble("launch.sound.pitch", 1.0D);
            location.getWorld().playSound(location, sound, volume, pitch);
        }

        if (plugin.getConfig().getBoolean("launch.particles.enabled", true)) {
            String name = pad != null && pad.getParticle() != null
                    ? pad.getParticle()
                    : plugin.getConfig().getString("launch.particles.name", "CLOUD");
            Particle particle = parseParticle(name, Particle.CLOUD);
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
