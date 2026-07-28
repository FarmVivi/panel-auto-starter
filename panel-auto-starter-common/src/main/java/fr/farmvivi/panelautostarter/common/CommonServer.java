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
    /**
     * Indique si le proxy interdit de lui-même ce serveur à un joueur.
     * <p>
     * BungeeCord possède la notion en propre : un serveur marqué
     * {@code restricted} dans sa configuration exige la permission
     * {@code bungeecord.server.<nom>}. La réutiliser évite de demander à
     * l'administrateur de déclarer deux fois la même restriction.
     * <p>
     * Velocity n'a pas d'équivalent — {@code RegisteredServer} ignore la notion
     * de permission — et répond donc toujours que l'accès est libre. La
     * convention du plugin, elle, vaut sur les deux plateformes.
     *
     * @param player le joueur
     * @return true si le proxy laisse passer ce joueur
     */
    default boolean isAllowedByProxy(CommonPlayer player) {
        return true;
    }

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
