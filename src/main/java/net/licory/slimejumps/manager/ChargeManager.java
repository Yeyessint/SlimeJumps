package net.licory.slimejumps.manager;

import net.licory.slimejumps.SlimeJumpsPlugin;
import net.licory.slimejumps.model.JumpPad;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Runs charge pads ({@code /sj setcharge}): while a player stands on
 * one, a charge bar fills in their action bar with a rising sound
 * pitch. At 100% the pad launches at full power; stepping off earlier
 * launches with the accumulated fraction (or cancels if below the
 * configured minimum).
 */
public final class ChargeManager implements Listener, Runnable {

    private static final int BAR_SEGMENTS = 20;

    /** State of one player's charge in progress. */
    private static final class Charge {
        final String padName;
        final long startedAt = System.currentTimeMillis();
        int lastSoundStep = -1;

        Charge(String padName) {
            this.padName = padName;
        }
    }

    private final SlimeJumpsPlugin plugin;
    private final Map<UUID, Charge> charging = new HashMap<>();
    private final Map<UUID, Long> lastLaunch = new HashMap<>();

    public ChargeManager(SlimeJumpsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts charging a pad for a player. No-op if the player is
     * already charging or launched from a charge pad very recently.
     */
    public void beginCharge(Player player, JumpPad pad) {
        UUID id = player.getUniqueId();
        if (charging.containsKey(id)) {
            return;
        }
        long cooldownMs = pad.getCooldownMs() != null
                ? pad.getCooldownMs()
                : plugin.getConfig().getLong("pads.cooldown-ms", 500L);
        Long last = lastLaunch.get(id);
        if (last != null && System.currentTimeMillis() - last < Math.max(cooldownMs, 500L)) {
            return;
        }
        charging.put(id, new Charge(pad.getName()));
    }

    @Override
    public void run() {
        if (charging.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Charge>> iterator = charging.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Charge> entry = iterator.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            Charge charge = entry.getValue();

            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }

            JumpPad pad = plugin.getPadManager().get(charge.padName);
            if (pad == null || !pad.isEnabled() || pad.getChargeMs() == null) {
                iterator.remove();
                continue;
            }

            double fraction = Math.min(1.0D,
                    (System.currentTimeMillis() - charge.startedAt) / (double) Math.max(1L, pad.getChargeMs()));

            if (!isStandingOn(player, pad)) {
                iterator.remove();
                double minFraction = plugin.getConfig().getDouble("charge.min-launch-fraction", 0.2D);
                if (fraction >= minFraction) {
                    launch(player, pad, fraction);
                } else {
                    sendActionBar(player, plugin.getMessages().get("charge-cancelled"));
                }
                continue;
            }

            if (fraction >= 1.0D) {
                iterator.remove();
                launch(player, pad, 1.0D);
                continue;
            }

            renderBar(player, fraction);
            playChargeSound(player, charge, fraction);
        }
    }

    private void launch(Player player, JumpPad pad, double fraction) {
        lastLaunch.put(player.getUniqueId(), System.currentTimeMillis());
        plugin.getJumpPadListener().executeCharged(player, pad, fraction);
    }

    private boolean isStandingOn(Player player, JumpPad pad) {
        Location location = player.getLocation();
        Block below = location.getWorld().getBlockAt(
                location.getBlockX(),
                (int) Math.floor(location.getY() - 0.0625D),
                location.getBlockZ());
        JumpPad at = plugin.getPadManager().getAt(below);
        return at != null && at.getName().equalsIgnoreCase(pad.getName());
    }

    private void renderBar(Player player, double fraction) {
        int filled = (int) Math.round(fraction * BAR_SEGMENTS);
        String bar = plugin.getMessages().get("charge-bar",
                "filled", "|".repeat(filled),
                "empty", "|".repeat(BAR_SEGMENTS - filled),
                "percent", String.valueOf((int) (fraction * 100)));
        sendActionBar(player, bar);
    }

    private void playChargeSound(Player player, Charge charge, double fraction) {
        if (!plugin.getConfig().getBoolean("charge.sound.enabled", true)) {
            return;
        }
        int step = (int) (fraction * 10);
        if (step == charge.lastSoundStep) {
            return;
        }
        charge.lastSoundStep = step;
        String sound = plugin.getConfig().getString("charge.sound.name", "block.note_block.pling");
        float pitch = (float) (0.6D + fraction * 1.2D);
        Location location = player.getLocation();
        location.getWorld().playSound(location, sound, 0.7F, pitch);
    }

    private static void sendActionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(ChatColor.translateAlternateColorCodes('&', text)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        charging.remove(id);
        lastLaunch.remove(id);
    }
}
