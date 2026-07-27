package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour PlayerDisconnectEvent.
 */
public class PlayerDisconnectEventTest {

    private PlayerDisconnectEvent event;
    private CommonPlayer player;

    @Before
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
        event = new PlayerDisconnectEvent(player);
    }

    /**
     * Test : Vérifier que PlayerDisconnectEvent contient le joueur
     */
    @Test
    public void testPlayerDisconnectEventConstructor() {
        assertNotNull("L'événement ne doit pas être null", event);
        assertNotNull("Le joueur ne doit pas être null", event.getPlayer());
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
     * Test : Vérifier que le PlayerDisconnectEvent hérite de Event
     */
    @Test
    public void testPlayerDisconnectEventInheritance() {
        assertTrue("PlayerDisconnectEvent doit être une instance de Event", 
            event instanceof Event);
    }

    /**
     * Test : Vérifier que le joueur peut être null
     */
    @Test
    public void testPlayerDisconnectEventWithNullPlayer() {
        PlayerDisconnectEvent eventWithNull = new PlayerDisconnectEvent(null);
        assertNull("Le joueur peut être null", eventWithNull.getPlayer());
    }

    /**
     * Test : Vérifier avec différents joueurs
     */
    @Test
    public void testPlayerDisconnectEventWithMultiplePlayers() {
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");
        
        PlayerDisconnectEvent event1 = new PlayerDisconnectEvent(player1);
        PlayerDisconnectEvent event2 = new PlayerDisconnectEvent(player2);
        
        assertNotEquals("Les joueurs doivent être différents", 
            event1.getPlayer().getUsername(), event2.getPlayer().getUsername());
    }
}
