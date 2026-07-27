package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour PlayerDisconnectEvent.
 */
public class PlayerDisconnectEventTest {

    private PlayerDisconnectEvent event;
    private CommonPlayer player;

    @BeforeEach
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
        event = new PlayerDisconnectEvent(player);
    }

    /**
     * Test : Vérifier que PlayerDisconnectEvent contient le joueur
     */
    @Test
    public void testPlayerDisconnectEventConstructor() {
        assertNotNull(event, "L'événement ne doit pas être null");
        assertNotNull(event.getPlayer(), "Le joueur ne doit pas être null");
    }

    /**
     * Test : Vérifier que getPlayer() retourne le bon joueur
     */
    @Test
    public void testGetPlayer() {
        assertEquals(player, event.getPlayer(), "Le joueur doit être celui passé au constructeur");
    }

    /**
     * Test : Vérifier que le PlayerDisconnectEvent hérite de Event
     */
    @Test
    public void testPlayerDisconnectEventInheritance() {
        assertTrue(event instanceof Event, "PlayerDisconnectEvent doit être une instance de Event");
    }

    /**
     * Test : Vérifier que le joueur peut être null
     */
    @Test
    public void testPlayerDisconnectEventWithNullPlayer() {
        PlayerDisconnectEvent eventWithNull = new PlayerDisconnectEvent(null);
        assertNull(eventWithNull.getPlayer(), "Le joueur peut être null");
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
        
        assertNotEquals(event1.getPlayer().getUsername(), event2.getPlayer().getUsername(), "Les joueurs doivent être différents");
    }
}
