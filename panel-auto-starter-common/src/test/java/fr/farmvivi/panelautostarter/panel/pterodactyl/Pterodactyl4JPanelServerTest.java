package fr.farmvivi.panelautostarter.panel.pterodactyl;

import com.mattmalec.pterodactyl4j.PteroAction;
import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import com.mattmalec.pterodactyl4j.client.entities.Utilization;
import fr.farmvivi.panelautostarter.panel.PanelServerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de la résolution du serveur Pterodactyl4J.
 * <p>
 * Le bug qu'ils gardent : Pterodactyl4J renvoie {@code null} quand la requête
 * échoue, et ce {@code null} était résolu une fois pour toutes au démarrage du
 * proxy. Un panel injoignable quelques secondes à ce moment-là rendait le
 * serveur impossible à démarrer, chaque {@code /pas start} finissant en
 * {@code NullPointerException}, jusqu'au redémarrage du proxy. Constaté sur un
 * vrai Velocity 4.3.1 et un vrai BungeeCord.
 */
public class Pterodactyl4JPanelServerTest {
    private static final String ID = "abc";

    private PteroClient client;
    private PteroAction<ClientServer> retrieval;
    private ClientServer server;
    private PteroAction<Void> startAction;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        client = mock(PteroClient.class);
        retrieval = mock(PteroAction.class);
        server = mock(ClientServer.class);
        startAction = mock(PteroAction.class);
        when(client.retrieveServerByIdentifier(ID)).thenReturn(retrieval);
        when(server.start()).thenReturn(startAction);
    }

    @Test
    public void testConstructionDoesNotContactThePanel() {
        new Pterodactyl4JPanelServer(ID, client);

        verifyNoInteractions(client);
    }

    @Test
    public void testFailedResolutionIsRetriedNotRemembered() {
        Utilization utilization = mock(Utilization.class);
        PteroAction<Utilization> utilizationAction = mockAction();
        when(utilizationAction.execute()).thenReturn(utilization);
        when(utilization.getState()).thenReturn(UtilizationState.OFFLINE);
        when(server.retrieveUtilization()).thenReturn(utilizationAction);
        when(retrieval.execute()).thenReturn(null, server);

        Pterodactyl4JPanelServer panelServer = new Pterodactyl4JPanelServer(ID, client);

        // L'echec doit lever, pour que la surveillance le traite comme un panel
        // injoignable, et non laisser un null qui casserait chaque appel suivant.
        assertThrows(IllegalStateException.class, panelServer::retrieveState);
        assertEquals(PanelServerState.OFFLINE, panelServer.retrieveState(),
                "Le panel revenu, le serveur doit etre resolu au tour suivant");
    }

    @Test
    public void testSuccessfulResolutionIsCached() {
        PteroAction<Utilization> utilizationAction = mockAction();
        Utilization utilization = mock(Utilization.class);
        when(utilizationAction.execute()).thenReturn(utilization);
        when(server.retrieveUtilization()).thenReturn(utilizationAction);
        when(retrieval.execute()).thenReturn(server);

        Pterodactyl4JPanelServer panelServer = new Pterodactyl4JPanelServer(ID, client);
        panelServer.retrieveState();
        panelServer.retrieveState();
        panelServer.start();

        verify(client, times(1)).retrieveServerByIdentifier(ID);
        verify(startAction).executeAsync();
    }

    @Test
    public void testStartAfterFailedResolutionResolvesAgainWithoutBlocking() {
        Pterodactyl4JPanelServer panelServer = new Pterodactyl4JPanelServer(ID, client);

        // Premier essai : le panel echoue, Pterodactyl4J rappelle avec null.
        answerAsyncWith(null);
        assertDoesNotThrow(panelServer::start);
        verify(server, never()).start();

        // Second essai, panel revenu : le demarrage part.
        answerAsyncWith(server);
        panelServer.start();

        verify(startAction).executeAsync();
        // Jamais d'appel bloquant depuis start() : il est appele depuis les
        // evenements du proxy.
        verify(retrieval, never()).execute();
    }

    @SuppressWarnings("unchecked")
    private void answerAsyncWith(ClientServer result) {
        doAnswer(invocation -> {
            ((Consumer<ClientServer>) invocation.getArgument(0)).accept(result);
            return null;
        }).when(retrieval).executeAsync(any());
    }

    @SuppressWarnings("unchecked")
    private static <T> PteroAction<T> mockAction() {
        return mock(PteroAction.class);
    }
}
