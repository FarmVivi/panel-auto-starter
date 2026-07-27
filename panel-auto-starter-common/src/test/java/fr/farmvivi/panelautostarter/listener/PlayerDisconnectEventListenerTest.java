package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import net.md_5.bungee.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PlayerDisconnectEventListener.
 */
public class PlayerDisconnectEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private PlayerDisconnectEvent mockEvent;

    @Mock
    private MinecraftServer mockMinecraftServer;

    private PlayerDisconnectEventListener listener;
    private CommonPlayer testPlayer;
    private Map<CommonServer, MinecraftServer> serversMap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        testPlayer = new MockCommonPlayer("TestPlayer");
        serversMap = new HashMap<>();

        // Configuration des mocks
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockConfiguration.getLong("server-start.check-interval-normal", 15)).thenReturn(15L);
        when(mockConfiguration.getLong("server-start.check-interval-startup", 3)).thenReturn(3L);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        
        // Mock getServers() pour retourner une map mutable
        doReturn(serversMap).when(mockPlugin).getServers();

        listener = new PlayerDisconnectEventListener(mockPlugin);
    }

    /**
     * Test : Vérifier que l'événement null est géré
     */
    @Test
    public void testNullPlayerHandling() {
        when(mockEvent.getPlayer()).thenReturn(null);

        listener.onPlayerDisconnect(mockEvent);

        // Aucune exception levée et le test passes
        assertTrue(true);
    }

    /**
     * Test : Vérifier que le joueur est retiré de la queue
     */
    @Test
    public void testPlayerRemovedFromQueue() {
        CommonServer server = new MockCommonServer("test-server");
        when(mockEvent.getPlayer()).thenReturn(testPlayer);
        when(mockMinecraftServer.getQueue()).thenReturn(new java.util.LinkedList<>());

        serversMap.put(server, mockMinecraftServer);

        mockMinecraftServer.getQueue().add(testPlayer);
        listener.onPlayerDisconnect(mockEvent);

        assertFalse("Le joueur ne doit pas être dans la queue", 
            mockMinecraftServer.getQueue().contains(testPlayer));
    }

    /**
     * Test : Vérifier le comportement avec une queue vide
     */
    @Test
    public void testEmptyQueueHandling() {
        CommonServer server = new MockCommonServer("test-server");
        when(mockEvent.getPlayer()).thenReturn(testPlayer);
        when(mockMinecraftServer.getQueue()).thenReturn(new java.util.LinkedList<>());

        serversMap.put(server, mockMinecraftServer);

        listener.onPlayerDisconnect(mockEvent);

        assertEquals("La queue doit rester vide", 0, mockMinecraftServer.getQueue().size());
    }

    /**
     * Test : Vérifier avec plusieurs serveurs
     */
    @Test
    public void testMultipleServers() {
        CommonServer server1 = new MockCommonServer("server1");
        CommonServer server2 = new MockCommonServer("server2");

        MinecraftServer minecraftServer1 = new MinecraftServer(mockPlugin, server1, null);
        MinecraftServer minecraftServer2 = new MinecraftServer(mockPlugin, server2, null);

        when(mockEvent.getPlayer()).thenReturn(testPlayer);

        serversMap.put(server1, minecraftServer1);
        serversMap.put(server2, minecraftServer2);

        minecraftServer1.getQueue().add(testPlayer);
        minecraftServer2.getQueue().add(testPlayer);

        listener.onPlayerDisconnect(mockEvent);

        assertFalse("Le joueur ne doit pas être dans la queue du serveur 1", 
            minecraftServer1.getQueue().contains(testPlayer));
        assertFalse("Le joueur ne doit pas être dans la queue du serveur 2", 
            minecraftServer2.getQueue().contains(testPlayer));
    }

    /**
     * Test : Vérifier que d'autres joueurs ne sont pas affectés
     */
    @Test
    public void testOtherPlayersNotRemoved() {
        CommonServer server = new MockCommonServer("test-server");
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, server, null);

        when(mockEvent.getPlayer()).thenReturn(player1);

        serversMap.put(server, minecraftServer);

        minecraftServer.getQueue().add(player1);
        minecraftServer.getQueue().add(player2);

        listener.onPlayerDisconnect(mockEvent);

        assertFalse("Player1 doit être retiré", minecraftServer.getQueue().contains(player1));
        assertTrue("Player2 doit rester", minecraftServer.getQueue().contains(player2));
    }

    /**
     * Test : Vérifier le constructeur
     */
    @Test
    public void testListenerInitialization() {
        assertNotNull("Le listener ne doit pas être null", listener);
    }

    /**
     * Test : Vérifier avec une seule instance dans la queue
     */
    @Test
    public void testSinglePlayerInQueue() {
        CommonServer server = new MockCommonServer("test-server");
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, server, null);

        when(mockEvent.getPlayer()).thenReturn(testPlayer);
        serversMap.put(server, minecraftServer);

        minecraftServer.getQueue().add(testPlayer);
        assertEquals("La queue doit avoir 1 joueur", 1, minecraftServer.getQueue().size());

        listener.onPlayerDisconnect(mockEvent);

        assertEquals("La queue doit être vide", 0, minecraftServer.getQueue().size());
    }
}
