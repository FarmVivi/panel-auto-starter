package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ServerConnectEvent.
 */
public class ServerConnectEventTest {

    private ServerConnectEvent event;
    private CommonPlayer player;
    private CommonServer server;

    @BeforeEach
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
        server = new MockCommonServer("test-server");
        event = new ServerConnectEvent(player, server);
    }

    /**
     * Test : Vérifier que ServerConnectEvent contient le joueur et le serveur
     */
    @Test
    public void testServerConnectEventConstructor() {
        assertNotNull(event, "L'événement ne doit pas être null");
        assertNotNull(event.getPlayer(), "Le joueur ne doit pas être null");
        assertNotNull(event.getTarget(), "Le serveur ne doit pas être null");
    }

    /**
     * Test : Vérifier que getPlayer() retourne le bon joueur
     */
    @Test
    public void testGetPlayer() {
        assertEquals(player, event.getPlayer(), "Le joueur doit être celui passé au constructeur");
    }

    /**
     * Test : Vérifier que getTarget() retourne le bon serveur
     */
    @Test
    public void testGetTarget() {
        assertEquals(server, event.getTarget(), "Le serveur doit être celui passé au constructeur");
    }

    /**
     * Test : Vérifier que le ServerConnectEvent hérite de Event
     */
    @Test
    public void testServerConnectEventInheritance() {
        assertTrue(event instanceof Event, "ServerConnectEvent doit être une instance de Event");
    }

    /**
     * Test : Vérifier que le joueur peut être null
     */
    @Test
    public void testServerConnectEventWithNullPlayer() {
        ServerConnectEvent eventWithNull = new ServerConnectEvent(null, server);
        assertNull(eventWithNull.getPlayer(), "Le joueur peut être null");
        assertEquals(server, eventWithNull.getTarget(), "Le serveur ne doit pas être null");
    }

    /**
     * Test : Vérifier que le serveur peut être null
     */
    @Test
    public void testServerConnectEventWithNullServer() {
        ServerConnectEvent eventWithNull = new ServerConnectEvent(player, null);
        assertEquals(player, eventWithNull.getPlayer(), "Le joueur ne doit pas être null");
        assertNull(eventWithNull.getTarget(), "Le serveur peut être null");
    }

    /**
     * Test : Vérifier avec différents joueurs et serveurs
     */
    @Test
    public void testServerConnectEventWithMultiplePlayersAndServers() {
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");
        CommonServer server1 = new MockCommonServer("server1");
        CommonServer server2 = new MockCommonServer("server2");
        
        ServerConnectEvent event1 = new ServerConnectEvent(player1, server1);
        ServerConnectEvent event2 = new ServerConnectEvent(player2, server2);
        
        assertNotEquals(event1.getPlayer().getUsername(), event2.getPlayer().getUsername(), "Les joueurs doivent être différents");
        assertNotEquals(event1.getTarget().getDisplayName(), event2.getTarget().getDisplayName(), "Les serveurs doivent être différents");
    }
}
