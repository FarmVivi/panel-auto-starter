package fr.farmvivi.panelautostarter.mocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour MockCommonServerPing.
 * Vérifie que les mocks de ping fonctionnent correctement.
 */
public class MockCommonServerPingTest {

    private MockCommonServerPing ping;

    @BeforeEach
    public void setUp() {
        ping = new MockCommonServerPing();
    }

    @Test
    public void testPingInitialization() {
        assertEquals(0, ping.getOnlinePlayers(), "Le nombre de joueurs en ligne doit être 0");
        assertEquals(20, ping.getMaxPlayers(), "Le nombre max de joueurs doit être 20");
        assertEquals(769, ping.getProtocolVersion(), "La version de protocole doit être 769");
    }

    @Test
    public void testPingWithCustomPlayerCount() {
        MockCommonServerPing customPing = new MockCommonServerPing(10, 30);

        assertEquals(10, customPing.getOnlinePlayers(), "Le nombre de joueurs en ligne doit être 10");
        assertEquals(30, customPing.getMaxPlayers(), "Le nombre max de joueurs doit être 30");
    }

    @Test
    public void testSetOnlinePlayers() {
        ping.setOnlinePlayers(15);
        assertEquals(15, ping.getOnlinePlayers(), "Le nombre de joueurs doit être 15");

        ping.setOnlinePlayers(0);
        assertEquals(0, ping.getOnlinePlayers(), "Le nombre de joueurs doit être 0");
    }

    @Test
    public void testSetMaxPlayers() {
        ping.setMaxPlayers(50);
        assertEquals(50, ping.getMaxPlayers(), "Le nombre max de joueurs doit être 50");
    }

    @Test
    public void testSetProtocolVersion() {
        ping.setProtocolVersion(760);
        assertEquals(760, ping.getProtocolVersion(), "La version de protocole doit être 760");
    }

    @Test
    public void testSamplePlayersInitialization() {
        assertNotNull(ping.getSamplePlayers(), "La liste des joueurs d'exemple ne doit pas être null");
        assertTrue(ping.getSamplePlayers().isEmpty(), "La liste des joueurs d'exemple doit être vide au démarrage");
    }

    @Test
    public void testServerIsFull() {
        MockCommonServerPing fullPing = new MockCommonServerPing(20, 20);
        assertEquals(20, fullPing.getOnlinePlayers(), "Le serveur doit être plein");
        assertEquals(20, fullPing.getMaxPlayers(), "Le nombre max doit être 20");
    }

    @Test
    public void testServerHasCapacity() {
        MockCommonServerPing availablePing = new MockCommonServerPing(5, 20);
        assertTrue(availablePing.getOnlinePlayers() < availablePing.getMaxPlayers(), "Le serveur doit avoir de la capacité");
    }

    @Test
    public void testDescriptionComponent() {
        assertNull(ping.getDescriptionComponent(), "La description doit être null au démarrage");
    }

    @Test
    public void testProtocolName() {
        assertNotNull(ping.getProtocolName(), "Le nom du protocole ne doit pas être null");
    }
}
