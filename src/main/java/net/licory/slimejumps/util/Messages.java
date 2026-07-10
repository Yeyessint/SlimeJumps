package net.licory.slimejumps.util;

import net.licory.slimejumps.SlimeJumpsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Loads and serves translatable plugin messages.
 * <p>
 * Messages are read from {@code messages_&lt;language&gt;.yml} in the data
 * folder, where {@code language} comes from {@code config.yml}. English
 * and Spanish files are shipped by default; missing keys fall back to the
 * bundled English messages.
 */
public final class Messages {

    private static final String DEFAULT_LANGUAGE = "en";

    private final SlimeJumpsPlugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private String prefix = "";

    public Messages(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)loads the message file selected in {@code config.yml}. */
    public void load() {
        saveDefault("messages_en.yml");
        saveDefault("messages_es.yml");

        String language = plugin.getConfig().getString("language", DEFAULT_LANGUAGE)
                .toLowerCase(Locale.ROOT);
        File file = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file for '" + language + "' not found, using English.");
            file = new File(plugin.getDataFolder(), "messages_" + DEFAULT_LANGUAGE + ".yml");
        }

        messages = YamlConfiguration.loadConfiguration(file);
        applyBundledDefaults();
        prefix = color(messages.getString("prefix", "&7SlimeJumps &a» "));
    }

    /**
     * Returns a colored message with {@code {placeholder}} values applied.
     * Placeholders are passed as key/value pairs, e.g.
     * {@code get("pad-created", "name", "spawn")}.
     */
    public String get(String key, String... placeholders) {
        String message = messages.getString(key, key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return color(message.replace("{prefix}", prefix));
    }

    /** Sends a translated message to the given sender. */
    public void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    /** Returns a list of colored message lines (used for the help screen). */
    public List<String> getList(String key) {
        return messages.getStringList(key).stream()
                .map(line -> color(line.replace("{prefix}", prefix)))
                .toList();
    }

    private void applyBundledDefaults() {
        InputStream resource = plugin.getResource("messages_" + DEFAULT_LANGUAGE + ".yml");
        if (resource == null) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            messages.setDefaults(YamlConfiguration.loadConfiguration(reader));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load bundled default messages: " + e.getMessage());
        }
    }

    private void saveDefault(String name) {
        if (!new File(plugin.getDataFolder(), name).exists()) {
            plugin.saveResource(name, false);
        }
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
