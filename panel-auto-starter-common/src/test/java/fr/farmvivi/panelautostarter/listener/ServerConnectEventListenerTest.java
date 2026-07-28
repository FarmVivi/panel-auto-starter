package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.panel.PanelServer;
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
 * Tests unitaires pour ServerConnectEventListener.
 */
public class ServerConnectEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    protected PendingNotifications pendingNotifications;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private ServerConnectEvent mockEvent;

    @Mock
    private PanelServer mockPanelServer;

    private ServerConnectEventListener listener;
    private CommonServer limboServer;
    private CommonServer targetServer;
    private CommonPlayer testPlayer;
    private Map<CommonServer, MinecraftServer> serversMap;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());
        pendingNotifications = new PendingNotifications();
        when(mockPlugin.getPendingNotifications()).thenReturn(pendingNotifications);

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
        assertNotNull(listener, "Le listener ne doit pas être null");
    }

    /**
     * Test : Vérifier que le joueur est ajouté à la queue d'un serveur OFFLINE
     */
    @Test
    public void testPlayerAddedToQueueOfflineServer() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockPanelServer, mock(ServerMotd.class));
        serversMap.put(targetServer, minecraftServer);

        CommonPlayer player = new MockCommonPlayer("QueuedPlayer");
        ((MockCommonPlayer) player).setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(), "Le serveur doit être OFFLINE");

        listener.onServerConnect(mockEvent);

        assertTrue(minecraftServer.getQueue().contains(player), "Le joueur doit être dans la queue");
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
     * Un joueur déjà en jeu qui tente de rejoindre un serveur éteint peut
     * recevoir le message immédiatement : sa connexion est établie.
     */
    @Test
    public void testAlreadyConnectedPlayerIsToldImmediately() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockPanelServer, mock(ServerMotd.class));
        serversMap.put(targetServer, minecraftServer);

        MockCommonPlayer player = new MockCommonPlayer("MessagePlayer");
        player.setCurrentServer(limboServer);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        assertNotNull(player.getLastMessage(), "Le joueur doit avoir recu le message tout de suite");
        assertEquals(0, pendingNotifications.size(), "Rien ne doit etre mis en attente");
    }

    /**
     * Le bug corrigé : un joueur qui rejoint le proxy directement sur un
     * serveur éteint n'a pas encore atteint l'état de jeu. Lui écrire à cet
     * instant ne produit rien — le message doit être différé jusqu'à son
     * arrivée effective sur le limbo.
     */
    @Test
    public void testJoiningPlayerHasHisMessageDeferredUntilHeLands() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockPanelServer, mock(ServerMotd.class));
        serversMap.put(targetServer, minecraftServer);

        MockCommonPlayer player = new MockCommonPlayer("JoiningPlayer");
        player.setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        assertNull(player.getLastMessage(),
                "Rien ne doit etre envoye tant que le joueur n'est pas en jeu");
        assertEquals(1, pendingNotifications.size(), "Le message doit etre mis de cote");

        // Le joueur atterrit sur le limbo.
        new ServerConnectedEventListener(pendingNotifications)
                .onServerConnected(new ServerConnectedEvent(player, limboServer));

        assertNotNull(player.getLastMessage(),
                "Le message doit etre delivre une fois le joueur arrive");
        assertEquals(0, pendingNotifications.size());
    }

    /**
     * Test : Vérifier que plusieurs joueurs peuvent être dans la queue
     */
    @Test
    public void testMultiplePlayersInQueue() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockPanelServer, mock(ServerMotd.class));
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

        assertEquals(2, minecraftServer.getQueue().size(), "La queue doit avoir 2 joueurs");
    }

    /**
     * Le titre souffrait du meme mal que le message de chat : envoye avant que
     * la connexion n'atteigne l'etat de jeu, il n'etait jamais affiche. Il doit
     * donc etre differe lui aussi.
     */
    @Test
    public void testJoiningPlayerHasHisTitleDeferredUntilHeLands() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockPanelServer, mock(ServerMotd.class));
        serversMap.put(targetServer, minecraftServer);

        MockCommonPlayer player = new MockCommonPlayer("JoiningPlayer");
        player.setCurrentServer(null);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        assertNull(player.getLastTitle(),
                "Le titre ne doit pas partir tant que le joueur n'est pas en jeu");

        new ServerConnectedEventListener(pendingNotifications)
                .onServerConnected(new ServerConnectedEvent(player, limboServer));

        assertNotNull(player.getLastTitle(),
                "Le titre doit etre affiche une fois le joueur arrive");
        assertNotNull(player.getLastMessage(), "Le message aussi");
    }

    @Test
    public void testAlreadyConnectedPlayerSeesHisTitleImmediately() {
        MinecraftServer minecraftServer = new MinecraftServer(mockPlugin, targetServer, mockPanelServer, mock(ServerMotd.class));
        serversMap.put(targetServer, minecraftServer);

        MockCommonPlayer player = new MockCommonPlayer("ConnectedPlayer");
        player.setCurrentServer(limboServer);

        when(mockEvent.getPlayer()).thenReturn(player);
        when(mockEvent.getTarget()).thenReturn(targetServer);

        listener.onServerConnect(mockEvent);

        assertNotNull(player.getLastTitle(), "Sa connexion est etablie : le titre part tout de suite");
        assertEquals(0, pendingNotifications.size());
    }
}
