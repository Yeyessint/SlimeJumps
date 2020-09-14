package net.licory.slimejumps.commands;

import net.licory.slimejumps.SlimeJumps;
import net.licory.slimejumps.locations.ConfigLocations;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class configCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command arg1, String arg2, String[] args) {
        Player p = (Player) sender;
        
        if (p.hasPermission("slimejumps.create")) {
            if (arg1.getName().equalsIgnoreCase("config")) {
                
                if (args.length < 1) {
                    
                    p.sendMessage("§c§m---------------------------------------------");
                    p.sendMessage("");
                    p.sendMessage("§c/config setpad §7(Add JumpPad)");
                    p.sendMessage("");
                    p.sendMessage("§c§m---------------------------------------------");
                    return true;
   
                }
                
                if (args[0].equalsIgnoreCase("setpad")) {
                    if (args.length < 1) {
                        p.sendMessage("§c/config setpad");
                        return true;
                    }
                    
                    ConfigLocations.makeLoc(p, p.getLocation());
                    SlimeJumps.getInstance().saveConfig();
                    SlimeJumps.getInstance().reloadConfig();
                    
                    p.sendMessage("§7SlimePads §a» §eYou have created a launchpad.");
                    p.getPlayer().playSound(p.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                    
                }
            }
        } else {
            
            p.sendMessage("§7SlimePads §a» §cYou do not have permissions.");
            
        }
        return true;
    }

}
