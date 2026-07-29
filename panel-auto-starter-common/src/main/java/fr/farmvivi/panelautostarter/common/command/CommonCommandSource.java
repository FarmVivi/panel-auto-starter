package fr.farmvivi.panelautostarter.common.command;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import net.kyori.adventure.text.Component;

/**
 * Auteur d'une commande : un joueur, ou la console.
 * <p>
 * Les deux plateformes distinguent le joueur de la console, et une commande
 * d'administration doit rester utilisable depuis la console — c'est le seul
 * moyen d'agir quand plus personne n'est connecté, ou depuis le panel.
 */
public interface CommonCommandSource {

    /**
     * Indique si l'auteur possède une permission.
     * <p>
     * La console répond toujours oui : elle n'a personne au-dessus d'elle.
     *
     * @param permission le nœud de permission
     * @return true s'il l'a
     */
    boolean hasPermission(String permission);

    /**
     * Envoie un message à l'auteur.
     *
     * @param message le message
     */
    void sendMessage(Component message);

    /**
     * Retourne le nom de l'auteur, pour les journaux.
     *
     * @return le pseudo du joueur, ou un nom désignant la console
     */
    String getName();

    /**
     * Retourne le joueur à l'origine de la commande.
     *
     * @return le joueur, ou null si la commande vient de la console
     */
    CommonPlayer asPlayer();
}
