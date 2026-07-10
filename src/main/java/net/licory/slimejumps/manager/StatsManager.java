package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks launch statistics: how many times each pad has been used and
 * the total number of launches on the server.
 * <p>
 * Counters are kept in memory and flushed to {@code stats.yml}
 * periodically and on shutdown, so recording a launch never touches
 * the disk on the hot path.
 */
public final class StatsManager {

    private final SlimeJumpsPlugin plugin;
    private final Map<String, Long> padUses = new HashMap<>();
    private final File file;
    private long totalLaunches;
    private boolean dirty;

    public StatsManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
    }

    /** Loads counters from {@code stats.yml}. */
    public void load() {
        padUses.clear();
        totalLaunches = 0L;
        dirty = false;

        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        totalLaunches = data.getLong("total-launches", 0L);
        ConfigurationSection pads = data.getConfigurationSection("pads");
        if (pads != null) {
            for (String name : pads.getKeys(false)) {
                padUses.put(name, pads.getLong(name, 0L));
            }
        }
    }

    /** Writes counters to {@code stats.yml} if anything changed. */
    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        YamlConfiguration data = new YamlConfiguration();
        data.set("total-launches", totalLaunches);
        for (Map.Entry<String, Long> entry : padUses.entrySet()) {
            data.set("pads." + entry.getKey(), entry.getValue());
        }
        try {
            data.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml: " + e.getMessage());
        }
    }

    /**
     * Records one launch. Slime-block-mode launches pass a {@code null}
     * pad and only increase the server total.
     */
    public void recordLaunch(JumpPad pad) {
        if (!plugin.getConfig().getBoolean("stats.enabled", true)) {
            return;
        }
        totalLaunches++;
        if (pad != null) {
            padUses.merge(pad.getName(), 1L, Long::sum);
        }
        dirty = true;
    }

    /** Forgets the counter of a deleted pad. */
    public void clearPad(String padName) {
        for (String key : new ArrayList<>(padUses.keySet())) {
            if (key.equalsIgnoreCase(padName)) {
                padUses.remove(key);
                dirty = true;
            }
        }
    }

    /** Moves a renamed pad's counter to its new name. */
    public void renamePad(String oldName, String newName) {
        for (String key : new ArrayList<>(padUses.keySet())) {
            if (key.equalsIgnoreCase(oldName)) {
                padUses.merge(newName, padUses.remove(key), Long::sum);
                dirty = true;
            }
        }
    }

    /** Total launches recorded on this server. */
    public long getTotalLaunches() {
        return totalLaunches;
    }

    /** The most used pads, ordered by launch count. */
    public List<Map.Entry<String, Long>> getTopPads(int limit) {
        return padUses.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }
}
