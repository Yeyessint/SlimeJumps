package net.licory.slimejumps;

import net.licory.slimejumps.commands.configCMD;
import net.licory.slimejumps.listeners.PlayerListeners;
import net.licory.slimejumps.runnables.JumpPadsRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public class SlimeJumps extends JavaPlugin {

    public static SlimeJumps slimeJumps;

    @Override
    public void onEnable() {
        
        slimeJumps = this;

        System.out.println("------------------");
        System.out.println("Slime-JumpPads");
        System.out.println("ACTIVATED");
        System.out.println("------------------");

        getConfig().options().copyDefaults(true);
        saveConfig();
        getConfig();
        saveDefaultConfig();
        
        cargarComandos();
        cargarListeners();

        int xs = Integer.valueOf(getConfig().getString("createds"));
        
        if (xs > 0) {

            getServer().getScheduler().scheduleSyncRepeatingTask(this, new JumpPadsRunnable(), 20L, 20L);
            
        }

    }

    @Override
    public void onDisable(){
        
        slimeJumps = this;
        
        System.out.println("------------------");
        System.out.println("Slime-JumpPads");
        System.out.println("DESACTIVATED");
        System.out.println("------------------");
        
    }
    
    public void cargarComandos() {
        
        getCommand("config").setExecutor(new configCMD());
        
        
    }

    public void cargarListeners() {
        
        getServer().getPluginManager().registerEvents(new PlayerListeners(), this);

    }

    public static SlimeJumps getInstance() {

        return slimeJumps;

    }

}
