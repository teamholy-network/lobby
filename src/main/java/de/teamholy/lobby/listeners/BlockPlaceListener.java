package de.teamholy.lobby.listeners;

import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.perks.enums.PerkType;
import de.teamholy.lobby.Lobby;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockBreakAnimation;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/* copyright by Yassino */
public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE)
            return;


        if (BukkitCore.getInstance().getPerkManager().getPerk(player, PerkType.BLOCK).itemStack.getType() == e.getBlock().getType()) {

            if (e.getBlockReplacedState().getType() != Material.AIR) {
                e.setCancelled(true);
                return;
            }

            if (BukkitCore.getInstance().getLocationManager().getLocation("de/teamholy/lobby").distance(e.getBlock().getLocation()) < 10.0) {
                player.sendMessage("§cYou cant place blocks here");
                e.setCancelled(true);
                return;
            }

            player.getInventory().setItem(4, BukkitCore.getInstance().getPerkManager().getPerk(player, PerkType.BLOCK).setName("§8» §6Blocks §8(§7rightclick§8)").setAmount(64).build());
            new BukkitRunnable() {
                int i = 0;
                final int random = new Random().nextInt(99999999);
                @Override
                public void run() {
                    PacketPlayOutBlockBreakAnimation packet = new PacketPlayOutBlockBreakAnimation(random, new BlockPosition(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ()), i);
                    if (e.getBlock().getLocation().getBlock().getType() == Material.AIR) cancel();
                    if (i >= 0 && i <= 7) {
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            ((CraftPlayer) all).getHandle().playerConnection.sendPacket(packet);
                        }
                    } else {
                        e.getBlock().setType(Material.AIR);
                        PacketPlayOutBlockBreakAnimation packet1 = new PacketPlayOutBlockBreakAnimation(random, new BlockPosition(e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ()), -1);
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            ((CraftPlayer) all).getHandle().playerConnection.sendPacket(packet1);
                        }
                        cancel();
                    }
                    i++;
                }
            }.runTaskTimer(Lobby.getInstance(), 10, 5);
        } else e.setCancelled(true);
    }

}
