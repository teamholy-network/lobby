package de.teamholy.lobby.listeners;

import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.npc.event.PlayerInteractAtNPCEvent;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.UUID;
import java.util.function.Consumer;

/* copyright by Yassino */
public class PlayerInteractAtNPCListener implements Listener {

    @EventHandler
    public void onNPC(PlayerInteractAtNPCEvent event) {
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(event.getPlayer().getUniqueId());
        String game = ChatColor.stripColor(event.getNpcEntry().getDisplayName()).toLowerCase();

        switch (game) {
            case "mlgrush" -> lobbyPlayer.openGameSubInventory("MLGRush", Material.STICK);
            case "clutches" -> lobbyPlayer.openGameSubInventory("Clutches", Material.RED_SANDSTONE);
            //case "knockbackffa" -> lobbyPlayer.openGameSubInventory("KnockbackFFA", Material.SANDSTONE);
            //case "sgffa" -> lobbyPlayer.openGameSubInventory("SGFFA", Material.IRON_SWORD);
            //case "skywarsffa" -> lobbyPlayer.openGameSubInventory("SkyWarsFFA", Material.GRASS);
            //case "duels" -> lobbyPlayer.openGameSubInventory("Duels", Material.DIAMOND_SWORD);
            case "spectate" -> lobbyPlayer.getPlayer().openInventory(Lobby.getInstance().getBedwarsSpectateInventory().getInventory());
            case "bedwars" -> Lobby.getInstance().getBedwarsServerInventory().openBWInventory(lobbyPlayer.getPlayer());
            case "rushbw" -> Lobby.getInstance().getBedwarsServerInventory().openRushInventory(lobbyPlayer.getPlayer());
            case "bedwars&rush" -> lobbyPlayer.getPlayer().teleport(BukkitCore.getInstance().getLocationManager().getLocation("bw_spawn"));
            case "website" -> lobbyPlayer.executeBungeeCommand("link");
            case "namemc" -> lobbyPlayer.executeBungeeCommand("vote");
            case "bridge" -> lobbyPlayer.openGameSubInventory("Bridge", Material.IRON_PICKAXE);
            case "web shop" -> Lobby.getInstance().getWebshopInventory().openInventory(lobbyPlayer);
            default -> {
            }

        }
    }




    private void hasLiked(UUID uuid, Consumer<String> consumer) {
        BukkitCore.getAPI().getExecutor().execute(() -> {
            try {
                URL url = new URL("https://api.namemc.com/server/teamholy.de/likes?profile=" + uuid);

                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

                String line;
                while ((line = in.readLine()) != null) {
                    consumer.accept(line);
                }
                in.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


}
