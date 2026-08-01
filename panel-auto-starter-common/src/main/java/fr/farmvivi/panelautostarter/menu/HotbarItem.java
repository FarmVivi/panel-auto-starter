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
}
