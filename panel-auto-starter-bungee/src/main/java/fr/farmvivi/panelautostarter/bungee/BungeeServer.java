package fr.farmvivi.panelautostarter.bungee;

import fr.farmvivi.panelautostarter.bungee.ping.BungeeServerPing;
import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import net.md_5.bungee.api.config.ServerInfo;

public class BungeeServer implements CommonServer {
    private final ServerInfo serverInfo;

    public BungeeServer(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public String getName() {
        return serverInfo.getName();
    }

    @Override
    public String getDisplayName() {
        return serverInfo.getMotd();
    }

    @Override
    public int getPlayerCount() {
        return serverInfo.getPlayers().size();
    }

    @Override
    public void ping(Callback<CommonServerPing> callback) {
        serverInfo.ping((result, error) -> {
            if (result == null) {
                callback.done(null);
                return;
            }
            callback.done(new BungeeServerPing(result));
        });
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}
