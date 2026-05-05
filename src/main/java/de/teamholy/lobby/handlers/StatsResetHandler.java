package de.teamholy.lobby.handlers;

import de.teamholy.core.api.entities.game.GameProfile;
import de.teamholy.core.api.entities.player.PlayerProfile;
import de.teamholy.core.api.utility.Gamemodes;
import de.teamholy.core.bukkit.BukkitCore;
import de.teamholy.core.bukkit.utils.Inventory;
import de.teamholy.core.bukkit.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class StatsResetHandler {

    private static final int INVENTORY_SIZE = 27;
    private static final int CONFIRM_INVENTORY_SIZE = 9;
    private static final byte GLASS_PANE_COLOR = 15;
    private static final String INVENTORY_TITLE = "§8» §6Statsreset";
    private static final String CONFIRM_TITLE_PREFIX = "§8» §6Statsreset in ";
    private static final String NO_TOKENS_MESSAGE = "§cNo statsreset tokens";
    private static final String BUY_TOKENS_MESSAGE = "§7Do you want to buy statsreset tokens? §ashop.teamholy.de";
    private static final String STATS_RESET_MESSAGE = "§6You currently have §a%s §6statsreset tokens! §7Click on the mode where you want to delete your stats";
    private static final String RESET_CONFIRM_MESSAGE = "§7Do you want to reset your stats in %s%s§7?";
    private static final String STATS_DELETED_MESSAGE = "§aYou deleted your stats in §%s%s§a!";
    private static final String NO_STATS_MESSAGE = "§cYou don't have any stats in §%s%s";
    private static final String ABORTED_RESET_MESSAGE = "§cAborted stats reset!";

    public void openStatsReset(Player player) {
        Inventory inventory = createInventory(INVENTORY_TITLE, INVENTORY_SIZE);

        PlayerProfile playerProfile = BukkitCore.getAPI().getPlayerService().getRedisCache().get(player.getUniqueId());
        if (playerProfile == null) return;

        long tokens = playerProfile.getStatsResetTokens();
        if (tokens == 0) {
            inventory.setItem(createItem(Material.BARRIER, NO_TOKENS_MESSAGE, BUY_TOKENS_MESSAGE), 13);
        } else {
            inventory.setItem(createItem(Material.PAPER, String.format(STATS_RESET_MESSAGE, tokens)), 4);
            addGamemodeItems(inventory, player, playerProfile);
        }

        player.openInventory(inventory.getInventory());
    }

    private void addGamemodeItems(Inventory inventory, Player player, PlayerProfile playerProfile) {
        addItem(inventory, player, playerProfile, Material.STICK, "§8» §6MLGRush", 10, Gamemodes.MLGRUSH);
        addItem(inventory, player, playerProfile, Material.SANDSTONE, "§8» §6KnockbackFFA", 11, Gamemodes.KNOCKBACKFFA);
        addItem(inventory, player, playerProfile, Material.BED, "§8» §6Bedwars", 12, Gamemodes.BEDWARS);
        addItem(inventory, player, playerProfile, Material.GRASS, "§8» §6SkywarsFFA", 13, Gamemodes.SKYWARSFFA);
        addItem(inventory, player, playerProfile, Material.IRON_SWORD, "§8» §6SGFFA", 14, Gamemodes.SGFFA);
        addItem(inventory, player, playerProfile, Material.BLAZE_ROD, "§8» §6Rush-Bedwars", 15, Gamemodes.RUSHBW);
        addItem(inventory, player, playerProfile, Material.DIAMOND_PICKAXE, "§8» §6Bridge", 16, Gamemodes.BRIDGE);
    }

    private void addItem(Inventory inventory, Player player, PlayerProfile playerProfile, Material material, String name, int slot, Gamemodes gamemode) {
        ItemBuilder itemBuilder = new ItemBuilder(material).setName(name);
        inventory.setItem(itemBuilder.build(), slot, clickEvent -> openStatsResetConfirm(player, playerProfile, itemBuilder, gamemode));
    }

    private void openStatsResetConfirm(Player player, PlayerProfile playerProfile, ItemBuilder itemBuilder, Gamemodes gamemode) {
        Inventory inventory = createInventory(CONFIRM_TITLE_PREFIX + gamemode.getColor() + gamemode.toString().toLowerCase(Locale.ROOT), CONFIRM_INVENTORY_SIZE);

        inventory.setItem(itemBuilder.setLore(String.format(RESET_CONFIRM_MESSAGE, gamemode.getColor(), gamemode.toString().toLowerCase(Locale.ROOT))).build(), 4);

        GameProfile gameProfile = BukkitCore.getAPI().getGameService().getEntity(player.getUniqueId(), () -> BukkitCore.getAPI().getGameService().getRepository().findFirstById(player.getUniqueId()));

        inventory.setItem(createItem(Material.INK_SACK, (byte) 10, "§8» §aYes"), 2, event -> {
            if (gameProfile.exists(gamemode.toString())) {
                gameProfile.delete(gamemode.toString());
                player.playSound(player.getLocation(), Sound.ANVIL_BREAK, 50f, 50f);
                player.sendMessage(String.format(STATS_DELETED_MESSAGE, gamemode.getColor(), gamemode.toString().toLowerCase(Locale.ROOT)));
                BukkitCore.getAPI().getGameService().saveEntity(gameProfile, true, true);
                playerProfile.setStatsResetTokens(playerProfile.getStatsResetTokens() - 1);
                BukkitCore.getAPI().getPlayerService().saveEntity(playerProfile, true, true);
            } else {
                player.sendMessage(String.format(NO_STATS_MESSAGE, gamemode.getColor(), gamemode.toString().toLowerCase(Locale.ROOT)));
            }
            player.closeInventory();
        });

        inventory.setItem(createItem(Material.INK_SACK, (byte) 1, "§8» §cNo"), 6, event -> {
            player.closeInventory();
            player.sendMessage(ABORTED_RESET_MESSAGE);
        });

        player.openInventory(inventory.getInventory());
    }

    private Inventory createInventory(String title, int size) {
        Inventory inventory = new Inventory(title, size);
        for (int i = 0; i < size; i++) {
            inventory.setItem(createItem(Material.STAINED_GLASS_PANE, GLASS_PANE_COLOR, "§8//"), i);
        }
        return inventory;
    }

    private ItemStack createItem(Material material, byte data, String name) {
        return new ItemBuilder(material, 1, data).setName(name).build();
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        return new ItemBuilder(material).setName(name).setLore(lore).build();
    }
}