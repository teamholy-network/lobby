package de.teamholy.lobby.bedwars;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.teamholy.core.api.utility.TimeUtil;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/* copyright by Yassino */
@Getter
public class BedwarsSpectateInventory implements Listener {

    private final Inventory inventory = Bukkit.createInventory(null, 9 * 6, "§8» §6Spectate");
    private final HashMap<String, SpectateGame> gameHashMap = new HashMap<>();
    private final JsonParser jsonParser = new JsonParser();

    public BedwarsSpectateInventory() {

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 0 || row == 5 || col == 0 || col == 8 || col == 4) {
                    inventory.setItem(row * 9 + col, new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build());
                }
            }
        }

        updateItems();
        getIngameGames("BW").forEach(this::updateServer);
        getIngameGames("RBW").forEach(this::updateServer);


        Bukkit.getScheduler().scheduleSyncRepeatingTask(Lobby.getInstance(), () -> {


            gameHashMap.forEach((s, spectateGame) -> {

                List<String> lore = spectateGame.getItemBuilder().itemStack.getItemMeta().getLore();

                lore.set(0, startedSinceLore(spectateGame.getStartedSince()));

                spectateGame.getItemBuilder().setLore(lore);


                gameHashMap.put(s, spectateGame);
            });

            updateServerSlots();

        }, 20, 20);

    }


    public void updateItems() {
        inventory.setItem(2, new ItemBuilder(Material.BED).setName("§8» §6Bedwars")
                .setLore("   §6", " §fIngame players §8» §6" + getIngamePlayers("BW"), " §fIngame games §8» §6" + getIngameGames("BW").size(), "   §6")
                .build()
        );

        inventory.setItem(6, new ItemBuilder(Material.BLAZE_ROD).setName("§8» §6RushBW")
                .setLore("   §6", " §fIngame players §8» §6" + getIngamePlayers("RBW"), " §fIngame games §8» §6" + getIngameGames("RBW").size(), "   §6")
                .build()
        );
    }


    public List<ServiceInfoSnapshot> getIngameGames(String group) {
        return Lobby.getInstance().getCloudCacheHandler().getServerInfos().values().stream()
                .filter(info -> info.getConfiguration().getGroups()[0].equalsIgnoreCase(group + "8x1") ||
                        info.getConfiguration().getGroups()[0].equalsIgnoreCase(group + "4x2") ||
                        info.getConfiguration().getGroups()[0].equalsIgnoreCase(group + "2x1"))
                .filter(info -> info.getProperty(BridgeServiceProperty.EXTRA).isPresent() && !info.getProperty(BridgeServiceProperty.EXTRA).get().equals("1")).collect(Collectors.toList());
    }

    public int getIngamePlayers(String group) {
        int count = 0;
        for (ServiceInfoSnapshot ingameGame : getIngameGames(group)) {
            JsonObject jsonObject = serviceExtra(ingameGame);
            count = count + jsonObject.get("ingame").getAsInt();
        }
        return count;
    }


    private void updateServerSlots() {


        List<Integer> slots = new ArrayList<>(getSpectateSlots("BW"));
        slots.addAll(getSpectateSlots("RBW"));

        for (Integer slot : slots) {
            inventory.setItem(slot, null);
        }

        gameHashMap.forEach((s, spectateGame) -> {
            if (s.startsWith("RBW")) {

                for (Integer rbw : getSpectateSlots("RBW")) {
                    if (inventory.getItem(rbw) == null) {
                        inventory.setItem(rbw, spectateGame.getItemBuilder().itemStack);
                        break;
                    }
                }

            } else if (s.startsWith("BW")) {

                for (Integer bw : getSpectateSlots("BW")) {
                    if (inventory.getItem(bw) == null) {
                        inventory.setItem(bw, spectateGame.getItemBuilder().itemStack);
                        break;
                    }
                }
            }


        });
        updateItems();
    }

    private final String[] teams = new String[]{"Red", "Blue", "Yellow", "Green", "Black", "White", "Orange", "Pink"};

    public void updateServer(ServiceInfoSnapshot server) {


        for (String supportedServer : BedwarsServerInventory.SUPPORTED_SERVERS) {
            if (server.getConfiguration().getGroups()[0].equalsIgnoreCase(supportedServer)) {

                String serverName = server.getName();
                SpectateGame spectateGame = gameHashMap.get(serverName);


                if ((!server.getProperty(BridgeServiceProperty.EXTRA).isPresent())
                        || (server.getProperty(BridgeServiceProperty.EXTRA).isPresent() && server.getProperty(BridgeServiceProperty.EXTRA).get().equalsIgnoreCase("1"))) {
                    removeServer(serverName);
                    return;
                }

                JsonObject data = serviceExtra(server);
                long startedSince = data.get("start").getAsLong();
                int ingamePlayers = data.get("ingame").getAsInt();

                if (spectateGame == null) {


                    String[] splitMotd = server.getProperty(BridgeServiceProperty.MOTD).get().split(";");


                    spectateGame = new SpectateGame(new ItemBuilder(Material.valueOf(splitMotd[1]), ingamePlayers, Byte.parseByte(splitMotd[2]))
                            .setName("§8» §6" + serverName + " §8(§7" + splitMotd[0] + "§8)"), startedSince);
                }

                ItemBuilder itemBuilder = spectateGame.getItemBuilder();

                itemBuilder.setAmount(ingamePlayers);

                List<String> lore = new ArrayList<>();

                lore.add(startedSinceLore(startedSince));
                lore.add(" §6");
                for (String team : teams) {
                    List<String> teamLore = new ArrayList<>();
                    if (data.has(team)) {
                        JsonObject teamData = data.get(team).getAsJsonObject();

                        String colorcode = teamData.get("colorCode").getAsString();
                        boolean hasBed = teamData.get("hasBed").getAsBoolean();
                        JsonArray jsonElements = teamData.get("players").getAsJsonArray();

                        teamLore.add("      " + colorcode + "§l" + (hasBed ? "" : "§m") + team.toUpperCase(Locale.ROOT));



                        for (JsonElement jsonElement : jsonElements) {
                            JsonObject playerData = jsonElement.getAsJsonObject();

                            boolean isNicked = playerData.get("nicked").getAsBoolean();
                            boolean isDead = playerData.get("dead").getAsBoolean();
                            UUID uuid = UUID.fromString(playerData.get("uuid").getAsString());
                            String name = (playerData.get("name").getAsString());

                            String playerLore = " " + (isNicked ? "§7" : BukkitCore.getAPI().getCloudManager().getColor(uuid)) + (isDead ? "§m§o" : "") + name
                                    + "§r §8× §7Kills §c" + playerData.get("kills").getAsInt() + " §8︳ §7Beds §e" + playerData.get("beds").getAsInt();

                            teamLore.add(playerLore);

                        }


                        teamLore.add(" §6");
                    }

                    lore.addAll(teamLore);
                }


                lore.add("§8» §7click to spectate");
                itemBuilder.setLore(lore);
                spectateGame.setItemBuilder(itemBuilder);
                gameHashMap.put(serverName, spectateGame);


            }
        }

    }

    private String startedSinceLore(long startedSince) {
        return "§7Running since §a" + TimeUtil.beautifyTime(System.currentTimeMillis() - startedSince, TimeUnit.MILLISECONDS, true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        try {
            if (event.getInventory().getName().equals("§8» §6Spectate")) {
                List<Integer> bigList = new ArrayList<>();
                bigList.addAll(getSpectateSlots("BW"));
                bigList.addAll(getSpectateSlots("RBW"));

                bigList.forEach(integer -> {
                    if (event.getSlot() == integer && event.getCurrentItem() != null) {
                        BukkitCore.getAPI().getCloudManager().getPlayerManager().getPlayerExecutor(event.getWhoClicked().getUniqueId()).connect(getServerNameFromItemStack(event.getCurrentItem()));
                    }
                });
            }
        } catch (Exception ignored) {
        }

    }

    private String getServerNameFromItemStack(ItemStack stack) {
        String[] name = ChatColor.stripColor(stack.getItemMeta().getDisplayName()).split(" ");
        return name[1];
    }

    private List<Integer> getSpectateSlots(String group) {
        List<Integer> targetSlots = Arrays.asList(14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43);

        if (group.startsWith("BW")) {
            targetSlots = Arrays.asList(10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39);
        }

        return targetSlots;
    }

    private JsonObject serviceExtra(ServiceInfoSnapshot serviceInfoSnapshot) {
        String jsonString = serviceInfoSnapshot.getProperty(BridgeServiceProperty.EXTRA).get();

        return jsonParser.parse(jsonString).getAsJsonObject();
    }

    public void removeServer(String server) {
        gameHashMap.remove(server);
        updateItems();
    }


}
