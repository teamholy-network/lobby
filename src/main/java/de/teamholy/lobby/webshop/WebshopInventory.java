package de.teamholy.lobby.webshop;

import de.teamholy.core.api.entities.player.PlayerProfile;
import de.teamholy.core.api.entities.stats.StatsProfile;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.utils.Inventory;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

public class WebshopInventory {

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public void openInventory(LobbyPlayer player) {
        getStatsPlayer(player.getPlayer(), profile -> {

            PlayerProfile playerProfile = BukkitCore.getInstance().getCoreAPI().getPlayerService().getEntity(player.getPlayer().getUniqueId(), () -> BukkitCore.getInstance().getCoreAPI().getPlayerService().getRepository().findFirstById(player.getPlayer().getUniqueId()));

            if (playerProfile == null) {
                return;
            }

            boolean linked = profile != null && profile.isPlayerProfileLinked();

            Bukkit.getScheduler().runTask(Lobby.getInstance(), () -> {
                if (!linked) {
                    player.getPlayer().sendMessage(Lobby.getInstance().getPrefix()+ "§cYou have to link your account first! §8(§e/link§8)");
                    player.executeBungeeCommand("link");
                    return;
                }

                Inventory inventory = new Inventory("§8» §6Webshop", 9 * 3);

                for (int i = 0; i < 9 * 3; i++) {
                    inventory.setItem(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build(), i);
                }

                inventory.setItem(new ItemBuilder(Material.CHEST).setName("§8» §eRank Loot Box").setLore(" ", " §7A Loot Box containing ranks ", " §7with different lengths and rarities! ", " §7you can open it on the §6§lwebsite! ", " §7(§ehttps://teamholy.de/profile/" + profile.getPlayerName() + "§7)", " ", "§7Buy one for §e50000 §6coins").build(), 13, inventoryClickEvent -> {


                    long coins = BukkitCore.getAPI().getCoinManager().getCoins(player.getPlayer().getUniqueId());

                    if (coins < 50000) {
                        player.getPlayer().sendMessage(Lobby.getInstance().getPrefix() + "§cYou don't have enough coins!");
                        player.getPlayer().closeInventory();
                        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.VILLAGER_NO, 1.0F, 100.0F);
                        return;
                    }

                        playerProfile.setCoins(playerProfile.getCoins() - 50000);

                        BukkitCore.getAPI().getPlayerService().saveEntity(playerProfile, true, true);
                        player.getPlayer().closeInventory();


                        player.updateCoinsScore();
                    addItemToPlayer(player.getPlayer().getUniqueId(), "564ff65c-d1fc-4cd5-947f-eb9ab4fe41b4");
                    player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.LEVEL_UP, 1.0F, 100.0F);
                    player.getPlayer().sendMessage(Lobby.getInstance().getPrefix() + "§7You have successfully bought §eRank Loot Box§7! Check your inventory on §6https://teamholy.de/profile/" + player.getPlayer().getDisplayName() + "§7!");

                });


                player.getPlayer().openInventory(inventory.getInventory());
            });
        });
    }

    public void addItemToPlayer(UUID playerId, String itemId) {
        String json = "{" + "\"type\":\"add_single\"," + "\"metadata\":{" + "\"itemsId\":[\"" + itemId + "\"]," + "\"playerId\":\"" + playerId + "\"," + "\"itemMessage\":\"In-game Purchase\"," + "\"ignore\":\"Z4mQk9SxA3N8LrD2FvHcWJ0E5pUo74B6GdKMTYas22pJAIlhjsohqoHOUAFh131313fsaHJh213wRfaC314eXh1IuVsqPOnb\"" + "}" + "}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create("https://teamholy.de/api/holy/manager/inventory")).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json; charset=utf-8").header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void getStatsPlayer(Player player, Consumer<StatsProfile> callback) {
        BukkitCore.getAPI().getStatsProfileService().getEntityAsync(player.getUniqueId(), () -> BukkitCore.getAPI().getStatsProfileService().getRepository().findFirstById(player.getUniqueId()), callback);
    }
}