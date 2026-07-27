package fr.farmvivi.panelautostarter.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

public class VelocityPlayer implements CommonPlayer {
    private final CommonProxy proxy;
    private final Player player;

    public VelocityPlayer(CommonProxy proxy, Player player) {
        this.proxy = proxy;
        this.player = player;
    }

    @Override
    public boolean isOnline() {
        return player.isActive();
    }

    @Override
    public String getUsername() {
        return player.getUsername();
    }

    @Override
    public String getDisplayName() {
        return this.getUsername();
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(Component.text(message));
    }

    @Override
    public void sendMessage(Component message) {
        player.sendMessage(message);
    }

    @Override
    public void connectToServer(CommonServer server) {
        if (server instanceof VelocityServer velocityServer) {
            player.createConnectionRequest(velocityServer.getServer()).fireAndForget();
        }
    }

    @Override
    public CommonServer getServer() {
        // Retrieve the server the player is connected to
        Optional<ServerConnection> serverConnection = player.getCurrentServer();

        // Return the server the player is connected to if it exists or null
        return serverConnection.map(connection -> proxy.getServer(connection.getServerInfo().getName())).orElse(null);
    }
}
