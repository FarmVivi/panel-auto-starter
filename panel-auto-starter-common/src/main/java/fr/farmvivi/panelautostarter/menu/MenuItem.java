package fr.farmvivi.panelautostarter.menu;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Entrée d'un menu.
 * <p>
 * L'action d'une entrée est <strong>une commande</strong>, et non un rappel de
 * code. Ce choix est ce qui rend les deux rendus possibles : un clic de chat ne
 * peut transporter qu'une commande, et un clic d'inventaire se traduit sans
 * peine en exécution de commande. Un rappel obligerait le rendu chat à inventer
 * un jeton de session pour retrouver l'action, pour un gain nul.
 * <p>
 * Cela a une conséquence heureuse : l'action passe par le même chemin qu'une
 * commande tapée à la main, et se retrouve donc soumise aux mêmes contrôles.
 * Un menu ne peut rien faire que son utilisateur n'aurait pu faire seul.
 *
 * @param icon    identifiant d'objet pour un rendu en inventaire, par exemple
 *                {@code minecraft:lime_concrete} ; ignoré par le rendu en chat
 * @param label   texte de l'entrée
 * @param lore    lignes de détail, affichées au survol ou sous l'entrée
 * @param command commande exécutée au clic, sans barre oblique ; null pour une
 *                entrée purement informative
 * @param enabled false pour une entrée grisée, qui s'affiche sans agir
 */
public record MenuItem(String icon, Component label, List<Component> lore, String command,
                       boolean enabled) {

    public MenuItem {
        lore = List.copyOf(lore);
    }

    /**
     * Entrée cliquable.
     *
     * @param icon    l'objet représentatif
     * @param label   le texte
     * @param lore    le détail
     * @param command la commande exécutée au clic
     * @return l'entrée
     */
    public static MenuItem of(String icon, Component label, List<Component> lore, String command) {
        return new MenuItem(icon, label, lore, command, true);
    }

    /**
     * Entrée informative, sans action.
     *
     * @param icon  l'objet représentatif
     * @param label le texte
     * @param lore  le détail
     * @return l'entrée
     */
    public static MenuItem info(String icon, Component label, List<Component> lore) {
        return new MenuItem(icon, label, lore, null, false);
    }

    /**
     * Indique si l'entrée déclenche quelque chose.
     *
     * @return true si un clic a un effet
     */
    public boolean isActionable() {
        return enabled && command != null && !command.isBlank();
    }
}
