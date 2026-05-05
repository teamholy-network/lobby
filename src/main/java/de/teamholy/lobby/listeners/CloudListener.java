package de.teamholy.lobby.listeners;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.event.events.service.CloudServiceConnectNetworkEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceInfoUpdateEvent;
import de.dytanic.cloudnet.ext.bridge.bukkit.event.BukkitCloudServiceStopEvent;
import de.teamholy.core.api.entities.player.PlayerProfile;
import de.teamholy.core.api.utility.PlayerRank;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.event.CloudChannelListenEvent;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/* copyright by Yassino */
public class CloudListener implements Listener {

    @EventHandler
    public void onSubChanne(CloudChannelListenEvent event) {
        JsonDocument data = event.getData();
        if (event.getChannel().equalsIgnoreCase("bukkit")) {
            if (event.getMessage().equalsIgnoreCase("onlineTime_update")) {
                //lobbyPlayer.setLabyModSubtitle();
                Lobby.getInstance().getLobbyPlayerEntryHandler().values().forEach(LobbyPlayer::updateOnlineTime);
            } else if (event.getMessage().equalsIgnoreCase("clan_update")) {
                UUID uuid = UUID.fromString(event.getData().getString("uuid"));
                LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(uuid);
                if (lobbyPlayer != null) {
                    lobbyPlayer.updateClanTagScore();
                    // lobbyPlayer.setLabyModSubtitle();
                }
            } else if (event.getMessage().equalsIgnoreCase("rank_update")) {
                UUID uuid = UUID.fromString(event.getData().getString("uuid"));
                if (BukkitCore.getInstance().getPlayerCacheManager().getCachedPlayers().containsKey(uuid)) {
                    Bukkit.getScheduler().runTaskLaterAsynchronously(Lobby.getInstance(), () -> {
                        PlayerProfile playerProfile = BukkitCore.getAPI().getPlayerService().getEntity(uuid, () -> BukkitCore.getAPI().getPlayerService().getRepository().findFirstById(uuid));
                        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(uuid);
                        lobbyPlayer.setPlayerRank(PlayerRank.valueOf(playerProfile.getRank()));
                        lobbyPlayer.updateRankScore();
                        lobbyPlayer.getPlayer().getInventory().setBoots(new ItemBuilder(Material.LEATHER_BOOTS).setLeatherColor(Color.fromBGR(
                                lobbyPlayer.getPlayerRank().getBlue(), lobbyPlayer.getPlayerRank().getGreen(), lobbyPlayer.getPlayerRank().getRed()
                        )).build());
                    }, 3);
                }
            } else if (event.getMessage().equalsIgnoreCase("coins_update")) {
                UUID uuid = UUID.fromString(event.getData().getString("uuid"));
                LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(uuid);
                if (lobbyPlayer != null) {
                    lobbyPlayer.updateCoinsScore();
                }
            } else if (event.getMessage().equalsIgnoreCase("cameFromBw")) {
                Bukkit.getScheduler().runTaskLater(Lobby.getInstance(), () -> {
                    System.out.println("Came from bw");
                    Player player = Bukkit.getPlayer(UUID.fromString(event.getData().getString("uuid")));
                    if (player != null) {
                        player.teleport(BukkitCore.getInstance().getLocationManager().getLocation("bw_spawn"));
                    }
                }, 3);
            } else if (event.getMessage().equalsIgnoreCase("friend_update")) {

                UUID uuid = data.get("target", UUID.class);
                String extra = data.getString("extra");
                UUID target = data.get("player", UUID.class);
                if (extra == null) extra = "";
                if (uuid != null) {
                    LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(uuid);
                    if (lobbyPlayer == null) return;
                    lobbyPlayer.getFriendEntry().updateFriendEntry(target, data.getString("type"), extra);
                } else {
                    String finalExtra = extra;
                    Lobby.getInstance().getLobbyPlayerEntryHandler().forEach((uuid1, lobbyPlayer) -> {
                        if (lobbyPlayer.getFriendEntry().getFriends().containsKey(target))
                            lobbyPlayer.getFriendEntry().updateFriendEntry(target, data.getString("type"), finalExtra);
                    });
                }
            } else if (event.getMessage().equalsIgnoreCase("friendrequest_update")) {
                LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(data.get("target", UUID.class));
                if (lobbyPlayer == null) return;
                lobbyPlayer.getFriendEntry().updateFriendRequestEntry(data.get("player", UUID.class), data.getString("type"));
            }
        }
    }

    @EventHandler
    public void onCloud(BukkitCloudServiceInfoUpdateEvent event) {
        Lobby.getInstance().getCloudCacheHandler().updateServerInfo(event.getServiceInfoSnapshot());
        Lobby.getInstance().getHologramHandler().updateHolograms();
        Lobby.getInstance().getBedwarsServerInventory().updateInventory(event.getServiceInfoSnapshot().getConfiguration().getGroups()[0]);

        Lobby.getInstance().getBedwarsSpectateInventory().updateServer(event.getServiceInfoSnapshot());
    }

    @EventListener
    public void onCloud(CloudServiceConnectNetworkEvent event) {
        Lobby.getInstance().getCloudCacheHandler().updateServerInfo(event.getServiceInfo());
        Lobby.getInstance().getBedwarsServerInventory().updateInventory(event.getServiceInfo().getConfiguration().getGroups()[0]);
        Lobby.getInstance().getHologramHandler().updateHolograms();
    }

    @EventHandler
    public void onCloud(BukkitCloudServiceStopEvent event) {
        Lobby.getInstance().getCloudCacheHandler().removeServerInfo(event.getServiceInfoSnapshot());
        Lobby.getInstance().getBedwarsServerInventory().updateInventory(event.getServiceInfoSnapshot().getConfiguration().getGroups()[0]);
        Lobby.getInstance().getHologramHandler().updateHolograms();

    }


}