package net.licory.slimejumps.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * A named jump pad anchored to a specific block.
 * <p>
 * Players standing on the pad block are launched in the direction they
 * are facing, using the pad's horizontal {@code power} and vertical
 * {@code vertical} velocity.
 */
public final class JumpPad {

    private final String name;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private double power;
    private double vertical;
    private String routeName;
    private Float fixedYaw;
    private String sound;
    private String particle;

    public JumpPad(String name, String worldName, int x, int y, int z, double power, double vertical) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.vertical = vertical;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /** Horizontal launch strength (multiplier of the player's facing direction). */
    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    /** Vertical launch velocity (Y component). */
    public double getVertical() {
        return vertical;
    }

    public void setVertical(double vertical) {
        this.vertical = vertical;
    }

    /**
     * Name of the route this pad launches players along, or {@code null}
     * for a regular directional launch.
     */
    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    /**
     * Fixed launch yaw in degrees, or {@code null} to launch players in
     * the direction they are looking.
     */
    public Float getFixedYaw() {
        return fixedYaw;
    }

    public void setFixedYaw(Float fixedYaw) {
        this.fixedYaw = fixedYaw;
    }

    /** Per-pad launch sound key, or {@code null} to use the global default. */
    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    /** Per-pad launch particle name, or {@code null} to use the global default. */
    public String getParticle() {
        return particle;
    }

    public void setParticle(String particle) {
        this.particle = particle;
    }

    /**
     * Resolves the block location of this pad.
     *
     * @return the location, or {@code null} if the world is not loaded
     */
    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    /** Unique key identifying the block this pad occupies. */
    public String blockKey() {
        return blockKey(worldName, x, y, z);
    }

    /** Builds the lookup key for a block position. */
    public static String blockKey(String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }
}
