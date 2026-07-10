package net.licory.slimejumps.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player launch cooldowns so pads cannot be spammed and
 * players are not re-launched every movement tick while standing on
 * a pad.
 */
public final class CooldownManager {

    private final Map<UUID, Long> lastUse = new HashMap<>();

    /**
     * Attempts to consume a launch for the given player.
     *
     * @param playerId   the player's unique id
     * @param cooldownMs minimum time between launches, in milliseconds
     * @return {@code true} if the launch is allowed
     */
    public boolean tryUse(UUID playerId, long cooldownMs) {
        long now = System.currentTimeMillis();
        Long last = lastUse.get(playerId);
        if (last != null && now - last < cooldownMs) {
            return false;
        }
        lastUse.put(playerId, now);
        return true;
    }

    /** Clears all state for a player (called on disconnect). */
    public void clear(UUID playerId) {
        lastUse.remove(playerId);
    }
}
