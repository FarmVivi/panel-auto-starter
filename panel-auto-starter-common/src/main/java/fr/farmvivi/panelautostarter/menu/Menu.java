package fr.farmvivi.panelautostarter.menu;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Description d'un menu, indépendante de la façon dont il sera affiché.
 * <p>
 * C'est le pivot qui permet de changer de rendu — chat cliquable aujourd'hui,
 * coffre demain — sans toucher à ce que les menus disent. Un menu ne connaît
 * ni inventaire, ni paquet, ni composant de chat : il énonce un titre et une
 * suite d'entrées, à charge du {@link MenuRenderer} de les présenter.
 *
 * @param title le titre du menu
 * @param items les entrées, dans l'ordre
 */
public record Menu(Component title, List<MenuItem> items) {

    public Menu {
        items = List.copyOf(items);
    }
}
