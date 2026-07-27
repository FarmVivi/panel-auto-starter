package fr.farmvivi.panelautostarter.mocks;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour MockCommonPlayer.
 * Vérifie que les mocks fonctionnent correctement.
 */
public class MockCommonPlayerTest {

    private MockCommonPlayer player;

    @BeforeEach
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
    }

    @Test
    public void testPlayerInitialization() {
        assertNotNull(player.getUniqueId(), "Le UUID ne doit pas être null");
        assertEquals("TestPlayer", player.getUsername(), "Le nom doit être TestPlayer");
        assertEquals("TestPlayer", player.getDisplayName(), "Le display name doit être TestPlayer");
        assertTrue(player.isOnline(), "Le joueur doit être online par défaut");
    }

    @Test
    public void testPlayerWithUUID() {
        UUID testUuid = UUID.randomUUID();
        MockCommonPlayer customPlayer = new MockCommonPlayer("TestPlayer", testUuid);

        assertEquals(testUuid, customPlayer.getUniqueId(), "L'UUID doit correspondre");
    }

    @Test
    public void testPlayerWithDisplayName() {
        MockCommonPlayer customPlayer = new MockCommonPlayer("TestPlayer", UUID.randomUUID(), "Test Display");

        assertEquals("Test Display", customPlayer.getDisplayName(), "Le display name doit correspondre");
    }

    @Test
    public void testSendStringMessage() {
        String testMessage = "Hello Player!";
        player.sendMessage(testMessage);

        assertEquals(testMessage, player.getLastStringMessage(), "Le message string doit être enregistré");
    }

    @Test
    public void testSendComponentMessage() {
        Component testMessage = Component.text("Hello!");
        player.sendMessage(testMessage);

        assertEquals(testMessage, player.getLastMessage(), "Le message composant doit être enregistré");
    }

    @Test
    public void testConnectToServer() {
        MockCommonServer testServer = new MockCommonServer("test-server");
        player.connectToServer(testServer);

        assertEquals(testServer, player.getServer(), "Le joueur doit être connecté au serveur");
        assertEquals(1, player.getConnectToServerCallCount(), "connectToServerCallCount doit être 1");
    }

    @Test
    public void testMultipleConnections() {
        MockCommonServer server1 = new MockCommonServer("server1");
        MockCommonServer server2 = new MockCommonServer("server2");

        player.connectToServer(server1);
        player.connectToServer(server2);

        assertEquals(server2, player.getServer(), "Le joueur doit être sur le serveur 2");
        assertEquals(2, player.getConnectToServerCallCount(), "connectToServerCallCount doit être 2");
    }

    @Test
    public void testSetOnline() {
        assertTrue(player.isOnline(), "Le joueur doit être online");
        
        player.setOnline(false);
        assertFalse(player.isOnline(), "Le joueur doit être offline");
        
        player.setOnline(true);
        assertTrue(player.isOnline(), "Le joueur doit être online de nouveau");
    }

    @Test
    public void testSetCurrentServer() {
        assertNull(player.getServer(), "Le joueur ne doit pas avoir de serveur initialement");

        MockCommonServer testServer = new MockCommonServer("test-server");
        player.setCurrentServer(testServer);

        assertEquals(testServer, player.getServer(), "Le serveur doit être défini");
    }
}
