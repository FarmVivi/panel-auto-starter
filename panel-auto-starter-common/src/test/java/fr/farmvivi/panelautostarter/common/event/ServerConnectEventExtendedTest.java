package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests approfondis pour ServerConnectEvent.
 */
public class ServerConnectEventExtendedTest {

    private CommonPlayer player;
    private CommonServer server;
    private ServerConnectEvent event;

    @BeforeEach
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
        assertNotNull(event, "L'événement ne doit pas être null");
        assertNotNull(event.getPlayer(), "Le joueur ne doit pas être null");
        assertNotNull(event.getTarget(), "Le serveur cible ne doit pas être null");
    }

    /**
     * Test : Vérifier que le joueur est retourné correctement
     */
    @Test
    public void testGetPlayer() {
        CommonPlayer returnedPlayer = event.getPlayer();
        assertEquals(player, returnedPlayer, "Le joueur retourné doit être le même");
    }

    /**
     * Test : Vérifier que le serveur cible est retourné correctement
     */
    @Test
    public void testGetTarget() {
        CommonServer returnedServer = event.getTarget();
        assertEquals(server, returnedServer, "Le serveur retourné doit être le même");
    }

    /**
     * Test : Vérifier que setTarget() change le serveur cible
     */
    @Test
    public void testSetTarget() {
        CommonServer newServer = new MockCommonServer("newServer");
        event.setTarget(newServer);

        CommonServer returnedServer = event.getTarget();
        assertEquals(newServer, returnedServer, "Le serveur doit être le nouveau serveur");
        assertNotEquals(server, returnedServer, "Ne doit plus être le serveur original");
    }

    /**
     * Test : Vérifier que l'événement n'est pas annulé par défaut
     */
    @Test
    public void testNotCancelledByDefault() {
        assertFalse(event.isCancelled(), "L'événement ne doit pas être annulé par défaut");
    }

    /**
     * Test : Vérifier que l'événement peut être annulé
     */
    @Test
    public void testSetCancelled() {
        event.setCancelled(true);
        assertTrue(event.isCancelled(), "L'événement doit être annulé");
    }

    /**
     * Test : Vérifier que l'événement peut être réactivé après annulation
     */
    @Test
    public void testSetCancelledToFalse() {
        event.setCancelled(true);
        assertTrue(event.isCancelled(), "L'événement doit être annulé");

        event.setCancelled(false);
        assertFalse(event.isCancelled(), "L'événement ne doit pas être annulé");
    }

    /**
     * Test : Vérifier que l'événement hérite d'Event
     */
    @Test
    public void testEventInheritance() {
        assertTrue(event instanceof Event, "ServerConnectEvent doit être une instance d'Event");
    }

    /**
     * Test : Vérifier que l'événement implémente CancellableEvent
     */
    @Test
    public void testCancellableEventImplementation() {
        assertTrue(event instanceof CancellableEvent, "ServerConnectEvent doit implémenter CancellableEvent");
    }

    /**
     * Test : Vérifier postCall() de l'Event parent
     */
    @Test
    public void testPostCall() {
        // Ne doit pas lever d'exception
        event.postCall();
        assertNotNull(event, "L'événement ne doit pas être null après postCall()");
    }

    /**
     * Test : Vérifier avec un serveur cible null
     */
    @Test
    public void testWithNullTarget() {
        ServerConnectEvent nullTargetEvent = new ServerConnectEvent(player, null);
        assertNull(nullTargetEvent.getTarget(), "Le serveur cible peut être null");
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
        assertEquals(server1, event.getTarget(), "Doit être server1");

        event.setTarget(server2);
        assertEquals(server2, event.getTarget(), "Doit être server2");

        event.setTarget(server3);
        assertEquals(server3, event.getTarget(), "Doit être server3");
    }

    /**
     * Test : Vérifier que le statut d'annulation peut basculer plusieurs fois
     */
    @Test
    public void testCancelToggle() {
        assertFalse(event.isCancelled(), "Pas annulé au départ");

        event.setCancelled(true);
        assertTrue(event.isCancelled(), "Annulé après setCancelled(true)");

        event.setCancelled(false);
        assertFalse(event.isCancelled(), "Pas annulé après setCancelled(false)");

        event.setCancelled(true);
        assertTrue(event.isCancelled(), "Annulé de nouveau");
    }

    /**
     * Test : Vérifier avec un joueur null
     */
    @Test
    public void testWithNullPlayer() {
        ServerConnectEvent nullPlayerEvent = new ServerConnectEvent(null, server);
        assertNull(nullPlayerEvent.getPlayer(), "Le joueur peut être null");
    }

    /**
     * Test : Vérifier que setTarget() accepte null
     */
    @Test
    public void testSetTargetToNull() {
        event.setTarget(null);
        assertNull(event.getTarget(), "Le serveur cible peut être défini à null");
    }

    /**
     * Test : Vérifier que les deux joueurs et serveurs peuvent être null
     */
    @Test
    public void testBothNullParameters() {
        ServerConnectEvent bothNullEvent = new ServerConnectEvent(null, null);
        assertNull(bothNullEvent.getPlayer(), "Joueur peut être null");
        assertNull(bothNullEvent.getTarget(), "Serveur peut être null");
    }
}
