package de.teamholy.lobby;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.google.common.reflect.ClassPath;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.npc.models.NPCEntry;
import de.teamholy.core.bukkit.npc.models.SkinEntry;
import de.teamholy.lobby.bedwars.BedwarsServerInventory;
import de.teamholy.lobby.bedwars.BedwarsSpectateInventory;
import de.teamholy.lobby.commands.SpawnCommand;
import de.teamholy.lobby.commands.TestCommand;
import de.teamholy.lobby.handlers.CloudCacheHandler;
import de.teamholy.lobby.handlers.HologramHandler;
import de.teamholy.lobby.handlers.StatsResetHandler;
import de.teamholy.lobby.leaderboard.LeaderboardInventory;
import de.teamholy.lobby.listeners.CloudListener;
import de.teamholy.lobby.lobbyplayer.LobbyPlayerHandler;
import de.teamholy.lobby.webshop.WebshopInventory;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

/* copyright by Yassino */
@Getter
@Setter
public class Lobby extends JavaPlugin {

  private static final String LOBBY_PREFIX = "§6Lobby §8× §7";
  private static final String PREMIUM_LOBBY_PREFIX = "§6PremiumLobby §8× §7";
  private static final int MAX_PLAYERS = 250;
  private static final long WM2026_BROADCAST_INTERVAL = 20L * 60 * 5;
  private static final String WM2026_BROADCAST = "The §a§lWM 2026 §7event is now live§8! §7Visit the §a§lWM 2026 §7NPC at spawn and win §eCoins §7and exclusive rewards§8!";

  // signed skin of GermanSider (f33ce1c3), fetched from sessionserver.mojang.com — pre-seeded into the
  // core's skin cache so the unreliable ashcon API is never asked (it serves stale/default skins)
  private static final UUID WM2026_SKIN_UUID = UUID.fromString("f33ce1c3-6e0f-41f9-b2c8-0af8a015b1fb");
  private static final String WM2026_SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc4MTI5MTk4Nzk4MSwKICAicHJvZmlsZUlkIiA6ICJmMzNjZTFjMzZlMGY0MWY5YjJjODBhZjhhMDE1YjFmYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJHZXJtYW5TaWRlciIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lZTNhZmYwN2NkZDZjYmQ3YTU4YzNiZTkyMWM2NTdlNDMwOWQ5ZWUyODE1YWMyOTQ3NDlmYjlhNzQ4NDkyMjUwIgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8yMzQwYzBlMDNkZDI0YTExYjE1YThiMzNjMmE3ZTllMzJhYmIyMDUxYjI0ODFkMGJhN2RlZmQ2MzVjYTdhOTMzIgogICAgfQogIH0KfQ==";
  private static final String WM2026_SKIN_SIGNATURE = "t4ywE0EhFchamQJFwCZDfm6v/Ep9kRfC4Ak8qIhpO4MF1TnmODXnOYdrcwfZuI4rGztAmcZqoiVQjJHOwvnCaGPV2+2otueDRD4/vHj4uJQ2hew3oGEpiyXAP1oqfC3m+izcE3rKKJrWlvzUoqOqvfLAQym/hYCYUs7UTVj0YqM1W58JC8bxgz7UIWnGsepxP3h103Io/QeFeKc25gcb1C7mctQitL63Bfvbxj87YUx6QrXJmgo6lVO/i3BGPJ+coVSRhq2TLOum3irZ7fXtSHkCpiM9e9pQeubo82xNw+pNtEnYwCYSPFrRY6Qc306gFMJJqLhgZQNob3PUWolVnVJlDZIJT4qrzHTSwz7kr5vdlcB177hYV+UGvnE872CkeufAQJDw3VUd5qQTn+HnjLvXAQEVkCQ/pI2O8p8BmLEpWW0TGGhEUNPXdYsCW4eAZ4yCJagKYM77rS197AyOXuCom62rVPmPlLXD1WGPfPODWm9JelFC4EG6yrqfu77WhQjqwQAL9iv9kQqUbYlrsBJV9XzVCMYgJF3GCsdygYxBfKdzLOelSuaGYjwM7oTl9efrt4CuUPTJ3JjyylQx73pn8dEJ9NpRDBnoOe0s6jKOtHLJYOUDYhBy399bf74LO8d2Yuqu0qocfNy3lM5M2WVGvnMTxKOw5va6TXNjwBA=";

  @Getter
  private static Lobby instance;
  private String prefix = LOBBY_PREFIX;
  private boolean isPremiumLobby;

  private LobbyPlayerHandler lobbyPlayerEntryHandler;
  private HologramHandler hologramHandler;
  private CloudCacheHandler cloudCacheHandler;
  private StatsResetHandler statsResetHandler;
  private BedwarsServerInventory bedwarsServerInventory;
  private BedwarsSpectateInventory bedwarsSpectateInventory;
  private LeaderboardInventory leaderboardInventory;
  private WebshopInventory webshopInventory;


  @Override
  public void onEnable() {
    instance = this;
    lobbyPlayerEntryHandler = new LobbyPlayerHandler();
    cloudCacheHandler = new CloudCacheHandler();
    hologramHandler = new HologramHandler(this);
    statsResetHandler = new StatsResetHandler();
    bedwarsServerInventory = new BedwarsServerInventory();
    bedwarsSpectateInventory = new BedwarsSpectateInventory();
    leaderboardInventory = new LeaderboardInventory();
    webshopInventory = new WebshopInventory();
    registerWm2026Skin();
    registerListener("de.teamholy.lobby.listeners");
    if (Bukkit.getServerName().contains("Premium")) {
      prefix = PREMIUM_LOBBY_PREFIX;
      isPremiumLobby = true;
    }
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
      try {
        int count = BukkitCore.getAPI().getCloudManager().getPlayerManager().getOnlineCountAsync()
            .get();
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.setLevel(count);
          player.setExp((float) count / MAX_PLAYERS);

          if (player.getLocation().getY() < 0) {
            org.bukkit.Location spawn = BukkitCore.getInstance().getLocationManager().getLocation("lobby");
            if (spawn != null) player.teleport(spawn);
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }

    }, 20, 20);

    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
      for (Player player : Bukkit.getOnlinePlayers()) {
        player.sendMessage(prefix + WM2026_BROADCAST);
      }
    }, WM2026_BROADCAST_INTERVAL, WM2026_BROADCAST_INTERVAL);

    getCommand("spawn").setExecutor(new SpawnCommand());
    getCommand("test1").setExecutor(new TestCommand());
    CloudNetDriver.getInstance().getEventManager().registerListener(new CloudListener());
    Bukkit.getPluginManager().registerEvents(bedwarsServerInventory, this);
    Bukkit.getPluginManager().registerEvents(bedwarsSpectateInventory, this);
  }

  private void registerWm2026Skin() {
    SkinEntry skinEntry = new SkinEntry();
    skinEntry.setUuid(WM2026_SKIN_UUID);
    skinEntry.setValue(WM2026_SKIN_VALUE);
    skinEntry.setSignature(WM2026_SKIN_SIGNATURE);
    BukkitCore.getInstance().getNpcService().getSkinEntryHashMap().put(WM2026_SKIN_UUID, skinEntry);
    getLogger().info("WM2026 skin cache seeded (build 2.0.0)");
  }

  // forces the signed texture onto the NPC profile, replacing anything the core's
  // async ashcon fetch may have attached — the profile ends up with exactly one textures property
  public void applyWm2026Skin(NPCEntry npcEntry) {
    PropertyMap properties = npcEntry.getGameProfile().getProperties();
    properties.removeAll("textures");
    properties.put("textures", new Property("textures", WM2026_SKIN_VALUE, WM2026_SKIN_SIGNATURE));
  }

  @Override
  public void onDisable() {
    hologramHandler.getHolograms().values().forEach(Hologram::delete);
  }

  private void registerListener(final String path) {
    try {
      final ClassLoader classLoader = this.getClass().getClassLoader();
      for (final ClassPath.ClassInfo info : ClassPath.from(classLoader).getTopLevelClasses(path)) {
        final Object obj = Class.forName(info.getName(), true, classLoader).getDeclaredConstructor().newInstance();
        if (obj instanceof Listener) {
          this.getServer().getPluginManager().registerEvents((Listener) obj, this);
        }
      }
    } catch (Exception ignored) {
    }
  }
}
