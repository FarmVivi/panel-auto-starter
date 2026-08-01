package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.common.CommonPlayer;

/**
 * Objet posé dans la barre d'action rapide d'un joueur, qui ouvre un menu.
 * <p>
 * Comme pour les menus, l'interface existe pour que sa mise en œuvre — qui
 * suppose de fabriquer des paquets — reste cantonnée derrière une frontière,
 * et que le reste du plugin puisse s'en passer.
 */
public interface HotbarItem {

    /**
     * Pose l'objet dans l'inventaire d'un joueur.
     *
     * @param player le joueur
     */
    void give(CommonPlayer player);

    /**
     * Oublie un joueur qui n'a plus l'objet.
     * <p>
     * Nécessaire, et pas seulement par propreté : tant qu'un joueur est réputé
     * porter l'objet, ses clics d'inventaire sont interceptés. Continuer à le
     * croire après son départ du serveur d'attente reviendrait à s'immiscer
     * dans l'inventaire d'un serveur de jeu, où le plugin n'a rien à dire.
     *
     * @param uuid l'identifiant du joueur
     */
    default void forget(java.util.UUID uuid) {
    }
}
