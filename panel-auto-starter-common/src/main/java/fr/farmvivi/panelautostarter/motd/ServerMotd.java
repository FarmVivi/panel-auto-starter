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
 * Le rendu décoré du favicon (désaturation, pastille d'état) est mémoïsé. Le
 * recalculer en Java2D à chaque ping client serait du gaspillage pur : il ne
 * change que lorsque le serveur revient en ligne avec un favicon différent.
 */
public class ServerMotd {
    private final MotdStore store;
    private final String serverName;
    private final CommonProxy proxy;
    private final Consumer<String> warningLogger;

    private volatile CachedMotd cached;
    private volatile CommonFavicon offlineFavicon;
    private volatile CommonFavicon startingFavicon;

    public ServerMotd(MotdStore store, String serverName, CommonProxy proxy, Consumer<String> warningLogger) {
        this.store = store;
        this.serverName = serverName;
        this.proxy = proxy;
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

        // On conserve ce qu'on avait si le serveur ne fournit pas l'un des deux.
        CachedMotd candidate = new CachedMotd(
                description != null ? description : cached.description(),
                faviconPng != null ? faviconPng : cached.faviconPng());

        if (candidate.equals(cached)) {
            return;
        }

        this.cached = candidate;
        // Le favicon a potentiellement change : les rendus memoises sont perimes.
        this.offlineFavicon = null;
        this.startingFavicon = null;

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
     * Indique si un MOTD a déjà été observé pour ce serveur.
     *
     * @return true si rien n'est disponible
     */
    public boolean isEmpty() {
        return cached.isEmpty();
    }

    /**
     * Retourne le favicon décoré correspondant à l'état du serveur.
     *
     * @param status l'état du serveur
     * @return le favicon, ou null si aucun n'a été observé ou si le rendu échoue
     */
    public CommonFavicon getFavicon(MinecraftServerStatus status) {
        return switch (status) {
            case OFFLINE -> offlineFavicon();
            case STARTING -> startingFavicon();
            case ONLINE -> null;
        };
    }

    private CommonFavicon offlineFavicon() {
        CommonFavicon memoized = offlineFavicon;
        if (memoized == null) {
            memoized = render(FaviconRenderer::offline);
            offlineFavicon = memoized;
        }
        return memoized;
    }

    private CommonFavicon startingFavicon() {
        CommonFavicon memoized = startingFavicon;
        if (memoized == null) {
            memoized = render(FaviconRenderer::starting);
            startingFavicon = memoized;
        }
        return memoized;
    }

    private CommonFavicon render(java.util.function.UnaryOperator<BufferedImage> decorator) {
        BufferedImage source = Favicons.read(cached.faviconPng());
        if (source == null) {
            return null;
        }
        BufferedImage decorated = decorator.apply(source);
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
