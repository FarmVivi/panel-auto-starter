package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour ServerConnectEvent.
 */
public class ServerConnectEventTest {

    private ServerConnectEvent event;
    private CommonPlayer player;
    private CommonServer server;

    @Before
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
        assertNotNull("L'événement ne doit pas être null", event);
        assertNotNull("Le joueur ne doit pas être null", event.getPlayer());
        assertNotNull("Le serveur ne doit pas être null", event.getTarget());
    }

    /**
     * Test : Vérifier que getPlayer() retourne le bon joueur
     */
    @Test
    public void testGetPlayer() {
        assertEquals("Le joueur doit être celui passé au constructeur", 
            player, event.getPlayer());
    }

    /**
     * Test : Vérifier que getTarget() retourne le bon serveur
     */
    @Test
    public void testGetTarget() {
        assertEquals("Le serveur doit être celui passé au constructeur", 
            server, event.getTarget());
    }

    /**
     * Test : Vérifier que le ServerConnectEvent hérite de Event
     */
    @Test
    public void testServerConnectEventInheritance() {
        assertTrue("ServerConnectEvent doit être une instance de Event", 
            event instanceof Event);
    }

    /**
     * Test : Vérifier que le joueur peut être null
     */
    @Test
    public void testServerConnectEventWithNullPlayer() {
        ServerConnectEvent eventWithNull = new ServerConnectEvent(null, server);
        assertNull("Le joueur peut être null", eventWithNull.getPlayer());
        assertEquals("Le serveur ne doit pas être null", server, eventWithNull.getTarget());
    }

    /**
     * Test : Vérifier que le serveur peut être null
     */
    @Test
    public void testServerConnectEventWithNullServer() {
        ServerConnectEvent eventWithNull = new ServerConnectEvent(player, null);
        assertEquals("Le joueur ne doit pas être null", player, eventWithNull.getPlayer());
        assertNull("Le serveur peut être null", eventWithNull.getTarget());
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
        
        assertNotEquals("Les joueurs doivent être différents", 
            event1.getPlayer().getUsername(), event2.getPlayer().getUsername());
        assertNotEquals("Les serveurs doivent être différents", 
            event1.getTarget().getDisplayName(), event2.getTarget().getDisplayName());
    }
}
