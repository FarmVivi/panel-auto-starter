package fr.farmvivi.panelautostarter.panel.pterodactyl;

import com.mattmalec.pterodactyl4j.PteroBuilder;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import fr.farmvivi.panelautostarter.panel.PanelClient;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelType;

/**
 * Implémentation de {@link PanelClient} adossée à Pterodactyl4J.
 * <p>
 * Avec {@code fr.farmvivi.panelautostarter.panel.pelican}, ce package est le
 * <strong>seul</strong> endroit du plugin autorisé à référencer
 * {@code com.mattmalec.pterodactyl4j}.
 */
public class Pterodactyl4JPanelClient implements PanelClient {
    private final PteroClient client;

    public Pterodactyl4JPanelClient(String url, String token) {
        this.client = PteroBuilder.createClient(url, token);
    }

    @Override
    public PanelType getType() {
        return PanelType.PTERODACTYL;
    }

    @Override
    public PanelServer retrieveServer(String identifier) {
        return new Pterodactyl4JPanelServer(identifier, client.retrieveServerByIdentifier(identifier).execute());
    }

    /**
     * Donne accès au client Pterodactyl4J sous-jacent aux sous-classes.
     *
     * @return le client délégué
     */
    protected PteroClient getClient() {
        return client;
    }
}
