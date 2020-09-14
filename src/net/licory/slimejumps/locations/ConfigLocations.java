package net.licory.slimejumps.locations;

import java.util.ArrayList;
import java.util.List;
import net.licory.slimejumps.SlimeJumps;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ConfigLocations {

    public static void makeLoc(Player p, Location loc) {

        int count = SlimeJumps.getInstance().getConfig().getInt("createds");
        SlimeJumps.getInstance().getConfig().set("locs." + (count + 1), ConfigLocations.setLoc(loc));
        SlimeJumps.getInstance().getConfig().set("createds", count + 1);
        SlimeJumps.getInstance().saveConfig();

    }

    public static Location getLoc(String path) {

        Location loc = null;
        String[] locs = path.split(",");
        loc = new Location(Bukkit.getWorld(locs[0]), Integer.parseInt(locs[1]), Integer.parseInt(locs[2]), Integer.parseInt(locs[3]));
        loc.add(0.5D, 0.0, 0.5D);
        return loc;

    }

    public static String setLoc(Location loc) {

        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + loc.getYaw() + "," + loc.getPitch();

    }

    public static List<Location> getLocs() {
        List<Location> locss = new ArrayList<>();
        int count = SlimeJumps.getInstance().getConfig().getInt("createds");

        for (int i = 1; i < count + 1; i++) {

            locss.add(ConfigLocations.getLoc(SlimeJumps.getInstance().getConfig().getString("locs." + i)));

        }
        return locss;
    }

}
