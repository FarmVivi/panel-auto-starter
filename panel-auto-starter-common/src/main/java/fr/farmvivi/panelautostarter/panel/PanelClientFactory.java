package fr.farmvivi.panelautostarter.panel;

import fr.farmvivi.panelautostarter.panel.pelican.PelicanPanelClient;
import fr.farmvivi.panelautostarter.panel.pterodactyl.Pterodactyl4JPanelClient;

/**
 * Construit le {@link PanelClient} correspondant au panel configuré.
 */
public final class PanelClientFactory {
    private PanelClientFactory() {
    }

    /**
     * Crée un client pour le panel demandé.
     *
     * @param type  le type de panel
     * @param url   l'URL du panel
     * @param token le token d'API client
     * @return le client correspondant
     */
    public static PanelClient create(PanelType type, String url, String token) {
        return switch (type) {
            case PTERODACTYL -> new Pterodactyl4JPanelClient(url, token);
            case PELICAN -> new PelicanPanelClient(url, token);
        };
    }
}
