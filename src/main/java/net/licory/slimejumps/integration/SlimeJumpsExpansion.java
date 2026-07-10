package net.licory.slimejumps.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.licory.slimejumps.SlimeJumpsPlugin;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PlaceholderAPI expansion. Available placeholders:
 * <ul>
 *   <li>{@code %slimejumps_total_launches%} — server-wide launch count</li>
 *   <li>{@code %slimejumps_player_launches%} — the player's launch count</li>
 *   <li>{@code %slimejumps_pads%} — number of registered pads</li>
 *   <li>{@code %slimejumps_routes%} — number of registered routes</li>
 *   <li>{@code %slimejumps_top_pad%} — name of the most used pad</li>
 *   <li>{@code %slimejumps_top_pad_uses%} — its launch count</li>
 * </ul>
 */
public final class SlimeJumpsExpansion extends PlaceholderExpansion {

    private final SlimeJumpsPlugin plugin;

    public SlimeJumpsExpansion(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "slimejumps";
    }

    @Override
    public String getAuthor() {
        return "Yeyessint";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        switch (params.toLowerCase(Locale.ROOT)) {
            case "total_launches":
                return String.valueOf(plugin.getStatsManager().getTotalLaunches());
            case "player_launches":
                return player == null ? "0"
                        : String.valueOf(plugin.getStatsManager().getPlayerLaunches(player.getUniqueId()));
            case "pads":
                return String.valueOf(plugin.getPadManager().getAll().size());
            case "routes":
                return String.valueOf(plugin.getRouteManager().getAll().size());
            case "top_pad": {
                List<Map.Entry<String, Long>> top = plugin.getStatsManager().getTopPads(1);
                return top.isEmpty() ? "-" : top.get(0).getKey();
            }
            case "top_pad_uses": {
                List<Map.Entry<String, Long>> top = plugin.getStatsManager().getTopPads(1);
                return top.isEmpty() ? "0" : String.valueOf(top.get(0).getValue());
            }
            default:
                return null;
        }
    }
}
