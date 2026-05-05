package de.teamholy.lobby.bedwars;

import de.teamholy.core.bukkit.utils.ItemBuilder;
import lombok.Getter;
import lombok.Setter;

/* copyright by Yassino */
@Getter @Setter
public class SpectateGame {

    private ItemBuilder itemBuilder;
    private long startedSince;

    public SpectateGame(ItemBuilder itemBuilder, long startedSince) {
        this.itemBuilder = itemBuilder;
        this.startedSince = startedSince;
    }

}
