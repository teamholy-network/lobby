package de.teamholy.lobby;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

// seasonal: schedule in Lobby#onEnable while the Halloween event is active
public class HalloweenEffectsRunnable extends BukkitRunnable {

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            world.setTime(18000);
        }
    }
}
