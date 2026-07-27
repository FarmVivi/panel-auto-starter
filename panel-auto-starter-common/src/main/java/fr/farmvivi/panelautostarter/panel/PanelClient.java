package fr.farmvivi.panelautostarter.panel;

/**
 * Point d'entrée vers un panel de gestion de serveurs.
 * <p>
 * Aucun type propre à une bibliothèque de panel ne doit apparaître dans cette
 * interface : c'est ce qui permet de changer d'implémentation sans toucher au
 * reste du plugin.
 */
public interface PanelClient {
    /**
     * Retourne le type de panel derrière ce client.
     *
     * @return le type de panel
     */
    PanelType getType();

    /**
     * Récupère un serveur depuis son identifiant côté panel.
     * <p>
     * Appel <strong>bloquant</strong>.
     *
     * @param identifier l'identifiant du serveur
     * @return le serveur correspondant
     */
    PanelServer retrieveServer(String identifier);
}
