package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Spawns and maintains hologram text displays above pads.
 * <p>
 * Holograms are non-persistent {@link TextDisplay} entities: they are
 * spawned when the plugin enables (or when their chunk loads) and are
 * removed on shutdown, so no orphaned entities are ever saved to the
 * world.
 */
public final class HologramManager implements Listener {

    private final SlimeJumpsPlugin plugin;
    private final Map<String, TextDisplay> holograms = new HashMap<>();

    public HologramManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Spawns holograms for every pad whose chunk is currently loaded. */
    public void spawnAll() {
        for (JumpPad pad : plugin.getPadManager().getAll()) {
            trySpawn(pad);
        }
    }

    /** Removes every hologram entity (used on shutdown and reload). */
    public void removeAll() {
        for (TextDisplay display : holograms.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        holograms.clear();
    }

    /** Spawns the pad's hologram if it has text and its chunk is loaded. */
    public void trySpawn(JumpPad pad) {
        if (pad.getHologram() == null) {
            return;
        }
        String key = key(pad.getName());
        TextDisplay existing = holograms.get(key);
        if (existing != null && existing.isValid()) {
            return;
        }

        Location location = pad.toLocation();
        if (location == null
                || !location.getWorld().isChunkLoaded(pad.getX() >> 4, pad.getZ() >> 4)) {
            return;
        }

        double height = plugin.getConfig().getDouble("holograms.height", 1.6D);
        location.add(0.5D, height, 0.5D);

        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class);
        display.setText(ChatColor.translateAlternateColorCodes('&',
                pad.getHologram().replace("|", "\n")));
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        holograms.put(key, display);
    }

    /** Removes the hologram of a pad by name, if one is live. */
    public void remove(String padName) {
        TextDisplay display = holograms.remove(key(padName));
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /** Re-creates a pad's hologram after its text changed. */
    public void refresh(JumpPad pad) {
        remove(pad.getName());
        trySpawn(pad);
    }

    /**
     * Non-persistent displays disappear when their chunk unloads, so they
     * are re-spawned as soon as the chunk comes back.
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        String world = event.getChunk().getWorld().getName();

        for (JumpPad pad : plugin.getPadManager().getAll()) {
            if (pad.getHologram() != null
                    && pad.getWorldName().equals(world)
                    && (pad.getX() >> 4) == chunkX
                    && (pad.getZ() >> 4) == chunkZ) {
                trySpawn(pad);
            }
        }
    }

    private static String key(String padName) {
        return padName.toLowerCase(Locale.ROOT);
    }
}
