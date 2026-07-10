package net.licory.slimejumps.task;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Locale;

/**
 * Periodically displays ambient particles above every registered pad so
 * players can spot them. Pads in unloaded worlds or chunks are skipped.
 */
public final class ParticleTask implements Runnable {

    private final SlimeJumpsPlugin plugin;

    public ParticleTask(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Particle particle = parseParticle(
                plugin.getConfig().getString("ambient-particles.name", "HAPPY_VILLAGER"));
        int count = plugin.getConfig().getInt("ambient-particles.count", 8);

        for (JumpPad pad : plugin.getPadManager().getAll()) {
            if (!pad.isEnabled()) {
                continue;
            }
            Location location = pad.toLocation();
            if (location == null) {
                continue;
            }
            if (!location.getWorld().isChunkLoaded(pad.getX() >> 4, pad.getZ() >> 4)) {
                continue;
            }
            location.add(0.5D, 1.15D, 0.5D);
            location.getWorld().spawnParticle(particle, location, count, 0.25D, 0.1D, 0.25D, 0.0D);
        }
    }

    private Particle parseParticle(String name) {
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
