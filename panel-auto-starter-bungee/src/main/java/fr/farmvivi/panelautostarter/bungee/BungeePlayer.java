package fr.farmvivi.panelautostarter.bungee;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeePlayer implements CommonPlayer {
    private final CommonProxy proxy;
    private final ProxiedPlayer player;

    public BungeePlayer(CommonProxy proxy, ProxiedPlayer player) {
        this.proxy = proxy;
        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(new TextComponent(message));
    }

    @Override
    public void sendMessage(Component message) {
        // Convert the Component to legacy string format (§-codes)
        String legacyMessage = LegacyComponentSerializer.legacySection().serialize(message);
        player.sendMessage(new TextComponent(legacyMessage));
    }

    @Override
    public void connectToServer(CommonServer server) {
        if (server instanceof BungeeServer bungeeServer) {
            player.connect(bungeeServer.getServerInfo());
        }
    }

    @Override
    public CommonServer getServer() {
        // Retrieve the server the player is connected to
        ServerInfo server = player.getServer().getInfo();

        // If the server is not null
        if (server != null) {
            // Return the server
            return proxy.getServer(server.getName());
        }

        // Return null
        return null;
    }

    @Override
    public boolean isOnline() {
        return player.isConnected();
    }

    @Override
    public String getUsername() {
        return player.getName();
    }

    @Override
    public String getDisplayName() {
        return player.getDisplayName();
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }
}
