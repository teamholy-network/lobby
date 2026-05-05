package de.teamholy.lobby.commands;

import de.teamholy.core.bukkit.BukkitCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Copyright (c) charon, All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by charon
 **/
public class SpawnCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            return true;
        }
        player.teleport(BukkitCore.getInstance().getLocationManager().getLocation("de/teamholy/lobby"));
        return false;
    }
}
