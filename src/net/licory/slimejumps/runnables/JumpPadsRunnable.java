package net.licory.slimejumps.runnables;

import java.util.List;
import static net.licory.slimejumps.locations.ConfigLocations.getLocs;
import net.licory.slimejumps.utils.ParticleEffect;
import org.bukkit.Location;

public class JumpPadsRunnable implements Runnable{

    @Override
    public void run() {
        
        List<Location> locs = getLocs();
        
        for (Location ls : locs) {
            
            ParticleEffect.VILLAGER_HAPPY.display(0.5F, 0.2F, 0.5F, 1.0F, 10, ls.clone(), 20.0D);
            
        }
        
    }
    
    
    
}