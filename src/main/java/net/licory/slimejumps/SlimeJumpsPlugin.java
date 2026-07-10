package net.licory.slimejumps;

import net.licory.slimejumps.command.SlimeJumpsCommand;
import net.licory.slimejumps.listener.JumpPadListener;
import net.licory.slimejumps.manager.FlightManager;
import net.licory.slimejumps.manager.JumpPadManager;
import net.licory.slimejumps.manager.RouteManager;
import net.licory.slimejumps.task.ParticleTask;
import net.licory.slimejumps.util.Messages;
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

    private JumpPadManager padManager;
    private RouteManager routeManager;
    private FlightManager flightManager;
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

        flightManager = new FlightManager(this);
        getServer().getPluginManager().registerEvents(flightManager, this);
        getServer().getScheduler().runTaskTimer(this, flightManager, 1L, 1L);

        registerCommand();
        getServer().getPluginManager().registerEvents(new JumpPadListener(this), this);
        startParticleTask();

        getLogger().info("SlimeJumps enabled with " + padManager.getAll().size()
                + " jump pad(s) and " + routeManager.getAll().size() + " route(s).");
    }

    @Override
    public void onDisable() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        if (padManager != null) {
            padManager.save();
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

    public JumpPadManager getPadManager() {
        return padManager;
    }

    public RouteManager getRouteManager() {
        return routeManager;
    }

    public FlightManager getFlightManager() {
        return flightManager;
    }

    public Messages getMessages() {
        return messages;
    }
}
