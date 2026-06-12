package de.teamholy.lobby.listeners;

import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.EnumSet;
import java.util.Set;

/* copyright by Yassino */
public class BlockListener implements Listener {

    private static final Set<Material> PRESSURE_PLATES = EnumSet.of(
            Material.STONE_PLATE, Material.GOLD_PLATE, Material.IRON_PLATE, Material.WOOD_PLATE);

    private static final Set<Material> BLOCKED_INTERACT_BLOCKS = EnumSet.of(
            Material.DRAGON_EGG, Material.CHEST, Material.FURNACE, Material.BURNING_FURNACE,
            Material.TRAP_DOOR, Material.ENDER_CHEST, Material.WORKBENCH, Material.WOODEN_DOOR,
            Material.WOOD_DOOR, Material.STONE_BUTTON, Material.WOOD_BUTTON, Material.LEVER);

    @EventHandler
    public void onBlockPhysic(BlockPhysicsEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE)
            event.setCancelled(true);
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent e) {

        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM)
            return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLobbyPVP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        LobbyPlayer lobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(event.getEntity().getUniqueId());
        LobbyPlayer attackerLobbyPlayer = Lobby.getInstance().getLobbyPlayerEntryHandler().get(event.getDamager().getUniqueId());

        if (lobbyPlayer.isInArena() && attackerLobbyPlayer.isInArena()) {
            event.setCancelled(false);
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        // non-player damagers (arrows etc.) never reached the checks below — keep it that way
        if (!(event.getDamager() instanceof Player player)) return;

        if (event.getEntity() instanceof Player && player.getItemInHand().getType() == Material.SKULL_ITEM) {
            Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId()).executeBungeeCommand("friend add " + event.getEntity().getUniqueId());
        }
        if (event.getEntityType() == EntityType.ARMOR_STAND) {
            event.setCancelled(true);
        }
        if (event.getEntityType() == EntityType.ITEM_FRAME) {
            if (player.getGameMode() == GameMode.CREATIVE)
                return;
            event.setCancelled(true);
        }
    }


    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        e.setFoodLevel(20);
        e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onPressurePlate(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && PRESSURE_PLATES.contains(event.getClickedBlock().getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract1(PlayerInteractEvent event) {
        try {
            if (event.getClickedBlock().getType() == Material.ITEM_FRAME) event.setCancelled(true);
        } catch (Exception ignored) {
        }
    }

    @EventHandler
    public void onHaning(HangingBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.SOIL)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && BLOCKED_INTERACT_BLOCKS.contains(e.getClickedBlock().getType())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractFRame(PlayerInteractEntityEvent event) {
        if (event.getPlayer() == null) return;
        if (event.getRightClicked() == null) return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (event.getRightClicked().getType() == EntityType.ITEM_FRAME) event.setCancelled(true);
    }

    @EventHandler
    public void ach(PlayerAchievementAwardedEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e) {
        e.setCancelled(true);
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);
        }
    }

}
