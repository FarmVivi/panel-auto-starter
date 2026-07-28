package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.queue.QueueCoordinator;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PlayerDisconnectEventListener.
 */
public class PlayerDisconnectEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    protected PendingNotifications pendingNotifications;

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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        pendingNotifications = new PendingNotifications();
        when(mockPlugin.getPendingNotifications()).thenReturn(pendingNotifications);

        testPlayer = new MockCommonPlayer("TestPlayer");
        serversMap = new HashMap<>();

        // Configuration des mocks
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockConfiguration.getLong("server-start.check-interval-normal", 15)).thenReturn(15L);
        when(mockConfiguration.getLong("server-start.check-interval-startup", 3)).thenReturn(3L);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        
        // Mock getServers() pour retourner une map mutable
        doReturn(serversMap).when(mockPlugin).getServers();

        // La surveillance arme le sondage suivant des la construction d'un
        // MinecraftServer, sans attendre la reponse du precedent.
        when(mockPlugin.getProxy()).thenReturn(mock(fr.farmvivi.panelautostarter.common.CommonProxy.class));

        when(mockPlugin.getQueueCoordinator()).thenReturn(new QueueCoordinator(mockPlugin));

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
     * Le retrait est délégué au serveur, qui compare les joueurs sur leur
     * identifiant : rien ne garantit qu'une même connexion soit toujours
     * représentée par la même instance.
     */
    @Test
    public void testPlayerIsRemovedFromEveryQueue() {
        when(mockEvent.getPlayer()).thenReturn(testPlayer);
        serversMap.put(new MockCommonServer("test-server"), mockMinecraftServer);

        listener.onPlayerDisconnect(mockEvent);

        verify(mockMinecraftServer).dequeue(testPlayer);
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

        assertEquals(0, mockMinecraftServer.getQueue().size(), "La queue doit rester vide");
    }

    /**
     * Test : Vérifier avec plusieurs serveurs
     */
    @Test
    public void testMultipleServers() {
        CommonServer server1 = new MockCommonServer("server1");
        CommonServer server2 = new MockCommonServer("server2");

        MinecraftServer minecraftServer1 = new MinecraftServer(mockPlugin, server1, null, mock(ServerMotd.class));
        MinecraftServer minecraftServer2 = new MinecraftServer(mockPlugin, server2, null, mock(ServerMotd.class));

        when(mockEvent.getPlayer()).thenReturn(testPlayer);

        serversMap.put(server1, minecraftServer1);
        serversMap.put(server2, minecraftServer2);

        minecraftServer1.getQueue().add(testPlayer);
        minecraftServer2.getQueue().add(testPlayer);

        listener.onPlayerDisconnect(mockEvent);

        assertFalse(minecraftServer1.getQueue().contains(testPlayer), "Le joueur ne doit pas être dans la queue du serveur 1");
        assertFalse(minecraftServer2.getQueue().contains(testPlayer), "Le joueur ne doit pas être dans la queue du serveur 2");
    }

    /**
     * Test : Vérifier que d'autres joueurs ne sont pas affectés
     */
    @Test
    public void testOtherPlayersNotRemoved() {
        CommonServer server = new MockCommonServer("test-server");
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, server, null, mock(ServerMotd.class));

        when(mockEvent.getPlayer()).thenReturn(player1);

        serversMap.put(server, minecraftServer);

        minecraftServer.getQueue().add(player1);
        minecraftServer.getQueue().add(player2);

        listener.onPlayerDisconnect(mockEvent);

        assertFalse(minecraftServer.getQueue().contains(player1), "Player1 doit être retiré");
        assertTrue(minecraftServer.getQueue().contains(player2), "Player2 doit rester");
    }

    /**
     * Test : Vérifier le constructeur
     */
    @Test
    public void testListenerInitialization() {
        assertNotNull(listener, "Le listener ne doit pas être null");
    }

    /**
     * Test : Vérifier avec une seule instance dans la queue
     */
    @Test
    public void testSinglePlayerInQueue() {
        CommonServer server = new MockCommonServer("test-server");
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, server, null, mock(ServerMotd.class));

        when(mockEvent.getPlayer()).thenReturn(testPlayer);
        serversMap.put(server, minecraftServer);

        minecraftServer.getQueue().add(testPlayer);
        assertEquals(1, minecraftServer.getQueue().size(), "La queue doit avoir 1 joueur");

        listener.onPlayerDisconnect(mockEvent);

        assertEquals(0, minecraftServer.getQueue().size(), "La queue doit être vide");
    }
}
