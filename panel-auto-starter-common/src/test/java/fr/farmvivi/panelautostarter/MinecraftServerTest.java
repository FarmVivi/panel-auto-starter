package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
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
 * Tests unitaires pour MinecraftServer - Gestion de file d'attente et d'état.
 * Tests des fonctionnalités critiques et testables sans interactions complexes.
 */
public class MinecraftServerTest {

    @Mock
    private PanelAutoStarter mockPanelAutoStarter;

    @Mock
    private PanelServer mockPanelServer;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    private CommonServer testServer;
    private MinecraftServer minecraftServer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock la configuration
        when(mockPanelAutoStarter.getConfig()).thenReturn(mockConfiguration);
        when(mockConfiguration.getLong("server-start.check-interval-normal", 15)).thenReturn(15L);
        when(mockConfiguration.getLong("server-start.check-interval-startup", 3)).thenReturn(3L);

        // Mock le logger
        when(mockPanelAutoStarter.getLogger()).thenReturn(mockLogger);

        // Créer un serveur de test
        testServer = new MockCommonServer("test-server", "Test Server");

        // Initialiser MinecraftServer
        minecraftServer = new MinecraftServer(mockPanelAutoStarter, testServer, mockPanelServer, mock(ServerMotd.class));
    }

    // ========================= Tests de Queue =========================

    /**
     * Test 1.2.1 : Vérifier que la queue est initialisée vide
     */
    @Test
    public void testQueueInitiallyEmpty() {
        List<CommonPlayer> queue = minecraftServer.getQueue();
        assertNotNull(queue, "La queue ne doit pas être null");
        assertEquals(0, queue.size(), "La queue doit être vide au démarrage");
    }

    /**
     * Test 1.2.2 : Vérifier que plusieurs joueurs peuvent être ajoutés à la queue
     */
    @Test
    public void testAddPlayersToQueue() {
        List<CommonPlayer> queue = minecraftServer.getQueue();

        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");
        CommonPlayer player3 = new MockCommonPlayer("Player3");

        queue.add(player1);
        queue.add(player2);
        queue.add(player3);

        assertEquals(3, queue.size(), "La queue doit contenir 3 joueurs");
        assertTrue(queue.contains(player1), "Le joueur 1 doit être dans la queue");
        assertTrue(queue.contains(player2), "Le joueur 2 doit être dans la queue");
        assertTrue(queue.contains(player3), "Le joueur 3 doit être dans la queue");
    }

    /**
     * Test 1.2.3 : Vérifier que la queue peut être vidée
     */
    @Test
    public void testClearQueue() {
        List<CommonPlayer> queue = minecraftServer.getQueue();

        queue.add(new MockCommonPlayer("Player1"));
        queue.add(new MockCommonPlayer("Player2"));
        assertEquals(2, queue.size());

        queue.clear();
        assertEquals(0, queue.size(), "La queue doit être vide après clear()");
    }

    /**
     * Test 1.2.4 : Vérifier que les joueurs peuvent être retirés de la queue individuellement
     */
    @Test
    public void testRemovePlayerFromQueue() {
        List<CommonPlayer> queue = minecraftServer.getQueue();

        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        queue.add(player1);
        queue.add(player2);
        assertEquals(2, queue.size());

        queue.remove(player1);
        assertEquals(1, queue.size(), "La queue doit contenir 1 joueur");
        assertFalse(queue.contains(player1), "Le joueur 1 ne doit plus être dans la queue");
        assertTrue(queue.contains(player2), "Le joueur 2 doit toujours être dans la queue");
    }

    // ========================= Tests de Statut =========================

    /**
     * Test 1.2.5 : Vérifier l'état initial du serveur
     */
    @Test
    public void testServerInitializesAsOffline() {
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(), "Le serveur doit être OFFLINE au démarrage");
    }

    /**
     * Test 1.2.6 : Vérifier que serverPing est null initialement
     */
    @Test
    public void testServerPingInitiallyNull() {
        assertNull(minecraftServer.getServerPing(), "Le serverPing doit être null au démarrage");
    }

    /**
     * Test 1.2.7 : Vérifier que les données du serveur sont accessibles
     */
    @Test
    public void testServerDataAccessible() {
        assertNotNull(minecraftServer.getServer(), "Le serveur doit être accessible");
        assertEquals("test-server",
                minecraftServer.getServer().getName(), "Le nom du serveur doit être correct");
        assertEquals("Test Server",
                minecraftServer.getServer().getDisplayName(), "Le display name doit être correct");
    }

    /**
     * Test 1.2.8 : Vérifier que getStatus() retourne le bon état
     */
    @Test
    public void testGetStatusReturnsCorrectStatus() {
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());
    }

    /**
     * Test 1.2.9 : Vérifier que getServer() retourne le serveur configuré
     */
    @Test
    public void testGetServerReturnCorrectServer() {
        CommonServer server = minecraftServer.getServer();
        assertEquals("test-server", server.getName());
    }

    /**
     * Test 1.2.10 : Vérifier que getQueue() retourne la même liste
     */
    @Test
    public void testGetQueueReturnsSameList() {
        List<CommonPlayer> queue1 = minecraftServer.getQueue();
        List<CommonPlayer> queue2 = minecraftServer.getQueue();

        assertSame(queue1, queue2, "getQueue() doit retourner la même instance");
    }

    /**
     * Test 1.2.11 : Vérifier que getServerPing() retourne null avant ping
     */
    @Test
    public void testGetServerPingReturnsNullInitially() {
        assertNull(minecraftServer.getServerPing());
    }

    /**
     * Test 1.2.12 : Vérifier que les changements à la queue sont persistent
     */
    @Test
    public void testQueueChangesPersistent() {
        List<CommonPlayer> queue = minecraftServer.getQueue();
        CommonPlayer player = new MockCommonPlayer("Persistent");

        queue.add(player);

        List<CommonPlayer> queue2 = minecraftServer.getQueue();
        assertTrue(queue2.contains(player), "Le joueur doit être présent dans la deuxième référence");
    }

    /**
     * Test 1.2.13 : Vérifier que la queue accepte les doublons
     */
    @Test
    public void testQueueAcceptsDuplicates() {
        List<CommonPlayer> queue = minecraftServer.getQueue();
        CommonPlayer player = new MockCommonPlayer("Duplicate");

        queue.add(player);
        queue.add(player);

        assertEquals(2, queue.size(), "La queue doit contenir 2 occurrences");
        // Compter le nombre de fois que le joueur exactement appear
        int countOccurrences = 0;
        for (CommonPlayer p : queue) {
            if (p == player) { // Référence identique
                countOccurrences++;
            }
        }
        assertEquals(2, countOccurrences, "Le joueur doit apparaître 2 fois");
    }

    /**
     * Test 1.2.14 : Vérifier que les opérations sur la queue n'affectent pas le statut
     */
    @Test
    public void testQueueOperationsDontAffectStatus() {
        MinecraftServerStatus statusBefore = minecraftServer.getStatus();

        List<CommonPlayer> queue = minecraftServer.getQueue();
        queue.add(new MockCommonPlayer("Player1"));
        queue.add(new MockCommonPlayer("Player2"));
        queue.clear();

        MinecraftServerStatus statusAfter = minecraftServer.getStatus();

        assertEquals(statusBefore, statusAfter, "Le statut ne doit pas changer");
    }
}
