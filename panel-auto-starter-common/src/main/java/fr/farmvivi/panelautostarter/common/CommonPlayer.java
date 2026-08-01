package fr.farmvivi.panelautostarter.common;

import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Represents a player.
 */
public interface CommonPlayer {
    /**
     * Returns whether the player is online.
     *
     * @return whether the player is online
     */
    boolean isOnline();

    /**
     * Returns the username of the player.
     *
     * @return the username of the player
     */
    String getUsername();

    /**
     * Returns the display name of the player.
     *
     * @return the display name of the player
     */
    String getDisplayName();

    /**
     * Returns the UUID of the player.
     *
     * @return the UUID of the player
     */
    UUID getUniqueId();

    /**
     * Indique si le joueur possède une permission.
     * <p>
     * Passe par l'API du proxy, que les gestionnaires de permissions —
     * LuckPerms notamment — alimentent en s'y greffant. Il n'y a donc rien à
     * intégrer de leur côté : demander au proxy revient à leur demander.
     *
     * @param permission le nœud de permission
     * @return true si le joueur l'a
     */
    boolean hasPermission(String permission);

    /**
     * Retourne l'objet par lequel la plateforme représente ce joueur.
     * <p>
     * Réservé aux bibliothèques tierces qui raisonnent en objets natifs du
     * proxy — la manipulation de paquets, notamment. Le reste du plugin doit
     * passer par cette interface : c'est elle qui rend les deux proxys
     * interchangeables.
     *
     * @return le joueur natif du proxy
     */
    Object getPlatformHandle();

    /**
     * Retourne la langue annoncée par le client du joueur.
     * <p>
     * C'est elle qui décide dans quelle langue il lit ses messages : deux
     * joueurs d'un même serveur peuvent donc en lire deux différentes.
     *
     * @return la langue, ou null si le client ne l'a pas encore annoncée
     */
    default java.util.Locale getLocale() {
        return null;
    }

    /**
     * Retourne l'apparence du joueur, si la plateforme la donne.
     * <p>
     * L'implémentation par défaut n'en retourne aucune : BungeeCord n'expose pas
     * le profil de connexion dans son API publique, et l'atteindre supposerait
     * d'entrer par réflexion dans ses classes internes — la fragilité même que
     * cette abstraction existe pour éviter. Une tête générique s'affiche alors.
     *
     * @return l'apparence, ou null si elle est inconnue
     */
    default PlayerSkin getSkin() {
        return null;
    }

    /**
     * Sends a message to the player.
     *
     * @param message the message to send
     */
    void sendMessage(String message);

    /**
     * Sends a message to the player.
     *
     * @param message the message to send
     */
    void sendMessage(Component message);

    /**
     * Affiche un message dans la barre d'action, juste au-dessus de la barre
     * d'inventaire.
     * <p>
     * Contrairement au chat, cet emplacement n'accumule rien : chaque envoi
     * remplace le précédent. C'est ce qui permet de le rafraîchir chaque
     * seconde sans noyer le joueur, et ce qui impose de le réémettre
     * régulièrement — le client l'efface de lui-même au bout de quelques
     * secondes.
     *
     * @param message le message à afficher
     */
    void sendActionBar(Component message);

    /**
     * Displays a title and subtitle on the player's screen.
     *
     * @param title    the title, may be empty
     * @param subtitle the subtitle, may be empty
     * @param fadeIn   fade-in duration
     * @param stay     duration the title remains fully visible
     * @param fadeOut  fade-out duration
     */
    void showTitle(Component title, Component subtitle, Duration fadeIn, Duration stay, Duration fadeOut);

    /**
     * Joue un son au joueur, si la plateforme le permet.
     * <p>
     * Sans effet sur BungeeCord, qui n'expose aucune API de son ; voir
     * {@link CommonProxy#supportsSound()}.
     *
     * @param soundKey l'identifiant du son, par exemple {@code block.note_block.pling}
     * @param volume   le volume
     * @param pitch    la hauteur
     */
    void playSound(String soundKey, float volume, float pitch);

    /**
     * Connects the player to the server.
     *
     * @param server the server to connect to
     */
    void connectToServer(CommonServer server);

    /**
     * Retrieve the server the player is connected to.
     *
     * @return the server the player is connected to
     */
    CommonServer getServer();
}
