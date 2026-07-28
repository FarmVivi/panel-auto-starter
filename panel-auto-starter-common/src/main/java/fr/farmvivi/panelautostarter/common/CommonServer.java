package fr.farmvivi.panelautostarter.common;

import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;

/**
 * Represents a server.
 */
public interface CommonServer {
    /**
     * Returns the name of the server.
     *
     * @return the name of the server
     */
    String getName();

    /**
     * Returns the display name of the server.
     *
     * @return the display name of the server
     */
    String getDisplayName();

    /**
     * Returns player count.
     *
     * @return player count
     */
    int getPlayerCount();

    /**
     * Pings the server.
     *
     * @param callback the callback
     */
    void ping(Callback<CommonServerPing> callback);

    /**
     * Ping le serveur en bornant l'attente d'une réponse.
     * <p>
     * Un serveur en cours de démarrage accepte la connexion — le port est
     * publié avant que le jeu n'écoute — sans y répondre. Sans borne, le proxy
     * attend son propre délai, une trentaine de secondes, ce qui retarde
     * d'autant la détection de sa mise en ligne.
     * <p>
     * L'implémentation par défaut ignore la borne : toutes les plateformes ne
     * permettent pas de la régler. L'appelant doit donc prévoir sa propre
     * échéance plutôt que de compter sur celle-ci.
     *
     * @param callback le rappel appelé avec le résultat, ou null en cas d'échec
     * @param timeout  l'attente maximale souhaitée
     */
    default void ping(Callback<CommonServerPing> callback, java.time.Duration timeout) {
        ping(callback);
    }
}
