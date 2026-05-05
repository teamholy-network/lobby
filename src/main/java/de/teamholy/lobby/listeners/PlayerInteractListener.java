package de.teamholy.lobby.listeners;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.FriendEntry;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/* copyright by Yassino */
public class PlayerInteractListener implements Listener {

    @EventHandler
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof Player target)) return;
        if (player.getItemInHand() != null && player.getItemInHand().getType() != null) {
            if (player.getItemInHand().getType() == Material.FIREWORK) {
              BukkitCore.getAPI().getCloudManager().sendCloudMessage("party","removeinvite", JsonDocument.newDocument("attacker",player.getUniqueId()).append("entity",target.getUniqueId()));
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId());

        if (event.getAction() != null || event.getItem() != null || event.getItem().getType() != null || event.getItem().getType() != Material.AIR || event.getItem().getItemMeta() != null || event.getItem().getItemMeta().getDisplayName() != null) {
            if(event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

                if(event.getItem() == null || event.getItem().getItemMeta() == null) {
                    return;
                }

                if (event.getItem().getType() == Material.COMPASS) {
                    lobbyPlayer.openGamesInventory();
                } else if (event.getItem().getType() == Material.NETHER_STAR) {
                    lobbyPlayer.openLobbySwitcher();
                } else if (event.getItem().getType() == Material.SKULL_ITEM) {
                    lobbyPlayer.getFriendEntry().openFriendGui(player,1, FriendEntry.SortOption.LASTONLINE_RECENTLY);
                } else if (event.getItem().getType() == Material.FIREWORK) {
                    event.setCancelled(true);
                } else if (event.getItem().getType() == Material.REDSTONE_COMPARATOR) {
                    lobbyPlayer.openSettings();
                }


            }
        }
    }



}
