package fr.farmvivi.panelautostarter.panel.pelican;

import fr.farmvivi.panelautostarter.panel.PanelType;
import fr.farmvivi.panelautostarter.panel.pterodactyl.Pterodactyl4JPanelClient;

/**
 * Client pour le panel Pelican.
 * <p>
 * Pelican est un fork de Pterodactyl et son API client est volontairement
 * maintenue compatible. Les quatre opérations utilisées par ce plugin
 * (récupération d'un serveur, lecture de son état, démarrage, arrêt) sont
 * aujourd'hui identiques sur les deux panels : cette classe n'a donc
 * <strong>rien à surcharger</strong>.
 * <p>
 * Elle existe malgré tout comme point d'extension. Le jour où Pelican fait
 * diverger un endpoint, la correction consiste à surcharger ici la méthode
 * concernée — au besoin avec un appel HTTP écrit à la main — sans toucher au
 * reste du plugin. C'est aussi ce qui rend le type de panel visible dans les
 * logs et la configuration.
 */
public class PelicanPanelClient extends Pterodactyl4JPanelClient {
    public PelicanPanelClient(String url, String token) {
        super(url, token);
    }

    @Override
    public PanelType getType() {
        return PanelType.PELICAN;
    }
}
