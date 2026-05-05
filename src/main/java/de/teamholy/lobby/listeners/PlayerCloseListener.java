package de.teamholy.lobby.listeners;

import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

/* copyright by Yassino */
public class PlayerCloseListener implements Listener {


    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

      var inventory = event.getInventory();


        if (inventory == null) return;
        if (!inventory.getName().toLowerCase().contains("perks")) return;

        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId());
        lobbyPlayer.setInventory();

    }

}
