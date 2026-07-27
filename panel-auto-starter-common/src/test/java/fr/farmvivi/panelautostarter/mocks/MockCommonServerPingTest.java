package fr.farmvivi.panelautostarter.mocks;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour MockCommonServerPing.
 * Vérifie que les mocks de ping fonctionnent correctement.
 */
public class MockCommonServerPingTest {

    private MockCommonServerPing ping;

    @Before
    public void setUp() {
        ping = new MockCommonServerPing();
    }

    @Test
    public void testPingInitialization() {
        assertEquals("Le nombre de joueurs en ligne doit être 0", 0, ping.getOnlinePlayers());
        assertEquals("Le nombre max de joueurs doit être 20", 20, ping.getMaxPlayers());
        assertEquals("La version de protocole doit être 769", 769, ping.getProtocolVersion());
    }

    @Test
    public void testPingWithCustomPlayerCount() {
        MockCommonServerPing customPing = new MockCommonServerPing(10, 30);

        assertEquals("Le nombre de joueurs en ligne doit être 10", 10, customPing.getOnlinePlayers());
        assertEquals("Le nombre max de joueurs doit être 30", 30, customPing.getMaxPlayers());
    }

    @Test
    public void testSetOnlinePlayers() {
        ping.setOnlinePlayers(15);
        assertEquals("Le nombre de joueurs doit être 15", 15, ping.getOnlinePlayers());

        ping.setOnlinePlayers(0);
        assertEquals("Le nombre de joueurs doit être 0", 0, ping.getOnlinePlayers());
    }

    @Test
    public void testSetMaxPlayers() {
        ping.setMaxPlayers(50);
        assertEquals("Le nombre max de joueurs doit être 50", 50, ping.getMaxPlayers());
    }

    @Test
    public void testSetProtocolVersion() {
        ping.setProtocolVersion(760);
        assertEquals("La version de protocole doit être 760", 760, ping.getProtocolVersion());
    }

    @Test
    public void testSamplePlayersInitialization() {
        assertNotNull("La liste des joueurs d'exemple ne doit pas être null", ping.getSamplePlayers());
        assertTrue("La liste des joueurs d'exemple doit être vide au démarrage", ping.getSamplePlayers().isEmpty());
    }

    @Test
    public void testServerIsFull() {
        MockCommonServerPing fullPing = new MockCommonServerPing(20, 20);
        assertEquals("Le serveur doit être plein", 20, fullPing.getOnlinePlayers());
        assertEquals("Le nombre max doit être 20", 20, fullPing.getMaxPlayers());
    }

    @Test
    public void testServerHasCapacity() {
        MockCommonServerPing availablePing = new MockCommonServerPing(5, 20);
        assertTrue("Le serveur doit avoir de la capacité",
                availablePing.getOnlinePlayers() < availablePing.getMaxPlayers());
    }

    @Test
    public void testDescriptionComponent() {
        assertNull("La description doit être null au démarrage", ping.getDescriptionComponent());
    }

    @Test
    public void testProtocolName() {
        assertNotNull("Le nom du protocole ne doit pas être null", ping.getProtocolName());
    }
}
