package fr.farmvivi.panelautostarter.panel;

import fr.farmvivi.panelautostarter.panel.pelican.PelicanPanelClient;
import fr.farmvivi.panelautostarter.panel.pterodactyl.Pterodactyl4JPanelClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la fabrique de clients de panel.
 * <p>
 * La construction d'un client n'ouvre aucune connexion réseau : Pterodactyl4J
 * se contente de mémoriser l'URL et le token, les appels HTTP n'ont lieu qu'à
 * l'usage. Ces tests peuvent donc utiliser une URL fictive.
 */
public class PanelClientFactoryTest {
    private static final String URL = "https://panel.example.com";
    private static final String TOKEN = "ptlc_token_de_test";

    @Test
    public void testCreatePterodactylClient() {
        PanelClient client = PanelClientFactory.create(PanelType.PTERODACTYL, URL, TOKEN);

        assertInstanceOf(Pterodactyl4JPanelClient.class, client,
                "PTERODACTYL doit produire un Pterodactyl4JPanelClient");
        assertEquals(PanelType.PTERODACTYL, client.getType(),
                "Le client doit se déclarer comme Pterodactyl");
    }

    @Test
    public void testCreatePelicanClient() {
        PanelClient client = PanelClientFactory.create(PanelType.PELICAN, URL, TOKEN);

        assertInstanceOf(PelicanPanelClient.class, client,
                "PELICAN doit produire un PelicanPanelClient");
        assertEquals(PanelType.PELICAN, client.getType(),
                "Le client doit se déclarer comme Pelican");
    }

    /**
     * Pelican réutilise aujourd'hui l'implémentation Pterodactyl : c'est
     * volontaire, l'API client des deux panels étant compatible. Ce test
     * documente cette dépendance d'héritage, qui est le point d'extension prévu
     * pour les futures divergences de Pelican.
     */
    @Test
    public void testPelicanClientExtendsPterodactylImplementation() {
        PanelClient client = PanelClientFactory.create(PanelType.PELICAN, URL, TOKEN);

        assertInstanceOf(Pterodactyl4JPanelClient.class, client,
                "PelicanPanelClient doit hériter de l'implémentation Pterodactyl4J");
    }

    @Test
    public void testEachPanelTypeIsSupported() {
        for (PanelType type : PanelType.values()) {
            PanelClient client = PanelClientFactory.create(type, URL, TOKEN);
            assertNotNull(client, "Aucun client produit pour " + type);
            assertEquals(type, client.getType(), "Type incohérent pour " + type);
        }
    }
}
