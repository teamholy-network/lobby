package de.teamholy.lobby.lobbyplayer;

import de.teamholy.core.api.utility.PlayerRank;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/* copyright by Yassino */
@Getter
@Setter
public class Friend {

    private String value, signature;
    private UUID uuid;

    private boolean isOnline;
    private long lastJoin;

    private String name;
    private PlayerRank playerRank;
    private String currentServer;


}
