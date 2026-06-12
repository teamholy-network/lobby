package de.teamholy.lobby.lobbyplayer;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.teamholy.core.api.entities.clan.Clan;
import de.teamholy.core.api.entities.friend.FriendProfile;
import de.teamholy.core.api.entities.game.GameProfile;
import de.teamholy.core.api.entities.player.PlayerProfile;
import de.teamholy.core.api.utility.PartyInviteAllowance;
import de.teamholy.core.api.utility.PlayerRank;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.manager.PlayerCacheManager;
import de.teamholy.core.bukkit.npc.models.NPCEntry;
import de.teamholy.core.bukkit.perks.enums.PerkType;
import de.teamholy.core.bukkit.utils.Inventory;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.core.bukkit.utils.ScoreboardAPI;
import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.handlers.CloudCacheHandler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Per-player lobby session state: scoreboard, hotbar items, settings GUI,
 * friend data and server-selection inventories.
 */
@Getter
@Setter
public class LobbyPlayer {

    private static final int INVENTORY_BORDER_COLOR = 15;
    private static final int MAX_ITEM_STACK = 64;
    private static final String NO_CLAN_TEXT = "§cno clan";

    private final Player player;
    private final ScoreboardAPI scoreboardAPI;
    private PlayerRank playerRank;
    private final FriendEntry friendEntry;

    private GameProfile gameProfile;
    private String onlineTimeString;
    private String clanNameString;

    private boolean isInArena;
    private boolean fly;
    private boolean collectedNameMCReward;
    private boolean collectedLabyModReward;

    public LobbyPlayer(Player player, PlayerCacheManager.CachedBukkitPlayer cachedBukkitPlayer) {
        this.player = player;
        this.playerRank = cachedBukkitPlayer.getRank();
        this.scoreboardAPI = new ScoreboardAPI();

        initializePlayer();
        this.friendEntry = new FriendEntry(player);
        loadGameProfileAsync();
    }

    private void initializePlayer() {
        scoreboardAPI.createScoreboard(player, "§6");
        player.getInventory().clear();
        setScoreboard();
        setInventory();
    }

    private void loadGameProfileAsync() {
        BukkitCore.getAPI().getGameService().getEntityAsync(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getGameService().getRepository().findFirstById(player.getUniqueId()),
                this::setGameProfile
        );
    }

    public void executeBungeeCommand(String command) {
        BukkitCore.getAPI().getCloudManager().sendCloudMessage(
                "command",
                "command",
                JsonDocument.newDocument("uuid", player.getUniqueId()).append("command", command)
        );
    }

    public void createNPC(String name, UUID uuid, Location location) {
        BukkitCore.getInstance().getPlayerCacheManager()
                .getCachedPlayers()
                .get(player.getUniqueId())
                .getNpcPlayer()
                .getNpcs()
                .put(ChatColor.stripColor(name),
                        new NPCEntry(name, uuid, location, 100, 10, true, true).setPlayer(player)
                );
    }

    private String formatOnlineTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours);
        return String.format("§6%dh %dm", hours, minutes);
    }

    public void updateOnlineTime() {
        BukkitCore.getAPI().getPlayerService().getEntityAsync(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getPlayerService().getRepository().findFirstById(player.getUniqueId()),
                playerProfile -> {
                    onlineTimeString = formatOnlineTime(playerProfile.getOnlineTime());
                    scoreboardAPI.updateLine(3, " §7Playtime§8: §6" + onlineTimeString);
                }
        );
    }

    public void updateClanTagScore() {
        BukkitCore.getAPI().getClanPlayerService().getEntityAsync(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getClanPlayerService().getRepository().findFirstById(player.getUniqueId()),
                clanPlayerProfile -> {
                    if (clanPlayerProfile != null) {
                        Clan clan = BukkitCore.getAPI().getClanManager().getClanById(clanPlayerProfile.getClanId());
                        clanNameString = clan.getColor() + clan.getName();
                    } else {
                        clanNameString = NO_CLAN_TEXT;
                    }
                    scoreboardAPI.updateLine(5, " §7Clan§8: " + clanNameString);
                }
        );
    }

    public void updateCoinsScore() {
        BukkitCore.getAPI().getCoinManager().getCoinsAsync(
                player.getUniqueId(),
                coins -> scoreboardAPI.updateLine(4, " §7Coins§8: §6" + BukkitCore.getAPI().getCoinManager().formatInteger(coins))
        );
    }

    public void updateRankScore() {
        scoreboardAPI.updateLine(7, " §7Rank§8: " + playerRank.getColorCode() + playerRank.getName());
    }

    public void setScoreboard() {
        scoreboardAPI.setLine(9, "§8§m                       §7");
        scoreboardAPI.setLine(8, "§7");
        scoreboardAPI.setLine(7, " §7Rank§8: " + playerRank.getColorCode() + playerRank.getName());
        scoreboardAPI.setLine(6, "§2");
        scoreboardAPI.setLine(2, "§5");
        scoreboardAPI.setLine(1, "§8§m                       §7");
        scoreboardAPI.setLine(0, "§o" + Wrapper.getInstance().getCurrentServiceInfoSnapshot().getServiceId().getName());

        CountDownLatch latch = new CountDownLatch(2);

        loadClanForScoreboard(latch);

        loadPlayerDataForScoreboard(latch);

        Bukkit.getScheduler().runTaskAsynchronously(Lobby.getInstance(), () -> {
            try {
                if (latch.await(5, TimeUnit.SECONDS)) {
                    Bukkit.getScheduler().runTask(Lobby.getInstance(), scoreboardAPI::build);
                } else {
                    Bukkit.getLogger().warning("Scoreboard loading timeout for player " + player.getName());
                }
            } catch (InterruptedException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error loading scoreboard for " + player.getName(), e);
                Thread.currentThread().interrupt();
            }
        });
    }

    private void loadClanForScoreboard(CountDownLatch latch) {
        BukkitCore.getAPI().getClanPlayerService().getEntityAsync(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getClanPlayerService().getRepository().findFirstById(player.getUniqueId()),
                clanPlayerProfile -> {
                    try {
                        if (clanPlayerProfile != null) {
                            Clan clan = BukkitCore.getAPI().getClanManager().getClanById(clanPlayerProfile.getClanId());
                            clanNameString = clan.getColor() + clan.getName();
                        } else {
                            clanNameString = NO_CLAN_TEXT;
                        }
                        scoreboardAPI.setLine(5, " §7Clan§8: " + clanNameString);
                    } finally {
                        latch.countDown();
                    }
                }
        );
    }

    private void loadPlayerDataForScoreboard(CountDownLatch latch) {
        BukkitCore.getAPI().getPlayerService().getEntityAsync(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getPlayerService().getRepository().findFirstById(player.getUniqueId()),
                playerProfile -> {
                    try {
                        onlineTimeString = formatOnlineTime(playerProfile.getOnlineTime());
                        scoreboardAPI.setLine(4, " §7Coins§8: §6" + BukkitCore.getAPI().getCoinManager().formatInteger(playerProfile.getCoins()));
                        scoreboardAPI.setLine(3, " §7Playtime§8: §6" + onlineTimeString);
                    } finally {
                        latch.countDown();
                    }
                }
        );
    }

    public void openGameSubInventory(String group, Material material) {
        List<ServiceInfoSnapshot> gameServices = getAvailableServices(group);

        if (gameServices.isEmpty()) {
            sendNoServerMessage(group);
            return;
        }

        if (gameServices.size() == 1) {
            connectToServer(gameServices.get(0).getName());
            return;
        }

        openServerSelectionInventory(group, material, gameServices);
    }

    private List<ServiceInfoSnapshot> getAvailableServices(String group) {
        return Lobby.getInstance().getCloudCacheHandler()
                .getServerInfos()
                .values()
                .stream()
                .filter(info -> info.getConfiguration().getGroups()[0].equals(group))
                .filter(info -> !info.getProperty(BridgeServiceProperty.IS_STARTING).orElse(true))
                .sorted(Comparator.comparingInt(info -> info.getServiceId().getTaskServiceId()))
                .collect(Collectors.toList());
    }

    private void sendNoServerMessage(String group) {
        player.sendMessage(Lobby.getInstance().getPrefix() + "§cthere is currently no §6" + group + " §cserver available§4!");
    }

    private void connectToServer(String serverName) {
        BukkitCore.getAPI().getCloudManager()
                .getPlayerManager()
                .getPlayerExecutor(player.getUniqueId())
                .connect(serverName);
    }

    private void openServerSelectionInventory(String group, Material material, List<ServiceInfoSnapshot> services) {
        Inventory inventory = new Inventory("§8» §6" + group, 9);
        addInventoryBorder(inventory, 9);

        AtomicInteger slot = new AtomicInteger(0);
        services.forEach(service -> {
            int onlineCount = service.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0);
            inventory.setItem(
                    new ItemBuilder(material, Math.min(MAX_ITEM_STACK, onlineCount))
                            .setLore("§7Players §8× §6" + onlineCount)
                            .setName("§8» §6" + service.getName())
                            .build(),
                    slot.getAndIncrement(),
                    event -> connectToServer(service.getName())
            );
        });

        player.openInventory(inventory.getInventory());
    }

    public void sendPlayerToGroup(String group) {
        List<ServiceInfoSnapshot> availableServers = findBestServers(group);

        if (availableServers.isEmpty()) {
            player.sendMessage(Lobby.getInstance().getPrefix() + "Could not find a §c" + group + " §7server");
            return;
        }

        connectToServer(availableServers.get(0).getName());
    }

    private List<ServiceInfoSnapshot> findBestServers(String group) {
        List<ServiceInfoSnapshot> servers = Lobby.getInstance().getCloudCacheHandler()
                .getServerInfos()
                .values()
                .stream()
                .filter(info -> info.getConfiguration().getGroups()[0].equals(group))
                .filter(info -> !info.getProperty(BridgeServiceProperty.IS_IN_GAME).orElse(true))
                .filter(info -> info.getProperty(BridgeServiceProperty.STATE).isPresent())
                .filter(info -> "LOBBY".equalsIgnoreCase(info.getProperty(BridgeServiceProperty.STATE).get()))
                .filter(info -> "1".equals(info.getProperty(BridgeServiceProperty.EXTRA).orElse("")))
                .filter(info -> !info.getProperty(BridgeServiceProperty.IS_FULL).orElse(true))
                .sorted(Comparator.comparingInt(info -> info.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0)))
                .collect(Collectors.toList());

        Collections.reverse(servers);
        return servers;
    }

    public void openLobbySwitcher() {
        Inventory inventory = new Inventory("§8» §6Lobby Switcher", 9);
        addInventoryBorder(inventory, 9);

        AtomicInteger slot = new AtomicInteger(0);
        String currentServer = Wrapper.getInstance().getCurrentServiceInfoSnapshot().getConfiguration().getGroups()[0];

        addLobbyServers(inventory, "PremiumLobby", Material.GLOWSTONE_DUST, currentServer, slot, true);

        addLobbyServers(inventory, "Lobby", Material.SUGAR, currentServer, slot, false);

        player.openInventory(inventory.getInventory());
    }

    private void addLobbyServers(Inventory inventory, String group, Material material, String currentServer, AtomicInteger slot, boolean requiresPermission) {
        List<ServiceInfoSnapshot> servers = getAvailableServices(group);

        servers.forEach(service -> {
            int onlineCount = service.getProperty(BridgeServiceProperty.ONLINE_COUNT).orElse(0);
            boolean isCurrent = service.getName().equalsIgnoreCase(currentServer);

            ItemBuilder builder = new ItemBuilder(material, Math.min(MAX_ITEM_STACK, onlineCount))
                    .setName("§8» §6" + service.getName());

            if (isCurrent) {
                builder.setEnchantments(Enchantment.KNOCKBACK, 1)
                        .setAttributs()
                        .setLore("§7Players §8× §6" + onlineCount, "§cYou are currently on this lobby");
                inventory.setItem(builder.build(), slot.getAndIncrement());
            } else {
                if (requiresPermission) {
                    builder.setLore("§7You need §6Premium §7or above to join", "§7Players §8× §6" + onlineCount);
                    inventory.setItem(builder.build(), slot.getAndIncrement(), event -> {
                        if (player.hasPermission("teamholy.fulljoin")) {
                            connectToServer(service.getName());
                        }
                    });
                } else {
                    builder.setLore("§7Players §8× §6" + onlineCount);
                    inventory.setItem(builder.build(), slot.getAndIncrement(), event -> connectToServer(service.getName()));
                }
            }
        });
    }

    public void openGamesInventory() {
        Inventory inventory = new Inventory("§8» §6Games", 36);
        addInventoryBorder(inventory, 36);

        CloudCacheHandler cacheHandler = Lobby.getInstance().getCloudCacheHandler();
        int lobbyCount = cacheHandler.getOnlineCount("Lobby") + cacheHandler.getOnlineCount("PremiumLobby");

        inventory.setItem(
                new ItemBuilder(Material.SLIME_BALL, Math.min(MAX_ITEM_STACK, lobbyCount))
                        .setName("§8» §6Spawn")
                        .build(),
                10,
                event -> player.teleport(BukkitCore.getInstance().getLocationManager().getLocation("lobby"))
        );

        addGameModeItem(inventory, 21, Material.STICK, "MLGRush",
                "§7Fight against an opponent in a 1v1 or 1v1v1v1 mode!",
                "§7Try to destroy their bed and knock them down.",
                "§7It's also great for practicing, clutching, and improving your skills.",
                "Multiplayer"
        );

        addGameModeItem(inventory, 13, Material.RED_SANDSTONE, "Clutches",
                "§7The perfect mode for practicing your clutching skills §7§lalone§7!",
                "§7Use our ReduceBot, which behaves just like a real player, and",
                "§7it offers modes like reduce, clutch, diagonal-clutch, and multi-reduce.",
                "Singleplayer"
        );

        addGameModeItem(inventory, 14, Material.SANDSTONE, "KnockbackFFA",
                "§7Fight against other players on small platforms!",
                "§7You can practice your PvP and bow skills",
                "§7with different maps & perks.",
                "Multiplayer"
        );

        int bedwarsCount = Lobby.getInstance().getBedwarsServerInventory().getBedwarsPlayers() +
                Lobby.getInstance().getBedwarsServerInventory().getRushBWPlayers();
        inventory.setItem(
                new ItemBuilder(Material.BED, Math.min(MAX_ITEM_STACK, bedwarsCount))
                        .setName("§8» §6Bedwars §7& §cRushBW")
                        .setLore(
                                " ",
                                " §7The most intense PvP game, combining",
                                " §7all game modes at once: Bedwars, German-style.",
                                " §7Break the enemy's bed and knock them down",
                                " §7in 2x1, 4x2, and 8x1 variants.",
                                " §7Introducing §cRushBW§8: §7a faster Bedwars mode for",
                                " §7the best German mouse abuse experience",
                                " ",
                                " §fMultiplayer",
                                " §7Currently playing§8: §6" + bedwarsCount + " §7players",
                                " ",
                                "§8» §7Click to §6§nteleport"
                        )
                        .build(),
                15,
                event -> player.teleport(BukkitCore.getInstance().getLocationManager().getLocation("bw_spawn"))
        );

        addGameModeItem(inventory, 16, Material.IRON_SWORD, "SGFFA",
                "§7The all-time favorite Survival Games, but with a little",
                "§7twist: we combined Survival Games and Free-For-All in one mode",
                "§7Try to think fast and get out of difficult situations.",
                "Multiplayer"
        );

        addGameModeItem(inventory, 12, Material.IRON_PICKAXE, "Bridge",
                "§7Practice your building skills",
                "§7by fast building a §ebridge §7to the end island.",
                "§7Best way to practice your §atelly§8, §emoonwalk§8, §cgodbridge§8....",
                "Singleplayer"
        );

        addGameModeItem(inventory, 22, Material.GRASS, "SkyWarsFFA §c§lNEW",
                "§7Practice your combat skills",
                "§7by fighting against other players on",
                "§7skywars environment islands.",
                "Multiplayer"
        );

        addGameModeItem(inventory, 23, Material.DIAMOND_SWORD, "Duels",
                "§7Fight against other players in intense 1v1 duels!",
                "§7Choose your kit and prove your skills",
                "§7in various combat scenarios.",
                "Multiplayer"
        );

        player.openInventory(inventory.getInventory());
    }

    private void addGameModeItem(Inventory inventory, int slot, Material material, String gameMode,
                                 String desc1, String desc2, String desc3, String playerType) {
        int onlineCount = Lobby.getInstance().getCloudCacheHandler().getOnlineCount(gameMode.split(" ")[0]);

        inventory.setItem(
                new ItemBuilder(material, Math.min(MAX_ITEM_STACK, onlineCount))
                        .setName("§8» §6" + gameMode)
                        .setLore(
                                " ",
                                " " + desc1,
                                " " + desc2,
                                " " + desc3,
                                " ",
                                " §f" + playerType,
                                " §7Currently playing§8: §6" + onlineCount + " §7players",
                                " ",
                                "§8» §7Click to §6§nconnect"
                        )
                        .build(),
                slot,
                event -> openGameSubInventory(gameMode.split(" ")[0], material)
        );
    }

    public void openSettings() {
        Inventory inventory = new Inventory("§8» §6Settings", 27);
        addInventoryBorder(inventory, 27);

        FriendProfile friendProfile = loadFriendProfile();
        PlayerProfile playerProfile = loadPlayerProfile();

        if (friendProfile == null || playerProfile == null) {
            player.sendMessage(Lobby.getInstance().getPrefix() + "§cError loading settings!");
            return;
        }

        AtomicBoolean friendUpdate = new AtomicBoolean(false);
        AtomicBoolean nickUpdate = new AtomicBoolean(false);

        inventory.setItem(
                new ItemBuilder(Material.DIAMOND).setName("§8» §6Perks").build(),
                22,
                event -> BukkitCore.getInstance().getPerkManager().openMainPerkInventory(player)
        );

        addToggleSetting(inventory, 11, Material.BOOK, "Allow Friend requests",
                friendProfile.isAllowFriendRequests(),
                () -> {
                    friendProfile.setAllowFriendRequests(!friendProfile.isAllowFriendRequests());
                    friendUpdate.set(true);
                });

        addToggleSetting(inventory, 10, Material.ENDER_PEARL, "Allow Friend jump",
                friendProfile.isAllowFriendJump(),
                () -> {
                    friendProfile.setAllowFriendJump(!friendProfile.isAllowFriendJump());
                    friendUpdate.set(true);
                });

        addPartyInviteSetting(inventory, 15, friendProfile, friendUpdate);

        addAutoNickSetting(inventory, 16, playerProfile, nickUpdate);

        inventory.setItem(
                new ItemBuilder(Material.LAVA_BUCKET).setName("§8» §6Statsreset").build(),
                4,
                event -> Lobby.getInstance().getStatsResetHandler().openStatsReset(player)
        );

        inventory.setOnClose(event -> {
            if (nickUpdate.get()) {
                BukkitCore.getAPI().getPlayerService().saveEntity(playerProfile, true, true);
            }
            if (friendUpdate.get()) {
                BukkitCore.getAPI().getFriendService().saveEntity(friendProfile, true, true);
            }
        });

        player.openInventory(inventory.getInventory());
    }

    private FriendProfile loadFriendProfile() {
        return BukkitCore.getAPI().getFriendService().getEntity(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getFriendService().getRepository().findFirstById(player.getUniqueId())
        );
    }

    private PlayerProfile loadPlayerProfile() {
        return BukkitCore.getAPI().getPlayerService().getEntity(
                player.getUniqueId(),
                () -> BukkitCore.getAPI().getPlayerService().getRepository().findFirstById(player.getUniqueId())
        );
    }

    private void addToggleSetting(Inventory inventory, int slot, Material material, String name,
                                  boolean currentState, Runnable onToggle) {
        ItemBuilder builder = new ItemBuilder(material).setName("§8» §6" + name).setAttributs();
        updateToggleLore(builder, currentState);

        inventory.setItem(builder.build(), slot, event -> {
            player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 2F, 2F);
            onToggle.run();
            updateToggleLore(builder, !currentState);
            inventory.setItem(builder.build(), slot);
        });
    }

    private void addPartyInviteSetting(Inventory inventory, int slot, FriendProfile friendProfile, AtomicBoolean updateFlag) {
        ItemBuilder builder = new ItemBuilder(Material.FIREWORK).setName("§8» §6Allow Party invites").setAttributs();
        updatePartyInviteLore(builder, friendProfile.getPartyInviteAllowance());

        inventory.setItem(builder.build(), slot, event -> {
            player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 2F, 2F);

            PartyInviteAllowance newAllowance = getNextPartyAllowance(friendProfile.getPartyInviteAllowance());
            friendProfile.setPartyInviteAllowance(newAllowance);

            updatePartyInviteLore(builder, newAllowance);
            inventory.setItem(builder.build(), slot);
            updateFlag.set(true);
        });
    }

    private PartyInviteAllowance getNextPartyAllowance(PartyInviteAllowance current) {
        return switch (current) {
            case EVERYONE -> PartyInviteAllowance.ONLY_FRIENDS;
            case ONLY_FRIENDS -> PartyInviteAllowance.NONE;
            case NONE -> PartyInviteAllowance.EVERYONE;
        };
    }

    private void addAutoNickSetting(Inventory inventory, int slot, PlayerProfile playerProfile, AtomicBoolean updateFlag) {
        ItemBuilder builder = new ItemBuilder(Material.NAME_TAG).setName("§8» §6Autonick").setAttributs();

        if (!player.hasPermission("markupapi.nick")) {
            builder.setLore("§cYou need atleast the §dVIP §crank!");
            inventory.setItem(builder.build(), slot, event -> {
                player.sendMessage(Lobby.getInstance().getPrefix() + "§cYou need at least the §dVIP §crank to nick yourself!");
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 10, 50);
                player.closeInventory();
            });
            return;
        }

        updateToggleLore(builder, playerProfile.isAutoNick());
        inventory.setItem(builder.build(), slot, event -> {
            player.playSound(player.getLocation(), Sound.CHICKEN_EGG_POP, 2F, 2F);
            playerProfile.setAutoNick(!playerProfile.isAutoNick());
            updateToggleLore(builder, playerProfile.isAutoNick());
            inventory.setItem(builder.build(), slot);
            updateFlag.set(true);
        });
    }

    private void updateToggleLore(ItemBuilder builder, boolean activated) {
        if (activated) {
            builder.setLore("§7currently §aactivated");
            builder.setEnchantments(Enchantment.KNOCKBACK, 1);
        } else {
            builder.setLore("§7currently §cdeactivated");
            removeKnockbackGlow(builder);
        }
    }

    private void updatePartyInviteLore(ItemBuilder builder, PartyInviteAllowance allowance) {
        List<String> lore = Arrays.stream(PartyInviteAllowance.values())
                .map(value -> (allowance == value ? "§a" : "§7") +
                        value.toString().replace("_", " ").toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        Collections.reverse(lore);
        builder.setLore(lore);

        if (allowance == PartyInviteAllowance.EVERYONE || allowance == PartyInviteAllowance.ONLY_FRIENDS) {
            builder.setEnchantments(Enchantment.KNOCKBACK, 1);
        } else {
            removeKnockbackGlow(builder);
        }
    }

    private void removeKnockbackGlow(ItemBuilder builder) {
        ItemMeta meta = builder.itemStack.getItemMeta();
        meta.removeEnchant(Enchantment.KNOCKBACK);
        builder.itemStack.setItemMeta(meta);
    }

    private void addInventoryBorder(Inventory inventory, int size) {
        ItemBuilder border = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) INVENTORY_BORDER_COLOR).setName("§8//");
        for (int i = 0; i < size; i++) {
            inventory.setItem(border.build(), i);
        }
    }

    public void setInventory() {
        player.getInventory().clear();

        ItemBuilder perk = BukkitCore.getInstance().getPerkManager().getPerk(player, PerkType.BLOCK);
        if (perk != null) {
            player.getInventory().setItem(4, perk.setAmount(MAX_ITEM_STACK).setName("§8» §6Blocks §8(§7rightclick§8)").build());
        }

        player.getInventory().setItem(0, new ItemBuilder(Material.COMPASS).setName("§8» §6Games §8(§7rightclick§8)").build());
        player.getInventory().setItem(7, new ItemBuilder(Material.REDSTONE_COMPARATOR).setName("§8» §6Settings §8(§7rightclick§8)").build());
        player.getInventory().setItem(1, new ItemBuilder(Material.NETHER_STAR).setName("§8» §6Lobby Switcher §8(§7rightclick§8)").build());
        player.getInventory().setItem(8,
                new ItemBuilder(Material.SKULL_ITEM, 1, (byte) 3)
                        .setSkullOwner(player.getName())
                        .setName("§8» §6Friends §8(§7rightclick§8)")
                        .build()
        );
    }
}