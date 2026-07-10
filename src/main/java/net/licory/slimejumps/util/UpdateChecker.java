package net.licory.slimejumps.util;

import net.licory.slimejumps.SlimeJumpsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks the project's GitHub releases for a newer version.
 * <p>
 * The check runs once, asynchronously, when the plugin enables. If a
 * newer release is found it is logged to the console and shown to
 * admins when they join. Can be turned off with {@code update-checker}
 * in {@code config.yml}.
 */
public final class UpdateChecker implements Listener {

    private static final String API_URL =
            "https://api.github.com/repos/Yeyessint/SlimeJumps/releases/latest";
    private static final Pattern TAG_PATTERN =
            Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"");
    private static final String ADMIN_PERMISSION = "slimejumps.admin";

    private final SlimeJumpsPlugin plugin;
    private volatile String newerVersion;

    public UpdateChecker(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Fetches the latest release tag off the main thread. */
    public void checkAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String latest = fetchLatestVersion();
            if (latest == null) {
                return;
            }
            String current = plugin.getDescription().getVersion();
            if (!latest.equalsIgnoreCase(current)) {
                newerVersion = latest;
                plugin.getLogger().info("A new version of SlimeJumps is available: " + latest
                        + " (you are on " + current + "). "
                        + "https://github.com/Yeyessint/SlimeJumps/releases");
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String latest = newerVersion;
        if (latest == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessages().send(player, "update-available", "version", latest);
        }
    }

    private String fetchLatestVersion() {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "SlimeJumps-UpdateChecker");

            if (connection.getResponseCode() != 200) {
                return null;
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }
            Matcher matcher = TAG_PATTERN.matcher(body.toString());
            return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
        } catch (Exception e) {
            // Network hiccups are not worth alarming the console about.
            return null;
        }
    }
}
