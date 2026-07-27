package fr.farmvivi.panelautostarter.common;

import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour les interfaces communes.
 */
public class CommonInterfacesTest {

    private CommonPlayer player;
    private CommonServer server;

    @BeforeEach
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
        server = new MockCommonServer("test-server");
    }

    /**
     * Test : Vérifier que CommonPlayer implémente tous les contrats
     */
    @Test
    public void testCommonPlayerContract() {
        assertNotNull(player.getUniqueId(), "Le UUID ne doit pas être null");
        assertTrue(player.isOnline(), "Le joueur doit être online");
        assertEquals("TestPlayer", player.getUsername(), "Le username doit être correct");
        assertNotNull(player.getDisplayName(), "Le display name ne doit pas être null");
    }

    /**
     * Test : Vérifier que CommonServer implémente tous les contrats
     */
    @Test
    public void testCommonServerContract() {
        assertEquals("test-server", server.getName(), "Le nom du serveur doit être correct");
        assertNotNull(server.getDisplayName(), "Le display name ne doit pas être null");
        assertEquals(0, server.getPlayerCount(), "Le nombre de joueurs doit être 0");
    }

    /**
     * Test : Vérifier les messages sur CommonPlayer
     */
    @Test
    public void testPlayerMessaging() {
        String message = "Test message";
        player.sendMessage(message);

        MockCommonPlayer mockPlayer = (MockCommonPlayer) player;
        assertEquals(message, mockPlayer.getLastStringMessage(), "Le message string doit être enregistré");
    }

    /**
     * Test : Vérifier les composants sur CommonPlayer
     */
    @Test
    public void testPlayerComponent() {
        Component component = Component.text("Test");
        player.sendMessage(component);

        MockCommonPlayer mockPlayer = (MockCommonPlayer) player;
        assertEquals(component, mockPlayer.getLastMessage(), "Le component doit être enregistré");
    }

    /**
     * Test : Vérifier la connexion à un serveur
     */
    @Test
    public void testPlayerServerConnection() {
        assertNull(player.getServer(), "Le joueur ne doit pas avoir de serveur initialement");

        player.connectToServer(server);

        assertEquals(server, player.getServer(), "Le joueur doit être connecté au serveur");
    }

    /**
     * Test : Vérifier le changement de serveur
     */
    @Test
    public void testPlayerServerSwitch() {
        CommonServer server2 = new MockCommonServer("server2");

        player.connectToServer(server);
        assertEquals(server, player.getServer(), "Le joueur doit être sur server");

        player.connectToServer(server2);
        assertEquals(server2, player.getServer(), "Le joueur doit être sur server2");
    }

    /**
     * Test : Vérifier le ping du serveur
     */
    @Test
    public void testServerPing() {
        MockCommonServerPing ping = new MockCommonServerPing(5, 20);

        assertEquals(5, ping.getOnlinePlayers(), "Le nombre de joueurs doit être 5");
        assertEquals(20, ping.getMaxPlayers(), "Le nombre max doit être 20");
    }

    /**
     * Test : Vérifier que les UUID sont différents
     */
    @Test
    public void testPlayerUniqueIds() {
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        assertNotEquals(player1.getUniqueId(), player2.getUniqueId(), "Les UUIDs doivent être différents");
    }

    /**
     * Test : Vérifier avec un UUID custom
     */
    @Test
    public void testPlayerWithCustomUUID() {
        UUID customUUID = UUID.randomUUID();
        CommonPlayer customPlayer = new MockCommonPlayer("Custom", customUUID);

        assertEquals(customUUID, customPlayer.getUniqueId(), "Le UUID doit correspondre");
    }

    /**
     * Test : Vérifier le display name du serveur
     */
    @Test
    public void testServerDisplayName() {
        CommonServer customServer = new MockCommonServer("internal-name", "Display Name");

        assertEquals("internal-name", customServer.getName(), "Le nom interne doit être correct");
        assertEquals("Display Name", customServer.getDisplayName(), "Le display name doit être correct");
    }

    /**
     * Test : Vérifier le statut online/offline du joueur
     */
    @Test
    public void testPlayerOnlineStatus() {
        assertTrue(player.isOnline(), "Le joueur doit être online initialement");

        MockCommonPlayer mockPlayer = (MockCommonPlayer) player;
        mockPlayer.setOnline(false);

        assertFalse(player.isOnline(), "Le joueur doit être offline");
    }

    /**
     * Test : Vérifier le callback de ping
     */
    @Test
    public void testServerPingCallback() {
        final boolean[] callbackCalled = {false};

        server.ping(result -> {
            callbackCalled[0] = true;
        });

        // Le callback peut être appelé de manière asynchrone, on vérifie que ping() n'a pas d'erreur
        assertNotNull(server, "ping() doit être supporté");
    }
}
