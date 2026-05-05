package de.teamholy.lobby.listeners;

import de.teamholy.lobby.Lobby;
import de.teamholy.lobby.lobbyplayer.LobbyPlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

/* copyright by Yassino */
public class BlockListener implements Listener {

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
    public void onHit(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Player player = (Player) entityDamageByEntityEvent.getDamager();
        if (entityDamageByEntityEvent.getDamager() == null || entityDamageByEntityEvent.getEntity() == null) {
            return;
        }
        if (entityDamageByEntityEvent.getDamager() instanceof Player && entityDamageByEntityEvent.getEntity() instanceof Player) {
            if (player.getItemInHand().getType() == Material.SKULL_ITEM) {
                Lobby.getInstance().getLobbyPlayerEntryHandler().get(player.getUniqueId()).executeBungeeCommand("friend add " + entityDamageByEntityEvent.getEntity().getUniqueId());
            }
        }
        if (entityDamageByEntityEvent.getEntityType() == EntityType.ARMOR_STAND) {
            entityDamageByEntityEvent.setCancelled(true);
        }
        if (entityDamageByEntityEvent.getEntityType() == EntityType.ITEM_FRAME) {
            if (player.getGameMode() == GameMode.CREATIVE)
                return;
            entityDamageByEntityEvent.setCancelled(true);
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
    public void handlepenmoisAsdffefe(PlayerInteractEvent ev) {
        if (ev.getAction().equals(Action.PHYSICAL)) {
            if (ev.getClickedBlock().getType() == Material.STONE_PLATE) {
                ev.setCancelled(true);
            } else if (ev.getClickedBlock().getType() == Material.GOLD_PLATE) {
                ev.setCancelled(true);
            } else if (ev.getClickedBlock().getType() == Material.IRON_PLATE) {
                ev.setCancelled(true);
            } else if (ev.getClickedBlock().getType() == Material.WOOD_PLATE) {
                ev.setCancelled(true);
            }
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
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if (block.getType() == Material.DRAGON_EGG) e.setCancelled(true);
            if (block.getType() == Material.CHEST) e.setCancelled(true);
            if (block.getType() == Material.FURNACE) e.setCancelled(true);
            if (block.getType() == Material.BURNING_FURNACE) e.setCancelled(true);
            if (block.getType() == Material.TRAP_DOOR) e.setCancelled(true);
            if (block.getType() == Material.ENDER_CHEST) e.setCancelled(true);
            if (block.getType() == Material.WORKBENCH) e.setCancelled(true);
            if (block.getType() == Material.WOODEN_DOOR) e.setCancelled(true);
            if (block.getType() == Material.WOOD_DOOR) e.setCancelled(true);
            if (block.getType() == Material.STONE_BUTTON) e.setCancelled(true);
            if (block.getType() == Material.WOOD_BUTTON) e.setCancelled(true);
            if (block.getType() == Material.LEVER) e.setCancelled(true);
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
