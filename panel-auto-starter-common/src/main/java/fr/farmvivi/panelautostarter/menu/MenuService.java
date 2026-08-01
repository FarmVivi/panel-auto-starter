package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.common.CommonPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Choisit le rendu par lequel un menu sera présenté.
 * <p>
 * Les rendus sont essayés dans l'ordre d'enregistrement, le plus riche
 * d'abord, et le premier qui se déclare utilisable l'emporte. Le rendu en chat
 * ferme toujours la marche : il ne dépend de rien et ne peut donc pas se
 * dérober, ce qui garantit qu'un menu s'ouvre toujours.
 * <p>
 * C'est ce repli qui rend un rendu en inventaire adoptable sans risque : s'il
 * cesse de fonctionner — bibliothèque en retard sur la version du jeu, client
 * trop récent — les menus s'affichent en chat au lieu de disparaître.
 */
public final class MenuService {

    private final List<MenuRenderer> renderers = new ArrayList<>();
    private final MenuSettings settings;

    public MenuService(MenuSettings settings) {
        this.settings = settings;
    }

    /**
     * Ajoute un rendu, prioritaire sur ceux déjà enregistrés.
     *
     * @param renderer le rendu
     */
    public void register(MenuRenderer renderer) {
        renderers.add(renderer);
    }

    /**
     * Retourne le rendu qui serait employé pour un joueur.
     *
     * @param viewer le joueur
     * @return le rendu retenu, ou null si aucun n'est utilisable
     */
    public MenuRenderer resolve(CommonPlayer viewer) {
        String wanted = settings.renderer();

        // Un rendu nomme explicitement est exige, non suggere : si
        // l'administrateur a demande « chat », lui ouvrir un inventaire serait
        // desobeir. Seul « auto » laisse le choix.
        if (!MenuSettings.RENDERER_AUTO.equals(wanted)) {
            for (MenuRenderer renderer : renderers) {
                if (renderer.getName().equals(wanted) && renderer.isAvailable(viewer)) {
                    return renderer;
                }
            }
        }

        for (MenuRenderer renderer : renderers) {
            if (renderer.isAvailable(viewer)) {
                return renderer;
            }
        }
        return null;
    }

    /**
     * Présente un menu à un joueur.
     *
     * @param viewer le joueur
     * @param menu   le menu
     * @return true si un rendu a pu s'en charger
     */
    public boolean open(CommonPlayer viewer, Menu menu) {
        MenuRenderer renderer = resolve(viewer);
        if (renderer == null) {
            return false;
        }
        renderer.open(viewer, menu);
        return true;
    }

    public MenuSettings getSettings() {
        return settings;
    }
}
