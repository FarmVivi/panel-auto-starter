package fr.farmvivi.panelautostarter;

import com.mattmalec.pterodactyl4j.PteroAction;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests pour les méthodes start() et stop() de MinecraftServer.
 */
public class MinecraftServerStartStopTest {

    @Mock
    private PanelAutoStarter mockPanelAutoStarter;

    @Mock
    private ClientServer mockClientServer;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private PteroAction<Void> mockPteroAction;

    private MinecraftServer minecraftServer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockPanelAutoStarter.getConfig()).thenReturn(mockConfiguration);
        when(mockConfiguration.getLong("server-start.check-interval-normal", 15)).thenReturn(15L);
        when(mockConfiguration.getLong("server-start.check-interval-startup", 3)).thenReturn(3L);
        when(mockPanelAutoStarter.getLogger()).thenReturn(mockLogger);
        
        // Mock ClientServer.start() pour retourner un PteroAction
        when(mockClientServer.start()).thenReturn(mockPteroAction);
        when(mockClientServer.stop()).thenReturn(mockPteroAction);

        minecraftServer = new MinecraftServer(mockPanelAutoStarter, 
            new MockCommonServer("test-server", "Test Server"), 
            mockClientServer);
    }

    /**
     * Test : Vérifier que start() change le statut à STARTING
     */
    @Test
    public void testStartChangesStatusToStarting() {
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(), "Le statut doit être OFFLINE");

        minecraftServer.start();

        assertEquals(MinecraftServerStatus.STARTING, minecraftServer.getStatus(), "Le statut doit être STARTING");
    }

    /**
     * Test : Vérifier que start() appelle le ClientServer
     */
    @Test
    public void testStartCallsClientServer() {
        minecraftServer.start();
        verify(mockClientServer, times(1)).start();
    }

    /**
     * Test : Vérifier que start() ne s'exécute qu'une fois
     */
    @Test
    public void testStartOnlyWorksOnce() {
        minecraftServer.start();
        minecraftServer.start();
        minecraftServer.start();

        verify(mockClientServer, times(1)).start();
    }

    /**
     * Test : Vérifier que stop() ne s'exécute pas depuis OFFLINE
     */
    @Test
    public void testStopIgnoredFromOffline() {
        minecraftServer.stop();

        verify(mockClientServer, never()).stop();
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(), "Le statut doit rester OFFLINE");
    }

    /**
     * Test : Vérifier que stop() ne s'exécute pas depuis STARTING
     */
    @Test
    public void testStopIgnoredFromStarting() {
        minecraftServer.start();
        assertEquals(MinecraftServerStatus.STARTING, minecraftServer.getStatus(), "Le statut doit être STARTING");

        minecraftServer.stop();

        verify(mockClientServer, never()).stop();
        assertEquals(MinecraftServerStatus.STARTING, minecraftServer.getStatus(), "Le statut doit rester STARTING");
    }

    /**
     * Test : Vérifier que la queue persiste après start()
     */
    @Test
    public void testQueuePersistsDuringStart() {
        List<CommonPlayer> queue = minecraftServer.getQueue();
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        queue.add(player1);
        queue.add(player2);

        minecraftServer.start();

        assertEquals(2, queue.size(), "La queue doit contenir 2 joueurs");
        assertTrue(queue.contains(player1), "Le joueur 1 doit être dans la queue");
        assertTrue(queue.contains(player2), "Le joueur 2 doit être dans la queue");
    }

    /**
     * Test : Vérifier que le status initial est OFFLINE
     */
    @Test
    public void testInitialStatusOffline() {
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(), "Le statut doit être OFFLINE");
    }

    /**
     * Test : Vérifier les getters après construction
     */
    @Test
    public void testGettersAfterConstruction() {
        assertNotNull(minecraftServer.getServer(), "Le serveur ne doit pas être null");
        assertNotNull(minecraftServer.getQueue(), "La queue ne doit pas être null");
        assertNull(minecraftServer.getServerPing(), "Le ping doit être null");
        assertEquals(0, minecraftServer.getQueue().size(), "La queue doit être vide");
    }
}
