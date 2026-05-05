package de.teamholy.lobby.bedwars;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/* copyright by Yassino */
@Getter
public class BedwarsServerInventory implements Listener {

    public final HashMap<String, Inventory> inventoryHashMap = new HashMap<>();
    public static final String[] SUPPORTED_SERVERS = new String[]{"BW2x1", "BW4x2", "BW8x1", "RBW4x2", "RBW8x1", "RBW2x1"};


    public BedwarsServerInventory() {
        inventoryHashMap.put("BW2x1",Bukkit.createInventory(null, 9,"§6BW2x1"));
        inventoryHashMap.put("BW4x2",Bukkit.createInventory(null, 9,"§6BW4x2"));
        inventoryHashMap.put("BW8x1",Bukkit.createInventory(null, 9,"§6BW8x1"));

        inventoryHashMap.put("RBW2x1",Bukkit.createInventory(null, 9,"§6RBW2x1"));
        inventoryHashMap.put("RBW4x2",Bukkit.createInventory(null, 9,"§6RBW4x2"));
        inventoryHashMap.put("RBW8x1",Bukkit.createInventory(null, 9,"§6RBW8x1"));

        for (String supportedServer : SUPPORTED_SERVERS) {
            updateInventory(supportedServer);
        }
    }



    public void openRushInventory(Player player) {
        de.teamholy.core.bukkit.utils.Inventory inventory = new de.teamholy.core.bukkit.utils.Inventory("§8» §6RushBW", 9 * 4);
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId());

        for (int i = 0; i < 9 * 4; i++) {
            inventory.setItem(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build(), i);
        }


        inventory.setItem(new ItemBuilder(Material.BLAZE_ROD)
                .setAmount(Lobby.getInstance().getCloudCacheHandler().getOnlineCount("RBW2x1")).setName("§8» §62x1").build(), 10);
        inventory.setItem(new ItemBuilder(Material.BLAZE_ROD)
                .setAmount(Lobby.getInstance().getCloudCacheHandler().getOnlineCount("RBW8x1")).setName("§8» §68x1").build(), 13);
        inventory.setItem(new ItemBuilder(Material.BLAZE_ROD)
                .setAmount(Lobby.getInstance().getCloudCacheHandler().getOnlineCount("RBW4x2")).setName("§8» §64x2").build(), 16);

        inventory.setItem(new ItemBuilder(Material.FIREWORK_CHARGE).setName("§6§lJOIN").setLore("§frightlick §7to show all server", "§fleftclick §7to quickjoin").build(), 19, inventoryClickEvent -> {
            if (inventoryClickEvent.getClick().isRightClick()) player.openInventory(getInventoryHashMap().get("RBW2x1"));
            else if (inventoryClickEvent.getClick().isLeftClick()) lobbyPlayer.sendPlayerToGroup("RBW2x1");
        });
        inventory.setItem(new ItemBuilder(Material.FIREWORK_CHARGE).setName("§6§lJOIN").setLore("§frightlick §7to show all server", "§fleftclick §7to quickjoin").build(), 22, inventoryClickEvent -> {
            if (inventoryClickEvent.getClick().isRightClick()) player.openInventory(getInventoryHashMap().get("RBW8x1"));
            else if (inventoryClickEvent.getClick().isLeftClick()) lobbyPlayer.sendPlayerToGroup("RBW8x1");
        });
        inventory.setItem(new ItemBuilder(Material.FIREWORK_CHARGE).setName("§6§lJOIN").setLore("§frightlick §7to show all server", "§fleftclick §7to quickjoin").build(), 25, inventoryClickEvent -> {

            if (inventoryClickEvent.getClick().isRightClick()) player.openInventory(getInventoryHashMap().get("RBW4x2"));
            else if (inventoryClickEvent.getClick().isLeftClick()) lobbyPlayer.sendPlayerToGroup("RBW4x2");

        });
        player.openInventory(inventory.getInventory());
    }

    public void openBWInventory(Player player) {
        de.teamholy.core.bukkit.utils.Inventory inventory = new de.teamholy.core.bukkit.utils.Inventory("§8» §6Bedwars", 9 * 4);
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId());

        for (int i = 0; i < 9 * 4; i++) {
            inventory.setItem(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build(), i);
        }


        inventory.setItem(new ItemBuilder(Material.BED)
                .setAmount(Lobby.getInstance().getCloudCacheHandler().getOnlineCount("BW2x1")).setName("§8» §62x1").build(), 10);
        inventory.setItem(new ItemBuilder(Material.BED)
                .setAmount(Lobby.getInstance().getCloudCacheHandler().getOnlineCount("BW8x1")).setName("§8» §68x1").build(), 13);
        inventory.setItem(new ItemBuilder(Material.BED)
                .setAmount(Lobby.getInstance().getCloudCacheHandler().getOnlineCount("BW4x2")).setName("§8» §64x2").build(), 16);

        inventory.setItem(new ItemBuilder(Material.FIREWORK_CHARGE).setName("§6§lJOIN").setLore("§frightlick §7to show all server", "§fleftclick §7to quickjoin").build(), 19, inventoryClickEvent -> {
            if (inventoryClickEvent.getClick().isRightClick()) player.openInventory(getInventoryHashMap().get("BW2x1"));
            else if (inventoryClickEvent.getClick().isLeftClick()) lobbyPlayer.sendPlayerToGroup("BW2x1");
        });
        inventory.setItem(new ItemBuilder(Material.FIREWORK_CHARGE).setName("§6§lJOIN").setLore("§frightlick §7to show all server", "§fleftclick §7to quickjoin").build(), 22, inventoryClickEvent -> {
            if (inventoryClickEvent.getClick().isRightClick()) player.openInventory(getInventoryHashMap().get("BW8x1"));
            else if (inventoryClickEvent.getClick().isLeftClick()) lobbyPlayer.sendPlayerToGroup("BW8x1");
        });
        inventory.setItem(new ItemBuilder(Material.FIREWORK_CHARGE).setName("§6§lJOIN").setLore("§frightlick §7to show all server", "§fleftclick§7to quickjoin").build(), 25, inventoryClickEvent -> {

            if (inventoryClickEvent.getClick().isRightClick()) player.openInventory(getInventoryHashMap().get("BW4x2"));
            else if (inventoryClickEvent.getClick().isLeftClick()) lobbyPlayer.sendPlayerToGroup("BW4x2");

        });
        player.openInventory(inventory.getInventory());
    }

    public int getBedwarsPlayers() {
        int twoxone = Lobby.getInstance().getCloudCacheHandler().getOnlineCount("BW2x1");
        int fourxtwo = Lobby.getInstance().getCloudCacheHandler().getOnlineCount("BW4x2");
        int eightxone = Lobby.getInstance().getCloudCacheHandler().getOnlineCount("BW8x1");
        return Math.min(64, twoxone + fourxtwo + eightxone);
    }

    public int getRushBWPlayers() {
        int twoxone = Lobby.getInstance().getCloudCacheHandler().getOnlineCount("RBW2x1");
        int fourxtwo = Lobby.getInstance().getCloudCacheHandler().getOnlineCount("RBW4x2");
        int eightxone = Lobby.getInstance().getCloudCacheHandler().getOnlineCount("RBW8x1");
        return Math.min(64, twoxone + fourxtwo + eightxone);
    }

    public void updateInventory(String group) {
        inventoryHashMap.forEach((s, itemStacks) -> {
            if (s.equalsIgnoreCase(group)) {
                List<ServiceInfoSnapshot> gameServices = Lobby.getInstance().getCloudCacheHandler().getServerInfos().values().stream()
                        .filter(service -> service.isConnected())
                        .filter(info -> info.getConfiguration().getGroups()[0].equals(group))
                        .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getProperty(BridgeServiceProperty.STATE).isPresent()
                                && serviceInfoSnapshot.getProperty(BridgeServiceProperty.STATE).get().equalsIgnoreCase("LOBBY"))
                        .sorted(Comparator.comparingInt(info -> info.getProperty(BridgeServiceProperty.ONLINE_COUNT).get())).collect(Collectors.toList());

                AtomicInteger i = new AtomicInteger();
                for (int il = 0; il < 9; il++) {
                    inventoryHashMap.get(group).setItem(il,new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build());
                }


                gameServices.forEach(gameService -> {
                    String motdProperty = gameService.getProperty(BridgeServiceProperty.MOTD).get();

                    if (motdProperty.equalsIgnoreCase("END")) return;

                    String[] splitMotd = motdProperty.split(";");
                    boolean voting = false;

                    if (splitMotd.length <= 2) return; // Did this so we don't get a ArrayIndexOutOfBoundsException

                    if (splitMotd[2].equalsIgnoreCase("null")) {
                        voting = true;
                    }

                    int count = gameService.getProperty(BridgeServiceProperty.ONLINE_COUNT).get();

                    if (splitMotd.length > 4) { // Ensure we can access 3&4 index, so we don't throw an Exception
                        if (count != 0) {
                            inventoryHashMap.get(group).setItem(i.getAndIncrement(), new ItemBuilder((voting ? Material.PAPER : Material.valueOf(splitMotd[3])), count, (voting ? 0 : Byte.parseByte(splitMotd[4])))
                                    .setAttributs()
                                    .setEnchantments(Enchantment.DURABILITY,1)
                                    .setLore("§7Map §8× §6" + (voting ? "voting..." : splitMotd[2]), "§7Players §8× §6" + splitMotd[0] + "§7/§c" + splitMotd[1])
                                    .setName("§8» §6" + gameService.getName()).build());
                        } else {
                            inventoryHashMap.get(group).setItem(i.getAndIncrement(), new ItemBuilder((voting ? Material.PAPER : Material.valueOf(splitMotd[3])), 1, (voting ? 0 : Byte.parseByte(splitMotd[4])))
                                    .setLore("§7Map §8× §6" + (voting ? "voting..." : splitMotd[2]), "§7Players §8× §6" + splitMotd[0] + "§7/§c" + splitMotd[1])
                                    .setName("§8» §6" + gameService.getName()).build());
                        }
                    }
                });

            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            inventoryHashMap.forEach((s, inventory) -> {
                if (event.getInventory().getName().equalsIgnoreCase(inventory.getName())) {
                    Player player = (Player) event.getWhoClicked();
                    if (event.getCurrentItem().getItemMeta().getDisplayName().startsWith("§8» §6")) {
                        String server = event.getCurrentItem().getItemMeta().getDisplayName().replace("§8» §6","");
                        BukkitCore.getAPI().getCloudManager().getPlayerManager().getPlayerExecutor(player.getUniqueId()).connect(server);
                        player.closeInventory();
                    }
                }
            });

        } catch (Exception ignored) {}
    }

}
