package fr.farmvivi.panelautostarter.mocks;

import net.kyori.adventure.text.Component;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour MockCommonPlayer.
 * Vérifie que les mocks fonctionnent correctement.
 */
public class MockCommonPlayerTest {

    private MockCommonPlayer player;

    @Before
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
    }

    @Test
    public void testPlayerInitialization() {
        assertNotNull("Le UUID ne doit pas être null", player.getUniqueId());
        assertEquals("Le nom doit être TestPlayer", "TestPlayer", player.getUsername());
        assertEquals("Le display name doit être TestPlayer", "TestPlayer", player.getDisplayName());
        assertTrue("Le joueur doit être online par défaut", player.isOnline());
    }

    @Test
    public void testPlayerWithUUID() {
        UUID testUuid = UUID.randomUUID();
        MockCommonPlayer customPlayer = new MockCommonPlayer("TestPlayer", testUuid);

        assertEquals("L'UUID doit correspondre", testUuid, customPlayer.getUniqueId());
    }

    @Test
    public void testPlayerWithDisplayName() {
        MockCommonPlayer customPlayer = new MockCommonPlayer("TestPlayer", UUID.randomUUID(), "Test Display");

        assertEquals("Le display name doit correspondre", "Test Display", customPlayer.getDisplayName());
    }

    @Test
    public void testSendStringMessage() {
        String testMessage = "Hello Player!";
        player.sendMessage(testMessage);

        assertEquals("Le message string doit être enregistré", testMessage, player.getLastStringMessage());
    }

    @Test
    public void testSendComponentMessage() {
        Component testMessage = Component.text("Hello!");
        player.sendMessage(testMessage);

        assertEquals("Le message composant doit être enregistré", testMessage, player.getLastMessage());
    }

    @Test
    public void testConnectToServer() {
        MockCommonServer testServer = new MockCommonServer("test-server");
        player.connectToServer(testServer);

        assertEquals("Le joueur doit être connecté au serveur", testServer, player.getServer());
        assertEquals("connectToServerCallCount doit être 1", 1, player.getConnectToServerCallCount());
    }

    @Test
    public void testMultipleConnections() {
        MockCommonServer server1 = new MockCommonServer("server1");
        MockCommonServer server2 = new MockCommonServer("server2");

        player.connectToServer(server1);
        player.connectToServer(server2);

        assertEquals("Le joueur doit être sur le serveur 2", server2, player.getServer());
        assertEquals("connectToServerCallCount doit être 2", 2, player.getConnectToServerCallCount());
    }

    @Test
    public void testSetOnline() {
        assertTrue("Le joueur doit être online", player.isOnline());
        
        player.setOnline(false);
        assertFalse("Le joueur doit être offline", player.isOnline());
        
        player.setOnline(true);
        assertTrue("Le joueur doit être online de nouveau", player.isOnline());
    }

    @Test
    public void testSetCurrentServer() {
        assertNull("Le joueur ne doit pas avoir de serveur initialement", player.getServer());

        MockCommonServer testServer = new MockCommonServer("test-server");
        player.setCurrentServer(testServer);

        assertEquals("Le serveur doit être défini", testServer, player.getServer());
    }
}
