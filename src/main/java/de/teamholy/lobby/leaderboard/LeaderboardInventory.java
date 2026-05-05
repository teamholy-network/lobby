package de.teamholy.lobby.leaderboard;

import com.google.common.collect.Lists;
import de.teamholy.core.api.entities.game.GameProfile;
import de.teamholy.core.api.entities.game.StatsType;
import de.teamholy.core.api.entities.player.PlayerProfile;
import de.teamholy.core.api.entities.skin.SkinProfile;
import de.teamholy.core.api.utility.Gamemodes;
import de.teamholy.core.api.utility.PlayerRank;
import de.teamholy.core.api.utility.TrophieLeague;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.utils.Inventory;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import de.teamholy.lobby.Lobby;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.redisson.api.RScoredSortedSet;
import org.redisson.client.protocol.ScoredEntry;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * The LeaderboardInventory class manages the creation and access of leaderboards
 * in different game modes and statistical types. It provides an interactive user
 * interface for players to view and interact with the leaderboard while also
 * handling updating operations for leaderboard data in a cached manner.
 *
 * This class organizes players' statistics, ranks, and other details into a
 * visually pleasing inventory for viewing and interaction.
 *
 * Fields:
 * - CACHE_UPDATE_INTERVAL: Interval at which the leaderboard cache updates.
 * - TOP_ENTRIES_LIMIT: Maximum number of top entries displayed in the leaderboard.
 * - MIN_SCORE_THRESHOLD: Minimum score required for an entry to be considered.
 * - DEFAULT_COLOR_CODE: Default color code used for text formatting.
 * - topEntries: Cache for leaderboard entries for different game modes and stats types.
 * - kdFormat: Format string used for Kill/Death ratio calculation.
 *
 * Methods:
 * - initializeCacheUpdater: Initializes an asynchronous cache-updating task.
 * - updateLeaderboardCache: Updates the leaderboard cache with fresh data.
 * - fetchTopEntries: Retrieves the top leaderboard entries for a specific game mode and stats type.
 * - createTopEntry: Creates a leaderboard entry for a player.
 * - getRankColor: Determines the color associated with a rank name.
 * - open: Opens the leaderboard inventory for the specified player and game mode.
 * - createInventory: Initializes the inventory for the specified game mode.
 * - addInventoryDecoration: Adds decorative elements to the inventory.
 * - highlightSelectedGamemode: Highlights the selected game mode in the inventory.
 * - addPlayerStats: Adds the player's specific statistics to the inventory.
 * - addGamemodeButtons: Adds buttons for navigating between game modes.
 * - addInfoItem: Adds an informational item to the inventory.
 * - addTopEntries: Inserts leaderboard entries into the inventory.
 * - addStatsTypeEntries: Adds entries related to specific stats types and places them in the inventory.
 * - statsLore: Generates lore for leaderboard stats, including player details and stats.
 * - addRankToLore: Appends rank-related details to the lore.
 * - addTrophiesToLore: Appends trophy-related details to the lore.
 * - addDetailedStatsToLore: Appends comprehensive stats to the lore.
 * - addKDAndWinrateToLore: Appends Kill/Death ratio and win rate details to the lore.
 * - calculateKD: Calculates a player's Kill/Death ratio.
 * - calculateWinrate: Calculates a player's win rate.
 * - formatStatName: Formats the name of a stat for display.
 * - getWinrateColor: Determines the color associated with a win rate percentage.
 */
public class LeaderboardInventory {

    private static final int CACHE_UPDATE_INTERVAL = 20 * 60;
    private static final int TOP_ENTRIES_LIMIT = 4;
    private static final int MIN_SCORE_THRESHOLD = 1000;
    private static final String DEFAULT_COLOR_CODE = "§7";

    @Getter
    private final Map<Gamemodes, Map<StatsType, List<TopEntry>>> topEntries = new ConcurrentHashMap<>();
    private final DecimalFormat kdFormat = new DecimalFormat("0.00");

    public LeaderboardInventory() {
        initializeCacheUpdater();
    }

    private void initializeCacheUpdater() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Lobby.getInstance(), () -> {
            long startTime = System.currentTimeMillis();
            try {
                updateLeaderboardCache();
                long duration = System.currentTimeMillis() - startTime;
                Bukkit.getLogger().info("Leaderboard cache updated successfully in " + duration + "ms");
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.SEVERE, "Error updating leaderboard cache", e);
            }
        }, 0, CACHE_UPDATE_INTERVAL);
    }

    private void updateLeaderboardCache() {
        Map<Gamemodes, Map<StatsType, List<TopEntry>>> newTopEntries = new ConcurrentHashMap<>();

        for (Gamemodes gamemode : Gamemodes.values()) {
            Map<StatsType, List<TopEntry>> statsMap = new ConcurrentHashMap<>();

            for (StatsType statsType : StatsType.values()) {
                List<TopEntry> topEntryList = fetchTopEntries(gamemode, statsType);
                Collections.reverse(topEntryList);
                statsMap.put(statsType, topEntryList);
            }

            newTopEntries.put(gamemode, statsMap);
        }

        topEntries.clear();
        topEntries.putAll(newTopEntries);
    }

    private List<TopEntry> fetchTopEntries(Gamemodes gamemode, StatsType statsType) {
        List<TopEntry> topEntryList = Lists.newArrayList();
        String setKey = gamemode.toString() + "_" + statsType.toString();

        try {
            RScoredSortedSet<UUID> scoredSortedSet = BukkitCore.getAPI()
                    .getRedissonManager()
                    .getRedissonClient()
                    .getScoredSortedSet(setKey);

            scoredSortedSet.entryRangeReversed(0, TOP_ENTRIES_LIMIT).forEach(entry -> {
                try {
                    @SuppressWarnings("unchecked")
                    ScoredEntry<UUID> scoredEntry = entry;

                    if (scoredEntry.getScore() > MIN_SCORE_THRESHOLD) {
                        TopEntry topEntry = createTopEntry(scoredEntry.getValue());
                        if (topEntry != null) {
                            topEntryList.add(topEntry);
                        }
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().log(Level.WARNING, "Error processing leaderboard entry", e);
                }
            });
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Error fetching top entries for " + setKey, e);
        }

        return topEntryList;
    }

    private TopEntry createTopEntry(UUID playerId) {
        try {
            GameProfile gameProfile = BukkitCore.getAPI().getGameService().getEntity(
                    playerId,
                    () -> BukkitCore.getAPI().getGameService().getRepository().findFirstById(playerId)
            );

            SkinProfile skinProfile = BukkitCore.getAPI().getSkinService().getEntity(
                    playerId,
                    () -> BukkitCore.getAPI().getSkinService().getRepository().findFirstById(playerId)
            );

            PlayerProfile playerProfile = BukkitCore.getAPI().getPlayerService().getEntity(
                    playerId,
                    () -> BukkitCore.getAPI().getPlayerService().getRepository().findFirstById(playerId)
            );

            if (gameProfile == null || skinProfile == null || playerProfile == null) {
                return null;
            }

            TopEntry topEntry = new TopEntry();
            topEntry.setGameProfile(gameProfile);
            topEntry.setSkinProfile(skinProfile);
            topEntry.setNameWithColor(getRankColor(playerProfile.getRank()) + playerProfile.getPlayerName());

            return topEntry;
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Error creating top entry for player " + playerId, e);
            return null;
        }
    }

    private String getRankColor(String rankName) {
        try {
            return PlayerRank.valueOf(rankName).getColorCode();
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Unknown rank: " + rankName + " - using default color");
            return DEFAULT_COLOR_CODE;
        }
    }

    public void open(Player player, Gamemodes gamemode) {
        if (player == null || gamemode == null) {
            return;
        }

        Inventory inventory = createInventory(gamemode);
        GameProfile gameProfile = Lobby.getInstance()
                .getLobbyPlayerEntryHandler()
                .get(player.getUniqueId())
                .getGameProfile();

        player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);

        addInventoryDecoration(inventory);
        highlightSelectedGamemode(inventory, gamemode);

        addPlayerStats(inventory, player, gameProfile, gamemode);

        addGamemodeButtons(inventory, player);

        addInfoItem(inventory);

        addTopEntries(inventory, gamemode);

        player.openInventory(inventory.getInventory());
    }

    private Inventory createInventory(Gamemodes gamemode) {
        String title = "§8» §6Leaderboard in §" + gamemode.getColor() + gamemode.toString().toLowerCase();
        return new Inventory(title, 54);
    }

    private void addInventoryDecoration(Inventory inventory) {
        ItemBuilder glassPane = new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte) 15).setName("§8//");

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                if (row == 1 || col == 0 || col == 8 || row == 5) {
                    inventory.setItem(glassPane.build(), row * 9 + col);
                }
            }
        }
    }

    private void highlightSelectedGamemode(Inventory inventory, Gamemodes gamemode) {
        int colorSlot = switch (gamemode) {
            case SKYWARSFFA -> 16;
            case KNOCKBACKFFA -> 17;
            case BEDWARS -> 11;
            case RUSHBW -> 13;
            case SGFFA -> 15;
            case MLGRUSH -> 12;
            default -> 11;
        };

        if (inventory.getInventory().getItem(colorSlot) != null) {
            inventory.getInventory().getItem(colorSlot).setDurability((short) 0);
        }
    }

    private void addPlayerStats(Inventory inventory, Player player, GameProfile gameProfile, Gamemodes gamemode) {
        String playerName = player.getName();
        String gamemodeColor = gamemode.getColor();
        String gamemodeName = gamemode.toString().toLowerCase();

        inventory.setItem(
                new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                        .setSkullOwner(playerName)
                        .setLore(statsLore(gameProfile, gamemode, StatsType.ALLTIME))
                        .setName("§7Your §c§lALLTIME §" + gamemodeColor + gamemodeName + " §7stats")
                        .build(),
                26
        );

        inventory.setItem(
                new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                        .setSkullOwner(playerName)
                        .setLore(statsLore(gameProfile, gamemode, StatsType.MONTHLY))
                        .setName("§7Your §e§lMONTHLY §" + gamemodeColor + gamemodeName + " §7stats")
                        .build(),
                35
        );

        inventory.setItem(
                new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                        .setSkullOwner(playerName)
                        .setLore(statsLore(gameProfile, gamemode, StatsType.DAILY))
                        .setName("§7Your §a§lDAILY §" + gamemodeColor + gamemodeName + " §7stats")
                        .build(),
                44
        );
    }

    private void addGamemodeButtons(Inventory inventory, Player player) {
        String clickToOpen = "§7Click to show leaderboard";

        inventory.setItem(
                new ItemBuilder(Material.GRASS).setLore(clickToOpen).setName("§8» §6SkywarsFFA").build(),
                6,
                event -> open(player, Gamemodes.SKYWARSFFA)
        );

        inventory.setItem(
                new ItemBuilder(Material.STICK).setLore(clickToOpen).setName("§8» §6MLGRush").build(),
                2,
                event -> open(player, Gamemodes.MLGRUSH)
        );

        inventory.setItem(
                new ItemBuilder(Material.SANDSTONE).setLore(clickToOpen).setName("§8» §6KnockbackFFA").build(),
                5,
                event -> open(player, Gamemodes.KNOCKBACKFFA)
        );

        inventory.setItem(
                new ItemBuilder(Material.BED).setLore(clickToOpen).setName("§8» §6Bedwars").build(),
                1,
                event -> open(player, Gamemodes.BEDWARS)
        );

        inventory.setItem(
                new ItemBuilder(Material.BLAZE_ROD).setLore(clickToOpen).setName("§8» §6Rush-Bedwars").build(),
                3,
                event -> open(player, Gamemodes.RUSHBW)
        );

        inventory.setItem(
                new ItemBuilder(Material.IRON_SWORD).setLore(clickToOpen).setName("§8» §6SGFFA").build(),
                7,
                event -> open(player, Gamemodes.SGFFA)
        );
    }

    private void addInfoItem(Inventory inventory) {
        inventory.setItem(
                new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                        .setSkullMeta("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM0ZTQ0MWVhYzg4NG" +
                                "RlMzM0N2E4Nzc1YTA3YTY2YmJjNGM4MmEyNGVkMmQwY2ZlYjFhY2FmNmNlOTlkNTNiNiJ9fX0=", "")
                        .setName("§6Infos")
                        .setLore(
                                " ",
                                " §f§lSTATSRESET",
                                " §7The §adaily §7stats will be reset at 00:00 CET",
                                " §7The §cmonthly §7stats will be reset at the first of the month",
                                " ",
                                " §3§lCHAMPION §f§lRANK",
                                " §7The §a#1 §7from every mode §adaily",
                                " §7will get the §3Champion§7 rank for §c24 hours§7!",
                                " "
                        )
                        .build(),
                49
        );
    }

    private void addTopEntries(Inventory inventory, Gamemodes gamemode) {
        inventory.setItem(
                new ItemBuilder(Material.STAINED_GLASS_PANE, 1, 5).setName("§a§lDAILY §f§lTOP 5 §8»").build(),
                36
        );
        inventory.setItem(
                new ItemBuilder(Material.STAINED_GLASS_PANE, 1, 4).setName("§e§lMONTHLY §f§lTOP 5 §8»").build(),
                27
        );
        inventory.setItem(
                new ItemBuilder(Material.STAINED_GLASS_PANE, 1, 14).setName("§c§lALLTIME §f§lTOP 5 §8»").build(),
                18
        );

        addStatsTypeEntries(inventory, gamemode, StatsType.DAILY, 38);

        addStatsTypeEntries(inventory, gamemode, StatsType.MONTHLY, 29);

        addStatsTypeEntries(inventory, gamemode, StatsType.ALLTIME, 20);
    }

    private void addStatsTypeEntries(Inventory inventory, Gamemodes gamemode, StatsType statsType, int startSlot) {
        Map<StatsType, List<TopEntry>> gamemodeEntries = topEntries.get(gamemode);
        if (gamemodeEntries == null) {
            return;
        }

        List<TopEntry> entries = gamemodeEntries.get(statsType);
        if (entries == null) {
            return;
        }

        int slot = startSlot;
        for (TopEntry topEntry : entries) {
            if (topEntry.getSkinProfile() != null && topEntry.getGameProfile() != null) {
                inventory.setItem(
                        new ItemBuilder(Material.SKULL_ITEM, 1, 3)
                                .setSkullMeta(topEntry.getSkinProfile().getValue(), "")
                                .setLore(statsLore(topEntry.getGameProfile(), gamemode, statsType))
                                .setName(topEntry.getNameWithColor())
                                .build(),
                        slot
                );
            }
            slot++;
        }
    }

    private List<String> statsLore(GameProfile gameProfile, Gamemodes gamemode, StatsType statsType) {
        if (gameProfile == null || !gameProfile.exists(gamemode.toString())) {
            return List.of("§cNo stats found!");
        }

        List<String> lore = new ArrayList<>();

        addRankToLore(lore, gameProfile, gamemode, statsType);

        addTrophiesToLore(lore, gameProfile, gamemode, statsType);

        lore.add(" ");

        addDetailedStatsToLore(lore, gameProfile, gamemode, statsType);

        addKDAndWinrateToLore(lore, gameProfile, gamemode, statsType);

        lore.add(" ");

        return lore;
    }

    private void addRankToLore(List<String> lore, GameProfile gameProfile, Gamemodes gamemode, StatsType statsType) {
        int rank = BukkitCore.getAPI().getRankingManager().getRankFromUUID(gamemode, statsType, gameProfile.getPlayerId());

        if (rank == -1) {
            lore.add(" §cYou don't have a rank yet!");
        } else {
            lore.add(" §7Rank §" + gamemode.getColor() + "§l#" + rank + " " + statsType.toBeauty().toLowerCase());
        }
    }

    private void addTrophiesToLore(List<String> lore, GameProfile gameProfile, Gamemodes gamemode, StatsType statsType) {
        int trophies = (int) gameProfile.getStat(gamemode.toString(), statsType, "trophies");
        String league = TrophieLeague.getEloRank(trophies).getName();
        lore.add(" §7Trophies§8: §" + gamemode.getColor() + trophies + " §8(" + league + "§8)");
    }

    private void addDetailedStatsToLore(List<String> lore, GameProfile gameProfile, Gamemodes gamemode, StatsType statsType) {
        gamemode.getStatKeys().forEach(statKey -> {
            if (!"trophies".equalsIgnoreCase(statKey.getName())) {
                String statName = formatStatName(statKey.getName());
                Object statValue = gameProfile.getStat(gamemode.toString(), statsType, statKey.getName());
                lore.add(" §7" + statName + "§8: §" + gamemode.getColor() + statValue);
            }
        });
    }

    private void addKDAndWinrateToLore(List<String> lore, GameProfile gameProfile, Gamemodes gamemode, StatsType statsType) {
        int kills = (int) gameProfile.getStat(gamemode.toString(), statsType, "kills");
        int deaths = (int) gameProfile.getStat(gamemode.toString(), statsType, "deaths");
        String kd = calculateKD(kills, deaths);

        StringBuilder line = new StringBuilder(" §7K§8/§7D§8: §" + gamemode.getColor() + kd);

        if (gamemode.getStatKeys().stream().anyMatch(statKey -> "played_games".equalsIgnoreCase(statKey.getName()))) {
            int games = (int) gameProfile.getStat(gamemode.toString(), statsType, "played_games");
            int wins = (int) gameProfile.getStat(gamemode.toString(), statsType, "won_games");
            String winrate = calculateWinrate(games, wins);
            line.append(" §8︳ §7Winrate§8: ").append(winrate);
        }

        lore.add(line.toString());
    }

    private String calculateKD(int kills, int deaths) {
        if (kills == 0 || deaths == 0) {
            return "§c-/-";
        }
        double kd = (double) kills / deaths;
        return kdFormat.format(kd);
    }

    private String calculateWinrate(int games, int wins) {
        if (games == 0 && wins == 0) {
            return "§c-/-";
        }

        double percent = (100.0 / games) * wins;
        int winrate = (int) Math.round(percent);
        return getWinrateColor(winrate).toString() + winrate + "%";
    }

    private String formatStatName(String statName) {
        return switch (statName.toLowerCase()) {
            case "kills" -> "Kills";
            case "deaths" -> "Deaths";
            case "played_games" -> "Played games";
            case "won_games" -> "Won games";
            case "destroyed_beds" -> "Destroyed beds";
            default -> statName;
        };
    }

    private ChatColor getWinrateColor(int winrate) {
        if (winrate >= 90) return ChatColor.DARK_GREEN;
        if (winrate >= 70) return ChatColor.GREEN;
        if (winrate >= 50) return ChatColor.YELLOW;
        if (winrate >= 30) return ChatColor.GOLD;
        if (winrate >= 10) return ChatColor.RED;
        return ChatColor.DARK_RED;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TopEntry {
        private GameProfile gameProfile;
        private SkinProfile skinProfile;
        private String nameWithColor;
    }
}