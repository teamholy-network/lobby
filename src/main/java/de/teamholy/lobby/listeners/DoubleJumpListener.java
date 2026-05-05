package de.teamholy.lobby.listeners;

import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

/* copyright by Yassino */
public class DoubleJumpListener implements Listener {

    @EventHandler
    public void onFly(PlayerToggleFlightEvent e) {
        Player player = e.getPlayer();
        if (isInArena(player))return;
        if (isFlying(player))return;
        if (player.getGameMode() == GameMode.SURVIVAL) {
            e.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFallDistance(0.0F);
            Location loc = player.getLocation();
            Vector v = loc.getDirection().multiply(1.0F).setY(1.0D);
            player.setVelocity(v);
            player.playSound(player.getLocation(), Sound.WITHER_SHOOT, 1, 0.2f);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (isInArena(player))return;
        if (isFlying(player))return;
        if (player.getGameMode() == GameMode.SURVIVAL) {
            if (player.isOnGround() && !player.getAllowFlight())
                player.setAllowFlight(true);
        }
    }

    private boolean isInArena(Player player) {
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId());
        if (lobbyPlayer == null) return false;
        return lobbyPlayer.isInArena();
    }

    private boolean isFlying(Player player) {
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId());
        if (lobbyPlayer == null) return false;
        return lobbyPlayer.isFly();
    }

}
