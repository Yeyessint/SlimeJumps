package net.licory.slimejumps;

import net.licory.slimejumps.command.SlimeJumpsCommand;
import net.licory.slimejumps.listener.JumpPadListener;
import net.licory.slimejumps.manager.JumpPadManager;
import net.licory.slimejumps.task.ParticleTask;
import net.licory.slimejumps.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Main entry point of the SlimeJumps plugin.
 * <p>
 * SlimeJumps provides configurable jump pads for lobbies and hubs:
 * named pads with per-pad launch power, ambient particles, launch
 * sounds, cooldowns and fall damage protection.
 */
public final class SlimeJumpsPlugin extends JavaPlugin {

    private JumpPadManager padManager;
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

        registerCommand();
        getServer().getPluginManager().registerEvents(new JumpPadListener(this), this);
        startParticleTask();

        getLogger().info("SlimeJumps enabled with " + padManager.getAll().size() + " jump pad(s).");
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

    public Messages getMessages() {
        return messages;
    }
}
