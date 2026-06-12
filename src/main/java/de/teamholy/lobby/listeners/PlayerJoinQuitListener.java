package de.teamholy.lobby.listeners;

import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.event.CachedPlayerJoinEvent;
import de.teamholy.core.bukkit.manager.PlayerCacheManager;
import de.teamholy.core.bukkit.npc.NPCBuilder;
import de.teamholy.core.bukkit.npc.models.NPCEntry;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.List;
import java.util.UUID;

/* copyright by Yassino */
public class PlayerJoinQuitListener implements Listener {

    private record NpcDefinition(String id, String displayName, UUID uuid, String locationKey,
                                 boolean skinDisplay, String[] holoLines) {
    }

    // seasonal/disabled NPCs:
    //new NPCBuilder("knockbackffa","§e§lKnockbackFFA", UUID.fromString("2646aecf-ddcc-4f3a-bedd-3b2c8d386350"),50,10,true,true,BukkitCore.getInstance().getLocationManager().getLocation("kbffa")).build(player);
    //new NPCBuilder("sgffa","§a§lSGFFA", UUID.fromString("fa44c187-80dd-4171-bb5a-2e694c4c8b4f"),50,10,true,true,BukkitCore.getInstance().getLocationManager().getLocation("sgffa")).build(player);
    //new NPCBuilder("duels","§d§lDuels", UUID.fromString("99f75c4a-daec-476b-98da-d791b1faf60d"),50,10,true,true,BukkitCore.getInstance().getLocationManager().getLocation("duels")).build(player);
    //new NPCBuilder("skywarsffa","§3§lSkyWarsFFA", UUID.fromString("eecc3c44-eaaf-48fe-af23-3af762578446"),50,10,true,true, BukkitCore.getInstance().getLocationManager().getLocation("skywarsffa")).build(player);
    private static final List<NpcDefinition> NPCS = List.of(
            new NpcDefinition("mlgrush", "§6§lMLGRush", UUID.fromString("a54e8818-845d-4f55-923b-38b334697096"), "mlgrush", true, null),
            new NpcDefinition("clutches", "§b§lClutches", UUID.fromString("1dd0cc8f-5271-4d49-b774-16dc36877017"), "clutches", true, null),
            new NpcDefinition("bridge", "§e§lBridge", UUID.fromString("0906aa96-698f-4542-8a22-9543d5dce379"), "bridge", true, null),
            new NpcDefinition("bedwars", "§c§lBedwars&Rush", UUID.fromString("6d40f495-d796-4244-9f45-964cdd7e685a"), "bedwars", true, null),
            new NpcDefinition("bedwars_spawn", "§c§lBedwars", UUID.fromString("1cfcd3b8-10ff-40a8-b3f9-c61628b5d098"), "bw_spawn_bw", true, null),
            new NpcDefinition("rush_spawn", "§c§lRushBW", UUID.fromString("ce397ef0-7973-4ce5-a3b1-3bd6e7fc9970"), "bw_spawn_rbw", true, null),
            new NpcDefinition("namemc", "§f§lNameMC", UUID.fromString("982239a5-582a-483f-ac1f-2289b8dccf20"), "namemc_npc", false,
                    new String[]{"Vote on", "§6https://teamholy.de/vote", "To receive §eCoins!"}),
            new NpcDefinition("webshop", "§f§lWeb Shop", UUID.fromString("eefecbcc-5ad4-4284-8ec7-ae7fc1434f5a"), "webinventory_npc", false,
                    new String[]{"Purchase", "§5Loot Boxes§f, §6Ranks§f, §cGifts §fand more", "With your §eCoins!"}),
            new NpcDefinition("bw_spec", "§6§lSpectate", UUID.fromString("60d97170-3d03-4562-918d-7ff7a493b68e"), "bw_spawn_spec", false, null),
            new NpcDefinition("wm2026", "§a§lWM 2026", UUID.fromString("f33ce1c3-6e0f-41f9-b2c8-0af8a015b1fb"), "wm2026_npc", false,
                    new String[]{"Play the", "§a§lFIFA World Cup 2026 §fEvent", "Win §eCoins §fand exclusive rewards!"})
    );

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (Lobby.getInstance().isPremiumLobby() && !player.hasPermission("teamholy.perk.premium")) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST,"§cshop.teamholy.de");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(CachedPlayerJoinEvent event) {
        Player player = event.getCachedBukkitPlayer().getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(20);
        player.sendTitle("§c§kN§c §6shop.teamholy.de §c§kd","§7§oour shop is now available");

        Bukkit.getScheduler().runTaskLater(Lobby.getInstance(),() -> {
            LobbyPlayer lobbyPlayer = new LobbyPlayer(player, event.getCachedBukkitPlayer());
            //player.teleport();
            Lobby.getInstance().getLobbyPlayerEntryHandler().put(player.getUniqueId(), lobbyPlayer);
            for (PotionEffect effect : player.getActivePotionEffects()) player.removePotionEffect(effect.getType());
            //player.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.getInventory().setBoots(new ItemBuilder(Material.LEATHER_BOOTS).setLeatherColor(Color.fromBGR(
                    lobbyPlayer.getPlayerRank().getBlue(), lobbyPlayer.getPlayerRank().getGreen(), lobbyPlayer.getPlayerRank().getRed()
            )).build());
            player.playSound(player.getLocation(),Sound.VILLAGER_YES,50f,50f);
           // player.teleport(BukkitCore.getInstance().getLocationManager().getLocation("lobby"));
            player.sendMessage(Lobby.getInstance().getPrefix() + "The §a§lWM 2026 §7event is now live§8! §7Click the §a§lWM 2026 §7NPC to join§8!");
        },1);
        Bukkit.getScheduler().runTaskLater(Lobby.getInstance(),() -> {
            for (NpcDefinition npc : NPCS) {
                Location location = BukkitCore.getInstance().getLocationManager().getLocation(npc.locationKey());
                if (location == null) {
                    Bukkit.getLogger().warning("[Lobby] Skipping NPC '" + npc.id() + "': location '" + npc.locationKey() + "' is not set");
                    continue;
                }
                NPCBuilder builder = new NPCBuilder(npc.id(), npc.displayName(), npc.uuid(), 50, 10, true, npc.skinDisplay(), location);
                if (npc.holoLines() != null) {
                    builder.addHolo(npc.holoLines());
                }
                builder.build(player);
            }
            PlayerCacheManager.CachedBukkitPlayer cachedPlayer = BukkitCore.getInstance().getPlayerCacheManager().getCachedPlayers().get(player.getUniqueId());
            if (cachedPlayer != null) {
                NPCEntry wmNpc = cachedPlayer.getNpcPlayer().getNpcs().get("wm2026");
                if (wmNpc != null) {
                    Lobby.getInstance().applyWm2026Skin(wmNpc);
                }
            }
        },10);
    }

    @EventHandler
    public void onSpawnLoc(PlayerSpawnLocationEvent event) {
        org.bukkit.Location spawn = BukkitCore.getInstance().getLocationManager().getLocation("lobby");
        if (spawn != null) {
            event.setSpawnLocation(spawn);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        Player player = event.getPlayer();
       Lobby.getInstance().getLobbyPlayerEntryHandler().remove(player.getUniqueId());
    }

}
