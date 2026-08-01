package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.common.CommonPlayer;

/**
 * Présente un {@link Menu} à un joueur.
 * <p>
 * Deux implémentations sont prévues : un rendu en chat cliquable, qui ne
 * dépend de rien et fonctionne sur les deux proxys, et un rendu en inventaire,
 * qui suppose une bibliothèque de manipulation de paquets. Cette interface
 * existe pour que la seconde puisse être remplacée — ou retirée — sans que le
 * contenu des menus ne bouge : une bibliothèque qui plafonne sous la version
 * du jeu est un risque connu, mieux vaut qu'il soit cantonné derrière une
 * frontière.
 */
public interface MenuRenderer {

    /**
     * Retourne le nom du rendu, tel qu'il s'écrit dans la configuration.
     *
     * @return le nom du rendu
     */
    String getName();

    /**
     * Indique si ce rendu peut fonctionner ici et maintenant.
     * <p>
     * Un rendu en inventaire répondra non si sa bibliothèque est absente ou si
     * le client est trop récent pour elle. C'est ce qui permet de retomber sur
     * un rendu qui marche plutôt que d'ouvrir un menu vide.
     *
     * @param viewer le joueur concerné
     * @return true si le menu peut lui être présenté
     */
    boolean isAvailable(CommonPlayer viewer);

    /**
     * Présente le menu.
     *
     * @param viewer le joueur
     * @param menu   le menu
     */
    void open(CommonPlayer viewer, Menu menu);
}
