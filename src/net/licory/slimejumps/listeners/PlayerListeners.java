package net.licory.slimejumps.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class PlayerListeners implements Listener{
    
    
    @EventHandler
    public void Saltador(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location standBlock = p.getWorld().getBlockAt(p.getLocation().add(0.0D, -0.01D, 0.0D)).getLocation();
        
        if (standBlock.getBlock().getType() == Material.SLIME_BLOCK) {
            
            Vector v = p.getLocation().getDirection().multiply(2.0D).setY(1.0D);
            p.setVelocity(v);
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_HIT, 1.0F, 1.0F);
            p.setFallDistance(999.0F);
            
        }
    }
    
    
}
