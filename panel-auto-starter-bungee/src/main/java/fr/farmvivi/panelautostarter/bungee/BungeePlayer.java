package fr.farmvivi.panelautostarter.bungee;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.chat.ComponentSerializer;
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
    public Object getPlatformHandle() {
        return player;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(new TextComponent(message));
    }

    @Override
    public void sendMessage(Component message) {
        player.sendMessage(toRichBungee(message));
    }

    /**
     * Convertit un composant en préservant clics et survols.
     * <p>
     * La sérialisation historique en codes {@code §} les perd tous les deux :
     * elle ne transporte que des couleurs. Les menus en seraient inertes sur
     * BungeeCord — un texte joliment coloré sur lequel il ne se passe rien.
     * <p>
     * Le passage par JSON est le seul pont disponible : BungeeCord sait relire
     * le format de chat du jeu, qu'Adventure sait écrire. En cas d'échec —
     * format que le proxy ne reconnaîtrait pas — on retombe sur les codes
     * {@code §} : un message appauvri vaut mieux qu'un message perdu.
     *
     * @param message le composant
     * @return son équivalent BungeeCord
     */
    public static BaseComponent[] toRichBungee(Component message) {
        try {
            return ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(message));
        } catch (RuntimeException ex) {
            return new BaseComponent[]{
                    new TextComponent(LegacyComponentSerializer.legacySection().serialize(message))};
        }
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

    /**
     * Donne accès au joueur BungeeCord sous-jacent, pour les API du proxy qui
     * raisonnent en {@code CommandSender} — le contrôle d'accès aux serveurs
     * notamment.
     *
     * @return le joueur BungeeCord
     */
    public ProxiedPlayer getProxiedPlayer() {
        return player;
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
