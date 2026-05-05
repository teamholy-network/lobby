package de.teamholy.lobby.handlers;

import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.ext.bridge.BridgeServiceProperty;
import lombok.Getter;

import java.util.HashMap;

/* copyright by Yassino */
@Getter
public class CloudCacheHandler {

    private final HashMap<String, ServiceInfoSnapshot> serverInfos = new HashMap<>();

    public CloudCacheHandler() {
        CloudNetDriver.getInstance().getCloudServiceProvider().getStartedCloudServices().forEach(serviceInfoSnapshot -> serverInfos.put(serviceInfoSnapshot.getName(),serviceInfoSnapshot));
    }

    public void updateServerInfo(ServiceInfoSnapshot serverInfo) {
        serverInfos.put(serverInfo.getName(),serverInfo);
    }

    public void removeServerInfo(ServiceInfoSnapshot serverInfo) {
        serverInfos.remove(serverInfo.getName());
    }

    public int getOnlineCount(String group) {
        return getServerInfos().values().stream().filter(info -> info.getConfiguration().getGroups()[0].equalsIgnoreCase(group)).mapToInt(info ->
                info.getProperty(BridgeServiceProperty.ONLINE_COUNT).isPresent() ? info.getProperty(BridgeServiceProperty.ONLINE_COUNT).get() : 0
        ).sum();
    }

}
