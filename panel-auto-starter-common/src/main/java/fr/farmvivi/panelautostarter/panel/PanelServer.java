package fr.farmvivi.panelautostarter.panel;

/**
 * Un serveur piloté depuis le panel.
 * <p>
 * Aucun type propre à une bibliothèque de panel ne doit apparaître dans cette
 * interface : c'est ce qui permet de changer d'implémentation sans toucher au
 * reste du plugin.
 */
public interface PanelServer {
    /**
     * Retourne l'identifiant du serveur côté panel.
     *
     * @return l'identifiant
     */
    String getIdentifier();

    /**
     * Interroge le panel pour connaître l'état courant du serveur.
     * <p>
     * Appel <strong>bloquant</strong>.
     *
     * @return l'état rapporté par le panel
     */
    PanelServerState retrieveState();

    /**
     * Demande le démarrage du serveur. L'appel est asynchrone : il rend la main
     * sans attendre que le serveur soit effectivement démarré.
     */
    void start();

    /**
     * Demande l'arrêt du serveur. L'appel est asynchrone : il rend la main sans
     * attendre que le serveur soit effectivement arrêté.
     */
    void stop();
}
