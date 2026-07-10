package net.licory.slimejumps;

import net.licory.slimejumps.command.SlimeJumpsCommand;
import net.licory.slimejumps.gui.PadListGui;
import net.licory.slimejumps.integration.SlimeJumpsExpansion;
import net.licory.slimejumps.listener.DoubleJumpListener;
import net.licory.slimejumps.listener.JumpPadListener;
import net.licory.slimejumps.listener.WandListener;
import net.licory.slimejumps.manager.ChargeManager;
import net.licory.slimejumps.manager.FlightManager;
import net.licory.slimejumps.manager.HologramManager;
import net.licory.slimejumps.manager.JumpPadManager;
import net.licory.slimejumps.manager.RouteEditManager;
import net.licory.slimejumps.manager.RouteManager;
import net.licory.slimejumps.manager.StatsManager;
import net.licory.slimejumps.task.ParticleTask;
import net.licory.slimejumps.util.Messages;
import net.licory.slimejumps.util.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Main entry point of the SlimeJumps plugin.
 * <p>
 * SlimeJumps provides configurable jump pads for lobbies and hubs:
 * named pads with per-pad launch power, flight routes, ambient
 * particles, launch sounds, cooldowns and fall damage protection.
 */
public final class SlimeJumpsPlugin extends JavaPlugin {

    /** How often launch statistics are flushed to disk (5 minutes). */
    private static final long STATS_SAVE_INTERVAL_TICKS = 6000L;

    /**
     * bStats service id. Register the plugin at
     * https://bstats.org/register to obtain a real id and set it here;
     * metrics are skipped while it is 0.
     */
    private static final int BSTATS_SERVICE_ID = 0;

    private JumpPadManager padManager;
    private RouteManager routeManager;
    private FlightManager flightManager;
    private StatsManager statsManager;
    private HologramManager hologramManager;
    private ChargeManager chargeManager;
    private RouteEditManager routeEditManager;
    private WandListener wandListener;
    private JumpPadListener jumpPadListener;
    private PadListGui padListGui;
    private Messages messages;
    private BukkitTask particleTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        messages.load();

        padManager = new JumpPadManager(this);
        padManager.load();
        padManager.migrateLegacyConfig();

        routeManager = new RouteManager(this);
        routeManager.load();

        statsManager = new StatsManager(this);
        statsManager.load();
        getServer().getScheduler().runTaskTimer(this, statsManager::saveIfDirty,
                STATS_SAVE_INTERVAL_TICKS, STATS_SAVE_INTERVAL_TICKS);

        flightManager = new FlightManager(this);
        getServer().getPluginManager().registerEvents(flightManager, this);
        getServer().getScheduler().runTaskTimer(this, flightManager, 1L, 1L);

        padListGui = new PadListGui(this);
        getServer().getPluginManager().registerEvents(padListGui, this);

        hologramManager = new HologramManager(this);
        getServer().getPluginManager().registerEvents(hologramManager, this);
        hologramManager.spawnAll();

        chargeManager = new ChargeManager(this);
        getServer().getPluginManager().registerEvents(chargeManager, this);
        getServer().getScheduler().runTaskTimer(this, chargeManager, 2L, 2L);

        routeEditManager = new RouteEditManager(this);
        getServer().getPluginManager().registerEvents(routeEditManager, this);
        getServer().getScheduler().runTaskTimer(this, routeEditManager, 10L, 10L);

        wandListener = new WandListener(this);
        getServer().getPluginManager().registerEvents(wandListener, this);

        registerCommand();
        jumpPadListener = new JumpPadListener(this);
        getServer().getPluginManager().registerEvents(jumpPadListener, this);
        getServer().getPluginManager().registerEvents(new DoubleJumpListener(this), this);
        startParticleTask();
        startMetrics();
        hookPlaceholderApi();

        if (getConfig().getBoolean("update-checker", true)) {
            UpdateChecker updateChecker = new UpdateChecker(this);
            getServer().getPluginManager().registerEvents(updateChecker, this);
            updateChecker.checkAsync();
        }

        getLogger().info("SlimeJumps enabled with " + padManager.getAll().size()
                + " jump pad(s) and " + routeManager.getAll().size() + " route(s).");
    }

    @Override
    public void onDisable() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (routeEditManager != null) {
            routeEditManager.reset();
        }
        if (padManager != null) {
            padManager.save();
        }
        if (statsManager != null) {
            statsManager.saveIfDirty();
        }
        getLogger().info("SlimeJumps disabled.");
    }

    /**
     * Reloads the configuration, messages and the ambient particle task.
     * Registered pads are reloaded from disk as well.
     */
    public void reload() {
        reloadConfig();
        messages.load();
        padManager.load();
        routeManager.load();
        routeEditManager.reset();
        hologramManager.removeAll();
        hologramManager.spawnAll();
        restartParticleTask();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("slimejumps");
        if (command == null) {
            getLogger().severe("Command 'slimejumps' is missing from plugin.yml!");
            return;
        }
        SlimeJumpsCommand executor = new SlimeJumpsCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void startParticleTask() {
        if (!getConfig().getBoolean("ambient-particles.enabled", true)) {
            return;
        }
        long interval = Math.max(1L, getConfig().getLong("ambient-particles.interval-ticks", 10L));
        particleTask = getServer().getScheduler().runTaskTimer(this, new ParticleTask(this), interval, interval);
    }

    private void restartParticleTask() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        startParticleTask();
    }

    /**
     * Starts anonymous bStats usage metrics, if enabled in the config
     * and a bStats service id has been configured.
     */
    private void startMetrics() {
        if (BSTATS_SERVICE_ID > 0 && getConfig().getBoolean("metrics", true)) {
            new Metrics(this, BSTATS_SERVICE_ID);
        }
    }

    /** Registers the PlaceholderAPI expansion when PlaceholderAPI is present. */
    private void hookPlaceholderApi() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SlimeJumpsExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }
    }

    public JumpPadManager getPadManager() {
        return padManager;
    }

    public RouteManager getRouteManager() {
        return routeManager;
    }

    public FlightManager getFlightManager() {
        return flightManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public PadListGui getPadListGui() {
        return padListGui;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public WandListener getWandListener() {
        return wandListener;
    }

    public ChargeManager getChargeManager() {
        return chargeManager;
    }

    public RouteEditManager getRouteEditManager() {
        return routeEditManager;
    }

    public JumpPadListener getJumpPadListener() {
        return jumpPadListener;
    }

    public Messages getMessages() {
        return messages;
    }
}
