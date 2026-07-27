package fr.farmvivi.panelautostarter.motd;

import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.common.ping.Favicons;
import net.kyori.adventure.text.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * MOTD d'un serveur : ce qu'il affichait en ligne, réutilisé quand il ne l'est
 * plus.
 * <p>
 * Le favicon décoré est mémoïsé par état. Le recalculer en Java2D à chaque ping
 * client serait du gaspillage : il ne change que lorsque le serveur revient en
 * ligne avec un favicon différent. Les réglages étant figés au démarrage, l'état
 * suffit comme clé de mémoïsation.
 */
public class ServerMotd {
    private final MotdStore store;
    private final String serverName;
    private final CommonProxy proxy;
    private final MotdSettings settings;
    private final Consumer<String> warningLogger;

    private volatile CachedMotd cached;
    private volatile CommonFavicon offlineFavicon;
    private volatile CommonFavicon startingFavicon;

    public ServerMotd(MotdStore store, String serverName, CommonProxy proxy,
                      MotdSettings settings, Consumer<String> warningLogger) {
        this.store = store;
        this.serverName = serverName;
        this.proxy = proxy;
        this.settings = settings;
        this.warningLogger = warningLogger;
        this.cached = store.load(serverName);
    }

    /**
     * Prend acte du ping d'un serveur en ligne et conserve son MOTD si celui-ci
     * a changé.
     * <p>
     * L'écriture n'a lieu que sur changement réel : le serveur est interrogé
     * toutes les 15 secondes, réécrire les fichiers à chaque fois serait inutile.
     *
     * @param onlinePing le ping obtenu du serveur, éventuellement null
     */
    public void observeOnline(CommonServerPing onlinePing) {
        if (onlinePing == null) {
            return;
        }

        Component description = onlinePing.getDescriptionComponent();
        CommonFavicon favicon = onlinePing.getFavicon();
        byte[] faviconPng = favicon == null ? null : Favicons.decodeToPng(favicon.getEncoded());
        int maxPlayers = onlinePing.getMaxPlayers();

        // On conserve ce qu'on avait quand le serveur ne fournit pas l'information.
        CachedMotd candidate = new CachedMotd(
                description != null ? description : cached.description(),
                faviconPng != null ? faviconPng : cached.faviconPng(),
                maxPlayers > 0 ? maxPlayers : cached.maxPlayers());

        if (candidate.equals(cached)) {
            return;
        }

        boolean faviconChanged = !java.util.Arrays.equals(candidate.faviconPng(), cached.faviconPng());
        this.cached = candidate;
        if (faviconChanged) {
            // Les rendus memoises portaient sur l'ancienne image.
            this.offlineFavicon = null;
            this.startingFavicon = null;
        }

        try {
            store.save(serverName, candidate);
        } catch (IOException | RuntimeException ex) {
            // Le cache disque est un confort, pas une necessite : en cas d'echec
            // le MOTD reste correct pour cette session, il sera simplement perdu
            // au prochain redemarrage.
            warningLogger.accept("Impossible d'enregistrer le MOTD de " + serverName + " : " + ex.getMessage());
        }
    }

    /**
     * Retourne la description conservée.
     *
     * @return la description, ou null si le serveur n'a jamais été vu en ligne
     */
    public Component getDescription() {
        return cached.description();
    }

    /**
     * Retourne le nombre maximum de joueurs conservé.
     *
     * @return le maximum, ou 0 si inconnu
     */
    public int getMaxPlayers() {
        return cached.maxPlayers();
    }

    /**
     * Indique si un MOTD a déjà été observé pour ce serveur.
     *
     * @return true si rien n'est disponible
     */
    public boolean isEmpty() {
        return cached.isEmpty();
    }

    /**
     * Retourne le favicon conservé, décoré selon les réglages de l'état.
     *
     * @param status l'état du serveur
     * @return le favicon, ou null si aucun n'est disponible ou si les réglages
     *         n'appellent pas le favicon conservé
     */
    public CommonFavicon getFavicon(MinecraftServerStatus status) {
        MotdSettings.State state = settings.forStatus(status);
        if (state == null || state.favicon() != MotdSettings.Favicon.CACHED) {
            return null;
        }

        if (status == MinecraftServerStatus.OFFLINE) {
            CommonFavicon memoized = offlineFavicon;
            if (memoized == null) {
                memoized = render(state, FaviconRenderer.Badge.STOP);
                offlineFavicon = memoized;
            }
            return memoized;
        }

        CommonFavicon memoized = startingFavicon;
        if (memoized == null) {
            memoized = render(state, FaviconRenderer.Badge.START);
            startingFavicon = memoized;
        }
        return memoized;
    }

    /**
     * @param state le réglage de l'état concerné
     * @param badge la pastille propre à cet état, appliquée seulement si les
     *              réglages la demandent
     */
    private CommonFavicon render(MotdSettings.State state, FaviconRenderer.Badge badge) {
        BufferedImage source = Favicons.read(cached.faviconPng());
        if (source == null) {
            return null;
        }

        BufferedImage decorated = FaviconRenderer.render(source, state.faviconGrayscale(),
                state.faviconBadge() ? badge : null);
        if (decorated == null) {
            return null;
        }
        try {
            return proxy.createFavicon(decorated);
        } catch (RuntimeException ex) {
            warningLogger.accept("Impossible de construire le favicon de " + serverName + " : " + ex.getMessage());
            return null;
        }
    }
}
