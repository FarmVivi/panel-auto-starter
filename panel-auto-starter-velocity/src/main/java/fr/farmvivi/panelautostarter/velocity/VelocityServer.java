package fr.farmvivi.panelautostarter.velocity;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.velocity.ping.VelocityServerPing;

public class VelocityServer implements CommonServer {
    private final RegisteredServer server;

    public VelocityServer(RegisteredServer server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return server.getServerInfo().getName();
    }

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    @Override
    public int getPlayerCount() {
        return server.getPlayersConnected().size();
    }

    @Override
    public void ping(Callback<CommonServerPing> callback) {
        server.ping().whenComplete((result, error) -> {
            if (result == null) {
                callback.done(null);
                return;
            }
            callback.done(new VelocityServerPing(result));
        });
    }

    public RegisteredServer getServer() {
        return server;
    }
}
