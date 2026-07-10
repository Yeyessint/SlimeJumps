package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads, stores and persists jump pads.
 * <p>
 * Pads are stored in {@code pads.yml} inside the plugin data folder and
 * indexed both by name and by block position for fast lookups from the
 * movement listener.
 */
public final class JumpPadManager {

    private final SlimeJumpsPlugin plugin;
    private final Map<String, JumpPad> padsByName = new LinkedHashMap<>();
    private final Map<String, JumpPad> padsByBlock = new HashMap<>();
    private final File file;

    public JumpPadManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pads.yml");
    }

    /** Loads all pads from {@code pads.yml}, replacing the in-memory state. */
    public void load() {
        padsByName.clear();
        padsByBlock.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection pads = data.getConfigurationSection("pads");
        if (pads == null) {
            return;
        }

        double defaultPower = plugin.getConfig().getDouble("pads.default-power", 1.6D);
        double defaultVertical = plugin.getConfig().getDouble("pads.default-vertical", 1.0D);

        for (String name : pads.getKeys(false)) {
            ConfigurationSection section = pads.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            String world = section.getString("world");
            if (world == null || world.isEmpty()) {
                plugin.getLogger().warning("Skipping pad '" + name + "': missing world.");
                continue;
            }
            JumpPad pad = new JumpPad(
                    name,
                    world,
                    section.getInt("x"),
                    section.getInt("y"),
                    section.getInt("z"),
                    section.getDouble("power", defaultPower),
                    section.getDouble("vertical", defaultVertical)
            );
            pad.setRouteName(section.getString("route"));
            if (section.contains("yaw")) {
                pad.setFixedYaw((float) section.getDouble("yaw"));
            }
            pad.setSound(section.getString("sound"));
            pad.setParticle(section.getString("particle"));
            pad.setCommand(section.getString("command"));
            if (section.contains("cooldown")) {
                pad.setCooldownMs(section.getLong("cooldown", 0L));
            }
            pad.setEnabled(section.getBoolean("enabled", true));
            String effect = section.getString("effect");
            if (effect != null) {
                pad.setEffect(effect,
                        section.getInt("effect-duration", 5),
                        section.getInt("effect-amplifier", 0));
            }
            pad.setMessage(section.getString("message"));
            pad.setHologram(section.getString("hologram"));
            if (section.contains("charge")) {
                pad.setChargeMs(section.getLong("charge", 0L));
            }
            index(pad);
        }
    }

    /** Writes all pads to {@code pads.yml}. */
    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (JumpPad pad : padsByName.values()) {
            String path = "pads." + pad.getName();
            data.set(path + ".world", pad.getWorldName());
            data.set(path + ".x", pad.getX());
            data.set(path + ".y", pad.getY());
            data.set(path + ".z", pad.getZ());
            data.set(path + ".power", pad.getPower());
            data.set(path + ".vertical", pad.getVertical());
            data.set(path + ".route", pad.getRouteName());
            data.set(path + ".yaw", pad.getFixedYaw() == null ? null : pad.getFixedYaw().doubleValue());
            data.set(path + ".sound", pad.getSound());
            data.set(path + ".particle", pad.getParticle());
            data.set(path + ".command", pad.getCommand());
            data.set(path + ".cooldown", pad.getCooldownMs());
            if (!pad.isEnabled()) {
                data.set(path + ".enabled", false);
            }
            if (pad.getEffect() != null) {
                data.set(path + ".effect", pad.getEffect());
                data.set(path + ".effect-duration", pad.getEffectDuration());
                data.set(path + ".effect-amplifier", pad.getEffectAmplifier());
            }
            data.set(path + ".message", pad.getMessage());
            data.set(path + ".hologram", pad.getHologram());
            data.set(path + ".charge", pad.getChargeMs());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pads.yml: " + e.getMessage());
        }
    }

    /**
     * Imports pads created by SlimeJumps 1.x, which stored locations in
     * {@code config.yml} under {@code locs.<index>}. Imported pads are
     * named {@code pad<index>} and the legacy keys are removed.
     */
    public void migrateLegacyConfig() {
        if (!plugin.getConfig().contains("createds")) {
            return;
        }

        int count = plugin.getConfig().getInt("createds", 0);
        double defaultPower = plugin.getConfig().getDouble("pads.default-power", 1.6D);
        double defaultVertical = plugin.getConfig().getDouble("pads.default-vertical", 1.0D);
        int imported = 0;

        for (int i = 1; i <= count; i++) {
            String raw = plugin.getConfig().getString("locs." + i);
            if (raw == null) {
                continue;
            }
            String[] parts = raw.split(",");
            if (parts.length < 4) {
                continue;
            }
            try {
                String name = uniqueName("pad" + i);
                JumpPad pad = new JumpPad(
                        name,
                        parts[0],
                        (int) Double.parseDouble(parts[1]),
                        (int) Double.parseDouble(parts[2]),
                        (int) Double.parseDouble(parts[3]),
                        defaultPower,
                        defaultVertical
                );
                index(pad);
                imported++;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Skipping legacy pad entry 'locs." + i + "': " + raw);
            }
        }

        plugin.getConfig().set("createds", null);
        plugin.getConfig().set("locs", null);
        plugin.saveConfig();

        if (imported > 0) {
            save();
            plugin.getLogger().info("Imported " + imported + " pad(s) from the legacy 1.x configuration.");
        }
    }

    /**
     * Creates and persists a new pad.
     *
     * @return the created pad, or {@code null} if the name or block is taken
     */
    public JumpPad create(String name, Location location, double power, double vertical) {
        String key = name.toLowerCase(Locale.ROOT);
        String blockKey = JumpPad.blockKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (padsByName.containsKey(key) || padsByBlock.containsKey(blockKey)) {
            return null;
        }
        JumpPad pad = new JumpPad(name, location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), power, vertical);
        index(pad);
        save();
        return pad;
    }

    /**
     * Deletes a pad by name.
     *
     * @return {@code true} if a pad was removed
     */
    public boolean delete(String name) {
        JumpPad pad = padsByName.remove(name.toLowerCase(Locale.ROOT));
        if (pad == null) {
            return false;
        }
        padsByBlock.remove(pad.blockKey());
        save();
        return true;
    }

    /**
     * Renames a pad, keeping every other setting.
     *
     * @return the renamed pad, or {@code null} if the new name is taken
     */
    public JumpPad rename(JumpPad pad, String newName) {
        if (padsByName.containsKey(newName.toLowerCase(Locale.ROOT))) {
            return null;
        }
        padsByName.remove(pad.getName().toLowerCase(Locale.ROOT));
        padsByBlock.remove(pad.blockKey());

        JumpPad renamed = new JumpPad(newName, pad.getWorldName(),
                pad.getX(), pad.getY(), pad.getZ(), pad.getPower(), pad.getVertical());
        renamed.setRouteName(pad.getRouteName());
        renamed.setFixedYaw(pad.getFixedYaw());
        renamed.setSound(pad.getSound());
        renamed.setParticle(pad.getParticle());
        renamed.setCommand(pad.getCommand());
        renamed.setCooldownMs(pad.getCooldownMs());
        renamed.setEnabled(pad.isEnabled());
        if (pad.getEffect() != null) {
            renamed.setEffect(pad.getEffect(), pad.getEffectDuration(), pad.getEffectAmplifier());
        }
        renamed.setMessage(pad.getMessage());
        renamed.setHologram(pad.getHologram());
        renamed.setChargeMs(pad.getChargeMs());

        index(renamed);
        save();
        return renamed;
    }

    /**
     * Creates a pad with an auto-generated name ({@code pad1}, {@code pad2}, …)
     * and default power values. Used by the wand.
     *
     * @return the created pad, or {@code null} if the block already has one
     */
    public JumpPad createAuto(Location location) {
        int i = padsByName.size() + 1;
        while (padsByName.containsKey("pad" + i)) {
            i++;
        }
        return create("pad" + i, location,
                plugin.getConfig().getDouble("pads.default-power", 1.6D),
                plugin.getConfig().getDouble("pads.default-vertical", 1.0D));
    }

    /** Looks up a pad by name (case-insensitive). */
    public JumpPad get(String name) {
        return padsByName.get(name.toLowerCase(Locale.ROOT));
    }

    /** Looks up the pad occupying the given block, if any. */
    public JumpPad getAt(Block block) {
        return padsByBlock.get(JumpPad.blockKey(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }

    /** Looks up the pad occupying the block at the given location, if any. */
    public JumpPad getAt(Location location) {
        return padsByBlock.get(JumpPad.blockKey(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    /** All registered pads, in creation order. */
    public Collection<JumpPad> getAll() {
        return Collections.unmodifiableCollection(padsByName.values());
    }

    private void index(JumpPad pad) {
        padsByName.put(pad.getName().toLowerCase(Locale.ROOT), pad);
        padsByBlock.put(pad.blockKey(), pad);
    }

    private String uniqueName(String base) {
        String candidate = base;
        int suffix = 1;
        while (padsByName.containsKey(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }
}
