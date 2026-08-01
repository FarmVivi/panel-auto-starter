package fr.farmvivi.panelautostarter.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import java.time.Duration;
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
    public Object getPlatformHandle() {
        return player;
    }

    @Override
    public java.util.Locale getLocale() {
        return player.getEffectiveLocale();
    }

    /**
     * Traduit avant d'envoyer.
     * <p>
     * Velocity sait le faire lui-même, mais le faire ici rend le comportement
     * identique sur les deux proxys — BungeeCord, lui, ignore Adventure — et
     * ne dépend pas de la façon dont Velocity choisit la langue. Sur un
     * composant déjà traduit, l'opération est sans effet.
     */
    private Component localized(Component message) {
        return fr.farmvivi.panelautostarter.i18n.Translations.render(message, getLocale());
    }

    /**
     * Velocity conserve les propriétés du profil qu'il a authentifié : la
     * texture est déjà là, signature comprise.
     */
    @Override
    public fr.farmvivi.panelautostarter.common.PlayerSkin getSkin() {
        for (com.velocitypowered.api.util.GameProfile.Property property
                : player.getGameProfileProperties()) {
            if ("textures".equals(property.getName())) {
                return new fr.farmvivi.panelautostarter.common.PlayerSkin(player.getUsername(),
                        player.getUniqueId(), property.getValue(), property.getSignature());
            }
        }
        return null;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(Component.text(message));
    }

    @Override
    public void sendMessage(Component message) {
        player.sendMessage(localized(message));
    }

    @Override
    public void sendActionBar(Component message) {
        player.sendActionBar(localized(message));
    }

    @Override
    public void showTitle(Component title, Component subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        player.showTitle(Title.title(localized(title), localized(subtitle),
                Title.Times.times(fadeIn, stay, fadeOut)));
    }

    @Override
    public void playSound(String soundKey, float volume, float pitch) {
        Key key;
        try {
            key = Key.key(soundKey);
        } catch (InvalidKeyException ex) {
            // Un identifiant de son invalide vient de la configuration : on
            // ignore plutot que de casser la teleportation en cours.
            return;
        }
        // L'emetteur est indispensable : playSound(Sound) seul est une methode
        // par defaut d'Audience que Velocity n'implemente pas, le contrat
        // d'Adventure exigeant de jouer le son a la position du joueur, que le
        // proxy ignore. Avec Emitter.self(), le son est rattache a l'entite du
        // joueur et le proxy sait l'emettre. Cote client, cela requiert 1.19.3
        // ou plus recent ; en deca, le son est silencieusement ignore.
        player.playSound(Sound.sound(key, Sound.Source.MASTER, volume, pitch), Sound.Emitter.self());
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
