
package de.teamholy.lobby.lobbyplayer;

import de.teamholy.core.api.entities.player.PlayerProfile;
import de.teamholy.core.api.entities.skin.SkinProfile;
import de.teamholy.core.api.utility.PlayerRank;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.utils.Inventory;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
public class FriendEntry {

    private static final String DEFAULT_SKIN_VALUE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGZk" +
            "NWJkZTk5NGUwYTY0N2FmMTgyMzY4MWE2MTNjMmJmYzNkOTczNmY4ODlkYmY4YzNiYmJhNWExM2Y4ZWQifX19";
    private static final int FRIENDS_PER_PAGE = 21;
    private static final int INVENTORY_ROWS = 5;

    private final UUID uuid;
    private final Player player;
    private final ConcurrentHashMap<UUID, Friend> friends = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Friend> requests = new ConcurrentHashMap<>();

    private int page = 1;
    private SortOption sortOption = SortOption.LASTONLINE_RECENTLY;

    public FriendEntry(Player player) {
        this.player = player;
        this.uuid = player.getUniqueId();
        loadFriendData();
    }

    private void loadFriendData() {
        BukkitCore.getAPI().getFriendService().getEntityAsync(
                uuid,
                () -> BukkitCore.getAPI().getFriendService().getRepository().findFirstById(uuid),
                friendProfile -> {
                    if (friendProfile == null) {
                        return;
                    }

                    long startTime = System.currentTimeMillis();

                    friendProfile.getFriendList().forEach(friendUUID -> loadFriendEntry(friendUUID, false));

                    BukkitCore.getAPI().getFriendService().saveEntity(friendProfile, true, true);

                    friendProfile.getFriendReqeustsList().forEach(requestUUID -> loadFriendEntry(requestUUID, true));

                    long duration = System.currentTimeMillis() - startTime;
                    Bukkit.getLogger().info("Loaded friends for " + player.getName() + " in " + duration + "ms");
                }
        );
    }

    public void updateFriendEntry(UUID friendUuid, String action, String extra) {
        switch (action.toLowerCase()) {
            case "add" -> loadFriendEntryAsync(friendUuid, false);
            case "remove" -> friends.remove(friendUuid);
            case "server_update" -> updateFriendServer(friendUuid, extra);
            case "online" -> updateFriendOnlineStatus(friendUuid, true);
            case "offline" -> updateFriendOnlineStatus(friendUuid, false);
        }
    }

    private void updateFriendServer(UUID friendUuid, String serverName) {
        Friend friend = friends.get(friendUuid);
        if (friend != null) {
            friend.setCurrentServer(serverName);
            friends.put(friendUuid, friend);
        }
    }

    private void updateFriendOnlineStatus(UUID friendUuid, boolean online) {
        Friend friend = friends.get(friendUuid);
        if (friend != null) {
            friend.setOnline(online);
            friends.put(friendUuid, friend);
        }
    }

    public void updateFriendRequestEntry(UUID requestUuid, String action) {
        switch (action.toLowerCase()) {
            case "send" -> loadFriendEntryAsync(requestUuid, true);
            case "remove" -> requests.remove(requestUuid);
        }
    }

    public void loadFriendEntry(UUID friendUuid, boolean isRequest) {
        try {
            PlayerProfile playerProfile = BukkitCore.getAPI().getPlayerService().getEntity(
                    friendUuid,
                    () -> BukkitCore.getAPI().getPlayerService().getRepository().findFirstById(friendUuid)
            );

            SkinProfile skinProfile = BukkitCore.getAPI().getSkinService().getEntity(
                    friendUuid,
                    () -> BukkitCore.getAPI().getSkinService().getRepository().findFirstById(friendUuid)
            );

            if (playerProfile == null) {
                Bukkit.getLogger().warning("PlayerProfile not found for UUID: " + friendUuid);
                return;
            }

            Friend friend = createFriendFromProfiles(friendUuid, playerProfile, skinProfile);

            if (isRequest) {
                requests.put(friendUuid, friend);
            } else {
                friends.put(friendUuid, friend);
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error loading friend entry for " + friendUuid, e);
        }
    }

    private Friend createFriendFromProfiles(UUID friendUuid, PlayerProfile playerProfile, SkinProfile skinProfile) {
        Friend friend = new Friend();

        String value = DEFAULT_SKIN_VALUE;
        String signature = "";
        if (skinProfile != null) {
            value = skinProfile.getValue();
            signature = skinProfile.getSignature();
        }

        PlayerRank playerRank = parsePlayerRank(playerProfile.getRank(), playerProfile.getPlayerName());

        friend.setUuid(friendUuid);
        friend.setOnline(playerProfile.isOnline());
        friend.setCurrentServer(playerProfile.getServerName());
        friend.setName(playerProfile.getPlayerName());
        friend.setLastJoin(playerProfile.getLastJoin());
        friend.setPlayerRank(playerRank);
        friend.setValue(value);
        friend.setSignature(signature);

        return friend;
    }

    private PlayerRank parsePlayerRank(String rankName, String playerName) {
        try {
            return PlayerRank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Unknown rank '" + rankName + "' for player " + playerName + " - using PLAYER as default");
            return PlayerRank.PLAYER;
        }
    }

    public void loadFriendEntryAsync(UUID friendUuid, boolean isRequest) {
        BukkitCore.getAPI().getExecutor().execute(() -> loadFriendEntry(friendUuid, isRequest));
    }

    public void openFriendGui(Player player, int page, SortOption sortOption) {
        if (page == 1) {
            this.page = 1;
            this.sortOption = SortOption.LASTONLINE_RECENTLY;
        }

        Inventory inventory = createBaseInventory("§8» §6Friends");
        List<Friend> sortedFriends = getSortedFriends(sortOption);

        addInventoryBorder(inventory);

        addRequestsButton(inventory, player);

        addNavigationButtons(inventory, player, page, sortedFriends, sortOption);

        addSortButton(inventory, player, sortOption);

        addFriendsToInventory(inventory, player, page, sortedFriends, sortOption);

        addInfoItem(inventory, page, sortedFriends);

        player.openInventory(inventory.getInventory());
    }

    private void openRequestGui(Player player) {
        Inventory inventory = createBaseInventory("§8» §6Requests");
        List<Friend> sortedRequests = getSortedRequests();

        addInventoryBorder(inventory);
        addBackToFriendsButton(inventory, player);
        addRequestsToInventory(inventory, player, sortedRequests);

        player.openInventory(inventory.getInventory());
    }

    private Inventory createBaseInventory(String title) {
        return new Inventory(title, 9 * INVENTORY_ROWS);
    }

    private void addInventoryBorder(Inventory inventory) {
        ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//");

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 0 || row == 4 || col == 0 || col == 8) {
                    inventory.setItem(glassPane.build(), row * 9 + col);
                }
            }
        }
    }

    private void addRequestsButton(Inventory inventory, Player player) {
        int requestCount = requests.size();
        String name = requestCount > 0
                ? "§8» §6Requests §7(§e" + requestCount + "§7)"
                : "§8» §6Requests";

        inventory.setItem(
                new ItemBuilder(Material.BOOK).setName(name).build(),
                4,
                event -> openRequestGui(player)
        );
    }

    private void addBackToFriendsButton(Inventory inventory, Player player) {
        inventory.setItem(
                new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3)
                        .setSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cm" +
                                "UvNzZjYmFlNzI0NmNjMmM2ZTg4ODU4NzE5OGM3OTU5OTc5NjY2YjRmNWE0MDg4ZjI0ZTI2ZTA3NWYxNDBhZTZjMyJ9fX0=", "")
                        .setName("§8» §6Friends")
                        .build(),
                4,
                event -> openFriendGui(player, 1, sortOption)
        );
    }

    private void addNavigationButtons(Inventory inventory, Player player, int page, List<Friend> friends, SortOption sortOption) {
        if (page > 1) {
            inventory.setItem(
                    new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3)
                            .setAttributs()
                            .setName("§8» §6Previous")
                            .setSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1Z" +
                                    "GFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==", "")
                            .build(),
                    38,
                    event -> {
                        this.page--;
                        player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 2F, 2F);
                        openFriendGui(player, this.page, sortOption);
                    }
            );
        } else {
            inventory.setItem(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build(), 38);
        }

        if (!getListForPage(page + 1, friends).isEmpty()) {
            inventory.setItem(
                    new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3)
                            .setAttributs()
                            .setName("§8» §6Next")
                            .setSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMT" +
                                    "I2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19", "")
                            .build(),
                    42,
                    event -> {
                        this.page++;
                        player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 2F, 2F);
                        openFriendGui(player, this.page, sortOption);
                    }
            );
        } else {
            inventory.setItem(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build(), 42);
        }
    }

    private void addSortButton(Inventory inventory, Player player, SortOption currentSort) {
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.addAll(Arrays.stream(SortOption.values())
                .map(option -> (currentSort == option ? "§a" : "§7") + option.getLore())
                .toList());
        lore.add(" ");

        inventory.setItem(
                new ItemBuilder(Material.HOPPER).setName("§8» §6Sort").setLore(lore).build(),
                18,
                event -> {
                    this.sortOption = getNextSortOption(currentSort);
                    player.playSound(player.getLocation(), Sound.CLICK, 1F, 10F);
                    openFriendGui(player, 1, this.sortOption);
                }
        );
    }

    private SortOption getNextSortOption(SortOption current) {
        return switch (current) {
            case LASTONLINE_RECENTLY -> SortOption.LASTONLINE_LONG;
            case LASTONLINE_LONG -> SortOption.NAME_A_TO_Z;
            case NAME_A_TO_Z -> SortOption.NAME_Z_TO_A;
            case NAME_Z_TO_A -> SortOption.RANK;
            case RANK -> SortOption.LASTONLINE_RECENTLY;
        };
    }

    private void addFriendsToInventory(Inventory inventory, Player player, int page, List<Friend> friends, SortOption sortOption) {
        getListForPage(page, friends).forEach(friend -> {
            ItemBuilder itemBuilder = createFriendItem(friend);
            inventory.setItem(
                    itemBuilder.build(),
                    inventory.getInventory().firstEmpty(),
                    event -> openFriendDetailGui(player, friend, sortOption)
            );
        });
    }

    private ItemBuilder createFriendItem(Friend friend) {
        if (friend.isOnline()) {
            return new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3)
                    .setSkullMeta(friend.getValue(), friend.getSignature())
                    .setName("§8» " + friend.getPlayerRank().getColorCode() + friend.getName())
                    .setLore("§aOnline §7on §6" + friend.getCurrentServer());
        } else {
            return new ItemBuilder(Material.SKULL_ITEM)
                    .setName("§8» " + friend.getPlayerRank().getColorCode() + friend.getName())
                    .setLore("§cOffline §7since §6" + formatDuration(System.currentTimeMillis() - friend.getLastJoin()));
        }
    }

    private void openFriendDetailGui(Player player, Friend friend, SortOption sortOption) {
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(uuid);
        ItemBuilder friendItem = createFriendItem(friend);
        ItemStack itemStack = friendItem.build();

        Inventory subInventory = new Inventory(itemStack.getItemMeta().getDisplayName(), 9);

        for (int i = 0; i < 8; i++) {
            subInventory.setItem(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//").build(), i);
        }

        subInventory.setItem(itemStack, 4);

        subInventory.setItem(
                new ItemBuilder(Material.ENDER_PEARL).setName("§8» §6Jump to friend").build(),
                0,
                event -> {
                    lobbyPlayer.executeBungeeCommand("friend jump " + friend.getName());
                    player.playSound(player.getLocation(), Sound.CLICK, 1F, 10F);
                    player.closeInventory();
                }
        );

        subInventory.setItem(
                new ItemBuilder(Material.CAKE).setName("§8» §6Invite to party").build(),
                1,
                event -> {
                    lobbyPlayer.executeBungeeCommand("party invite " + friend.getName());
                    player.playSound(player.getLocation(), Sound.CLICK, 1F, 10F);
                    openFriendGui(player, 1, sortOption);
                }
        );

        subInventory.setItem(
                new ItemBuilder(Material.BARRIER).setName("§8» §cDelete Friend").build(),
                8,
                event -> {
                    lobbyPlayer.executeBungeeCommand("friend remove " + friend.getName());
                    player.playSound(player.getLocation(), Sound.CLICK, 1F, 10F);
                    openFriendGui(player, 1, sortOption);
                }
        );

        player.openInventory(subInventory.getInventory());
    }

    private void addRequestsToInventory(Inventory inventory, Player player, List<Friend> requests) {
        getListForPage(1, requests).forEach(request -> {
            ItemBuilder itemBuilder = new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3)
                    .setSkullMeta(request.getValue(), request.getSignature())
                    .setName("§8» " + request.getName())
                    .setLore("", " §7leftclick to §aaccept", " §7rightclick to §cdeny", " ");

            inventory.setItem(
                    itemBuilder.build(),
                    inventory.getInventory().firstEmpty(),
                    event -> handleRequestAction(player, request, event.isLeftClick())
            );
        });
    }

    private void handleRequestAction(Player player, Friend request, boolean accept) {
        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(uuid);

        if (accept) {
            lobbyPlayer.executeBungeeCommand("friend accept " + request.getName());
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 10);
        } else {
            lobbyPlayer.executeBungeeCommand("friend deny " + request.getName());
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 10);
        }

        player.closeInventory();
    }

    private void addInfoItem(Inventory inventory, int page, List<Friend> friends) {
        long onlineCount = friends.stream().filter(Friend::isOnline).count();
        long offlineCount = friends.size() - onlineCount;

        inventory.setItem(
                new ItemBuilder(Material.SIGN)
                        .setName("§8» §6Informations")
                        .setLore(
                                " ",
                                " §7Page §6" + page + " §7of §c" + getMaxPages(friends),
                                " ",
                                " §aOnline friends§8: §6" + onlineCount,
                                " §cOffline friends§8: §6" + offlineCount,
                                ""
                        )
                        .build(),
                40
        );
    }

    private List<Friend> getSortedFriends(SortOption sortOption) {
        return friends.values().stream()
                .sorted(sortOption.getComparator())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Friend> getSortedRequests() {
        return requests.values().stream()
                .sorted((o1, o2) -> Boolean.compare(o2.isOnline(), o1.isOnline()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Friend> getListForPage(int page, List<Friend> list) {
        int startIndex = (page - 1) * FriendEntry.FRIENDS_PER_PAGE;
        int endIndex = Math.min(startIndex + FriendEntry.FRIENDS_PER_PAGE, list.size());

        if (startIndex >= list.size()) {
            return new ArrayList<>();
        }

        return list.subList(startIndex, endIndex);
    }

    private int getMaxPages(List<Friend> list) {
        return (int) Math.ceil((double) list.size() / FriendEntry.FRIENDS_PER_PAGE);
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append("d ");
            hours = hours % 24;
        }
        if (hours > 0) {
            result.append(hours).append("h ");
            minutes = minutes % 60;
        }
        if (minutes > 0 && days == 0) {
            result.append(minutes).append("m");
        }

        return result.toString().trim();
    }

    @Getter
    @AllArgsConstructor
    public enum SortOption {
        LASTONLINE_RECENTLY(
                " Last online §8(§6recently §7➡ §6long§8)",
                (o1, o2) -> {
                    boolean o1Online = o1.isOnline();
                    boolean o2Online = o2.isOnline();
                    if (o1Online && !o2Online) return -1;
                    if (!o1Online && o2Online) return 1;
                    return o1Online ? 0 : Long.compare(o2.getLastJoin(), o1.getLastJoin());
                }
        ),
        LASTONLINE_LONG(
                " Last online §8(§6long §7➡ §6recently§8)",
                Comparator.comparingLong(Friend::getLastJoin)
        ),
        NAME_A_TO_Z(
                " Name §8(§6A §7➡ §6Z§8)",
                Comparator.comparing(Friend::getName)
        ),
        NAME_Z_TO_A(
                " Name §8(§6Z §7➡ §6A§8)",
                (o1, o2) -> o2.getName().compareTo(o1.getName())
        ),
        RANK(
                " Ranks §8(§4Admin §7➡ §7Player§8)",
                Comparator.comparingInt(o -> o.getPlayerRank().getSortId())
        );

        private final String lore;
        private final Comparator<Friend> comparator;
    }
}