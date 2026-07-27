package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests approfondis pour ServerConnectEvent.
 */
public class ServerConnectEventExtendedTest {

    private CommonPlayer player;
    private CommonServer server;
    private ServerConnectEvent event;

    @Before
    public void setUp() {
        player = new MockCommonPlayer("testPlayer");
        server = new MockCommonServer("testServer");
        event = new ServerConnectEvent(player, server);
    }

    /**
     * Test : Vérifier que l'événement est construit correctement
     */
    @Test
    public void testEventConstruction() {
        assertNotNull("L'événement ne doit pas être null", event);
        assertNotNull("Le joueur ne doit pas être null", event.getPlayer());
        assertNotNull("Le serveur cible ne doit pas être null", event.getTarget());
    }

    /**
     * Test : Vérifier que le joueur est retourné correctement
     */
    @Test
    public void testGetPlayer() {
        CommonPlayer returnedPlayer = event.getPlayer();
        assertEquals("Le joueur retourné doit être le même", player, returnedPlayer);
    }

    /**
     * Test : Vérifier que le serveur cible est retourné correctement
     */
    @Test
    public void testGetTarget() {
        CommonServer returnedServer = event.getTarget();
        assertEquals("Le serveur retourné doit être le même", server, returnedServer);
    }

    /**
     * Test : Vérifier que setTarget() change le serveur cible
     */
    @Test
    public void testSetTarget() {
        CommonServer newServer = new MockCommonServer("newServer");
        event.setTarget(newServer);

        CommonServer returnedServer = event.getTarget();
        assertEquals("Le serveur doit être le nouveau serveur", newServer, returnedServer);
        assertNotEquals("Ne doit plus être le serveur original", server, returnedServer);
    }

    /**
     * Test : Vérifier que l'événement n'est pas annulé par défaut
     */
    @Test
    public void testNotCancelledByDefault() {
        assertFalse("L'événement ne doit pas être annulé par défaut", event.isCancelled());
    }

    /**
     * Test : Vérifier que l'événement peut être annulé
     */
    @Test
    public void testSetCancelled() {
        event.setCancelled(true);
        assertTrue("L'événement doit être annulé", event.isCancelled());
    }

    /**
     * Test : Vérifier que l'événement peut être réactivé après annulation
     */
    @Test
    public void testSetCancelledToFalse() {
        event.setCancelled(true);
        assertTrue("L'événement doit être annulé", event.isCancelled());

        event.setCancelled(false);
        assertFalse("L'événement ne doit pas être annulé", event.isCancelled());
    }

    /**
     * Test : Vérifier que l'événement hérite d'Event
     */
    @Test
    public void testEventInheritance() {
        assertTrue("ServerConnectEvent doit être une instance d'Event", event instanceof Event);
    }

    /**
     * Test : Vérifier que l'événement implémente CancellableEvent
     */
    @Test
    public void testCancellableEventImplementation() {
        assertTrue("ServerConnectEvent doit implémenter CancellableEvent", event instanceof CancellableEvent);
    }

    /**
     * Test : Vérifier postCall() de l'Event parent
     */
    @Test
    public void testPostCall() {
        // Ne doit pas lever d'exception
        event.postCall();
        assertNotNull("L'événement ne doit pas être null après postCall()", event);
    }

    /**
     * Test : Vérifier avec un serveur cible null
     */
    @Test
    public void testWithNullTarget() {
        ServerConnectEvent nullTargetEvent = new ServerConnectEvent(player, null);
        assertNull("Le serveur cible peut être null", nullTargetEvent.getTarget());
    }

    /**
     * Test : Vérifier la réassignation du serveur multiple fois
     */
    @Test
    public void testMultipleTargetChanges() {
        CommonServer server1 = new MockCommonServer("server1");
        CommonServer server2 = new MockCommonServer("server2");
        CommonServer server3 = new MockCommonServer("server3");

        event.setTarget(server1);
        assertEquals("Doit être server1", server1, event.getTarget());

        event.setTarget(server2);
        assertEquals("Doit être server2", server2, event.getTarget());

        event.setTarget(server3);
        assertEquals("Doit être server3", server3, event.getTarget());
    }

    /**
     * Test : Vérifier que le statut d'annulation peut basculer plusieurs fois
     */
    @Test
    public void testCancelToggle() {
        assertFalse("Pas annulé au départ", event.isCancelled());

        event.setCancelled(true);
        assertTrue("Annulé après setCancelled(true)", event.isCancelled());

        event.setCancelled(false);
        assertFalse("Pas annulé après setCancelled(false)", event.isCancelled());

        event.setCancelled(true);
        assertTrue("Annulé de nouveau", event.isCancelled());
    }

    /**
     * Test : Vérifier avec un joueur null
     */
    @Test
    public void testWithNullPlayer() {
        ServerConnectEvent nullPlayerEvent = new ServerConnectEvent(null, server);
        assertNull("Le joueur peut être null", nullPlayerEvent.getPlayer());
    }

    /**
     * Test : Vérifier que setTarget() accepte null
     */
    @Test
    public void testSetTargetToNull() {
        event.setTarget(null);
        assertNull("Le serveur cible peut être défini à null", event.getTarget());
    }

    /**
     * Test : Vérifier que les deux joueurs et serveurs peuvent être null
     */
    @Test
    public void testBothNullParameters() {
        ServerConnectEvent bothNullEvent = new ServerConnectEvent(null, null);
        assertNull("Joueur peut être null", bothNullEvent.getPlayer());
        assertNull("Serveur peut être null", bothNullEvent.getTarget());
    }
}
