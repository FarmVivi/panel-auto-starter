package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.PteroAction;
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
 * Tests unitaires pour ServerConnectEventListener.
 */
public class ServerConnectEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private ServerConnectEvent mockEvent;

    @Mock
    private ClientServer mockClientServer;

    @Mock
    private PteroAction<Void> mockPteroAction;

    private ServerConnectEventListener listener;
    private CommonServer limboServer;
    private CommonServer targetServer;
    private CommonPlayer testPlayer;
    private Map<CommonServer, MinecraftServer> serversMap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        limboServer = new MockCommonServer("limbo");
        targetServer = new MockCommonServer("target-server");
        testPlayer = new MockCommonPlayer("TestPlayer");
        serversMap = new HashMap<>();

        // Configuration des mocks
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockConfiguration.getString("queue.server")).thenReturn("limbo");
        when(mockProxy.getServer("limbo")).thenReturn(limboServer);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        
        // Mock ClientServer
        when(mockClientServer.start()).thenReturn(mockPteroAction);
        when(mockClientServer.stop()).thenReturn(mockPteroAction);
        
        // Mock getServers() pour retourner une map mutable
        doReturn(serversMap).when(mockPlugin).getServers();

        listener = new ServerConnectEventListener(mockPlugin);
    }

    /**
     * Test : Vérifier que l'événement null est géré
     */
    @Test
    public void testNullPlayerHandling() {
        when(mockEvent.getPlayer()).thenReturn(null);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        verify(mockEvent).getPlayer();
        verify(mockEvent, never()).setTarget(any());
        verify(mockEvent, never()).setCancelled(anyBoolean());
    }

    /**
     * Test : Vérifier qu'un serveur inconnu est ignoré
     */
    @Test
    public void testUnknownServerIgnored() {
        when(mockEvent.getPlayer()).thenReturn(testPlayer);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        verify(mockEvent, never()).setTarget(any());
        verify(mockEvent, never()).setCancelled(anyBoolean());
    }

    /**
     * Test : Vérifier que le listener est initialisé
     */
    @Test
    public void testListenerInitialization() {
        assertNotNull("Le listener ne doit pas être null", listener);
    }

    /**
     * Test : Vérifier que le joueur est ajouté à la queue d'un serveur OFFLINE
     */
    @Test
    public void testPlayerAddedToQueueOfflineServer() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockClientServer);
        serversMap.put(targetServer, minecraftServer);

        CommonPlayer player = new MockCommonPlayer("QueuedPlayer");
        ((MockCommonPlayer) player).setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        assertEquals("Le serveur doit être OFFLINE", MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());

        listener.onServerConnect(mockEvent);

        assertTrue("Le joueur doit être dans la queue", minecraftServer.getQueue().contains(player));
    }

    /**
     * Test : Vérifier que le serveur démarre quand le joueur rejoint une queue
     */
    @Test
    public void testServerStartsWhenPlayerQueued() {
        MinecraftServer minecraftServer = mock(MinecraftServer.class);
        when(minecraftServer.getStatus()).thenReturn(MinecraftServerStatus.OFFLINE);
        when(minecraftServer.getQueue()).thenReturn(new java.util.LinkedList<>());
        when(minecraftServer.getServer()).thenReturn(targetServer);

        serversMap.put(targetServer, minecraftServer);

        CommonPlayer player = new MockCommonPlayer("QueuedPlayer");
        ((MockCommonPlayer) player).setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        verify(minecraftServer).start();
    }

    /**
     * Test : Vérifier que le joueur reçoit un message
     */
    @Test
    public void testPlayerReceivesMessage() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockClientServer);
        serversMap.put(targetServer, minecraftServer);

        CommonPlayer player = new MockCommonPlayer("MessagePlayer");
        ((MockCommonPlayer) player).setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        assertNotNull("Le joueur doit avoir reçu un message", 
            ((MockCommonPlayer) player).getLastMessage());
    }

    /**
     * Test : Vérifier que plusieurs joueurs peuvent être dans la queue
     */
    @Test
    public void testMultiplePlayersInQueue() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockClientServer);
        serversMap.put(targetServer, minecraftServer);

        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        ((MockCommonPlayer) player1).setCurrentServer(null);
        ((MockCommonPlayer) player2).setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player1);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        when(mockEvent.getPlayer()).thenReturn(player2);
        listener.onServerConnect(mockEvent);

        assertEquals("La queue doit avoir 2 joueurs", 2, minecraftServer.getQueue().size());
    }
}
