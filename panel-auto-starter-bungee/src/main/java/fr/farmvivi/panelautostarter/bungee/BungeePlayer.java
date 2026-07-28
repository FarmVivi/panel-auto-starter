package fr.farmvivi.panelautostarter.bungee;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.time.Duration;
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

    /**
     * Contrairement au son, la barre d'action existe bien en natif sur
     * BungeeCord : c'est une variante du paquet de chat, que le proxy sait
     * fabriquer.
     */
    @Override
    public void sendActionBar(Component message) {
        player.sendMessage(ChatMessageType.ACTION_BAR, toBungee(message));
    }

    @Override
    public void showTitle(Component title, Component subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        // BungeeCord raisonne en ticks de 20 par seconde, la ou Adventure
        // raisonne en durees.
        ProxyServer.getInstance().createTitle()
                .title(toBungee(title))
                .subTitle(toBungee(subtitle))
                .fadeIn(toTicks(fadeIn))
                .stay(toTicks(stay))
                .fadeOut(toTicks(fadeOut))
                .send(player);
    }

    /**
     * Sans objet : BungeeCord n'expose aucune API de son, le proxy achemine les
     * paquets sans en fabriquer. Voir {@code CommonProxy.supportsSound()}, qui
     * permet d'en avertir l'administrateur au démarrage.
     */
    @Override
    public void playSound(String soundKey, float volume, float pitch) {
        // Volontairement vide.
    }

    private static BaseComponent toBungee(Component component) {
        return new TextComponent(LegacyComponentSerializer.legacySection().serialize(component));
    }

    private static int toTicks(Duration duration) {
        return (int) Math.max(0, duration.toMillis() / 50);
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
