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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and serves translatable plugin messages.
 * <p>
 * Messages are read from {@code messages_&lt;language&gt;.yml} in the data
 * folder, where {@code language} comes from {@code config.yml}. English
 * and Spanish files are shipped by default; missing keys fall back to the
 * bundled English messages. Every shipped language file is also kept
 * loaded so features like the tutorial can serve a language chosen by
 * the individual player, independent of the global default.
 */
public final class Messages {

    private static final String DEFAULT_LANGUAGE = "en";
    /** Language files bundled inside the jar. */
    private static final List<String> BUNDLED_LANGUAGES = List.of("en", "es");

    private final SlimeJumpsPlugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private final Map<String, YamlConfiguration> byLanguage = new LinkedHashMap<>();
    private String prefix = "";

    public Messages(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** (Re)loads the message file selected in {@code config.yml}. */
    public void load() {
        for (String language : BUNDLED_LANGUAGES) {
            saveDefault("messages_" + language + ".yml");
        }

        byLanguage.clear();
        for (String language : BUNDLED_LANGUAGES) {
            File langFile = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
            if (langFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                applyBundledDefaults(config, language);
                byLanguage.put(language, config);
            }
        }

        String language = plugin.getConfig().getString("language", DEFAULT_LANGUAGE)
                .toLowerCase(Locale.ROOT);
        File file = new File(plugin.getDataFolder(), "messages_" + language + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Language file for '" + language + "' not found, using English.");
            file = new File(plugin.getDataFolder(), "messages_" + DEFAULT_LANGUAGE + ".yml");
        }

        messages = YamlConfiguration.loadConfiguration(file);
        applyBundledDefaults(messages, DEFAULT_LANGUAGE);
        prefix = color(messages.getString("prefix", "&7SlimeJumps &a» "));
    }

    /** Language codes available for per-player selection (e.g. the tutorial). */
    public List<String> availableLanguages() {
        return new ArrayList<>(byLanguage.keySet());
    }

    /** Whether a language file is loaded for the given code. */
    public boolean hasLanguage(String language) {
        return language != null && byLanguage.containsKey(language.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns a colored message in a specific language, falling back to
     * the globally loaded messages when the language or key is missing.
     */
    public String getIn(String language, String key, String... placeholders) {
        YamlConfiguration config = language == null
                ? null : byLanguage.get(language.toLowerCase(Locale.ROOT));
        String message = config != null ? config.getString(key, null) : null;
        if (message == null) {
            message = messages.getString(key, key);
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return color(message.replace("{prefix}", prefix));
    }

    /** Returns a list of colored lines in a specific language. */
    public List<String> getListIn(String language, String key) {
        YamlConfiguration config = language == null
                ? null : byLanguage.get(language.toLowerCase(Locale.ROOT));
        List<String> lines = config != null && config.contains(key)
                ? config.getStringList(key)
                : messages.getStringList(key);
        return lines.stream()
                .map(line -> color(line.replace("{prefix}", prefix)))
                .toList();
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

    private void applyBundledDefaults(YamlConfiguration config, String language) {
        InputStream resource = plugin.getResource("messages_" + language + ".yml");
        if (resource == null) {
            resource = plugin.getResource("messages_" + DEFAULT_LANGUAGE + ".yml");
        }
        if (resource == null) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            config.setDefaults(YamlConfiguration.loadConfiguration(reader));
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
