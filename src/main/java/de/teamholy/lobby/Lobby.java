package de.teamholy.lobby;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.google.common.reflect.ClassPath;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.teamholy.core.bukkit.BukkitCore;
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
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutionException;

/* copyright by Yassino */
@Getter
@Setter
public class Lobby extends JavaPlugin {

  @Getter
  private static Lobby instance;
  private String prefix = "§6Lobby §8× §7";
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
    registerListener("de.teamholy.lobby.listeners");
    if (Bukkit.getServerName().contains("Premium")) {
      prefix = "§6PremiumLobby §8× §7";
      isPremiumLobby = true;
    }
    int maxPlayers = 250;
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
      try {
        int count = BukkitCore.getAPI().getCloudManager().getPlayerManager().getOnlineCountAsync()
            .get();
        for (Player player : Bukkit.getOnlinePlayers()) {
          player.setLevel(count);
          player.setExp((float) count / maxPlayers);

          if (player.getLocation().getY() < 0) {
            player.teleport(BukkitCore.getInstance().getLocationManager().getLocation("de/teamholy/lobby"));
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }

    }, 20, 20);

    getCommand("spawn").setExecutor(new SpawnCommand());
    getCommand("test1").setExecutor(new TestCommand());
    CloudNetDriver.getInstance().getEventManager().registerListener(new CloudListener());
    Bukkit.getPluginManager().registerEvents(bedwarsServerInventory, this);
    Bukkit.getPluginManager().registerEvents(bedwarsSpectateInventory, this);
  }


    public void sendActionBar(Player p, String nachricht) {
        CraftPlayer cp = (CraftPlayer) p;
        IChatBaseComponent cbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + nachricht + "\"}");
        PacketPlayOutChat ppoc = new PacketPlayOutChat(cbc, (byte) 2);
        cp.getHandle().playerConnection.sendPacket(ppoc);
    }


  @Override
  public void onDisable() {
    getHologramHandler().getHolograms().values().forEach(Hologram::delete);
  }

  private void registerListener(final String path) {
    try {
      final ClassLoader classLoader = this.getClass().getClassLoader();
      for (final ClassPath.ClassInfo info : ClassPath.from(classLoader).getTopLevelClasses(path)) {
        final Object obj = Class.forName(info.getName(), true, classLoader).newInstance();
        if (obj instanceof Listener) {
          this.getServer().getPluginManager().registerEvents((Listener) obj, this);
        }
      }
    } catch (Exception ignored) {
    }
  }
}
