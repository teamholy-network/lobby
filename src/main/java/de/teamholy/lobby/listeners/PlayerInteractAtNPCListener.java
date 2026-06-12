package de.teamholy.lobby.listeners;

import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.npc.event.PlayerInteractAtNPCEvent;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
            case "wm 2026" -> lobbyPlayer.openGameSubInventory("wm2026", Material.SLIME_BALL);
            case "bridge" -> lobbyPlayer.openGameSubInventory("Bridge", Material.IRON_PICKAXE);
            case "web shop" -> Lobby.getInstance().getWebshopInventory().openInventory(lobbyPlayer);
            default -> {
            }

        }
    }

}
