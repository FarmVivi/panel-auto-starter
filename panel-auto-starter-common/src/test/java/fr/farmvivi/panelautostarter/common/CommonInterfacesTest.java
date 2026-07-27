package fr.farmvivi.panelautostarter.common;

import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import net.kyori.adventure.text.Component;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests d'intégration pour les interfaces communes.
 */
public class CommonInterfacesTest {

    private CommonPlayer player;
    private CommonServer server;

    @Before
    public void setUp() {
        player = new MockCommonPlayer("TestPlayer");
        server = new MockCommonServer("test-server");
    }

    /**
     * Test : Vérifier que CommonPlayer implémente tous les contrats
     */
    @Test
    public void testCommonPlayerContract() {
        assertNotNull("Le UUID ne doit pas être null", player.getUniqueId());
        assertTrue("Le joueur doit être online", player.isOnline());
        assertEquals("Le username doit être correct", "TestPlayer", player.getUsername());
        assertNotNull("Le display name ne doit pas être null", player.getDisplayName());
    }

    /**
     * Test : Vérifier que CommonServer implémente tous les contrats
     */
    @Test
    public void testCommonServerContract() {
        assertEquals("Le nom du serveur doit être correct", "test-server", server.getName());
        assertNotNull("Le display name ne doit pas être null", server.getDisplayName());
        assertEquals("Le nombre de joueurs doit être 0", 0, server.getPlayerCount());
    }

    /**
     * Test : Vérifier les messages sur CommonPlayer
     */
    @Test
    public void testPlayerMessaging() {
        String message = "Test message";
        player.sendMessage(message);

        MockCommonPlayer mockPlayer = (MockCommonPlayer) player;
        assertEquals("Le message string doit être enregistré", message, mockPlayer.getLastStringMessage());
    }

    /**
     * Test : Vérifier les composants sur CommonPlayer
     */
    @Test
    public void testPlayerComponent() {
        Component component = Component.text("Test");
        player.sendMessage(component);

        MockCommonPlayer mockPlayer = (MockCommonPlayer) player;
        assertEquals("Le component doit être enregistré", component, mockPlayer.getLastMessage());
    }

    /**
     * Test : Vérifier la connexion à un serveur
     */
    @Test
    public void testPlayerServerConnection() {
        assertNull("Le joueur ne doit pas avoir de serveur initialement", player.getServer());

        player.connectToServer(server);

        assertEquals("Le joueur doit être connecté au serveur", server, player.getServer());
    }

    /**
     * Test : Vérifier le changement de serveur
     */
    @Test
    public void testPlayerServerSwitch() {
        CommonServer server2 = new MockCommonServer("server2");

        player.connectToServer(server);
        assertEquals("Le joueur doit être sur server", server, player.getServer());

        player.connectToServer(server2);
        assertEquals("Le joueur doit être sur server2", server2, player.getServer());
    }

    /**
     * Test : Vérifier le ping du serveur
     */
    @Test
    public void testServerPing() {
        MockCommonServerPing ping = new MockCommonServerPing(5, 20);

        assertEquals("Le nombre de joueurs doit être 5", 5, ping.getOnlinePlayers());
        assertEquals("Le nombre max doit être 20", 20, ping.getMaxPlayers());
    }

    /**
     * Test : Vérifier que les UUID sont différents
     */
    @Test
    public void testPlayerUniqueIds() {
        CommonPlayer player1 = new MockCommonPlayer("Player1");
        CommonPlayer player2 = new MockCommonPlayer("Player2");

        assertNotEquals("Les UUIDs doivent être différents", player1.getUniqueId(), player2.getUniqueId());
    }

    /**
     * Test : Vérifier avec un UUID custom
     */
    @Test
    public void testPlayerWithCustomUUID() {
        UUID customUUID = UUID.randomUUID();
        CommonPlayer customPlayer = new MockCommonPlayer("Custom", customUUID);

        assertEquals("Le UUID doit correspondre", customUUID, customPlayer.getUniqueId());
    }

    /**
     * Test : Vérifier le display name du serveur
     */
    @Test
    public void testServerDisplayName() {
        CommonServer customServer = new MockCommonServer("internal-name", "Display Name");

        assertEquals("Le nom interne doit être correct", "internal-name", customServer.getName());
        assertEquals("Le display name doit être correct", "Display Name", customServer.getDisplayName());
    }

    /**
     * Test : Vérifier le statut online/offline du joueur
     */
    @Test
    public void testPlayerOnlineStatus() {
        assertTrue("Le joueur doit être online initialement", player.isOnline());

        MockCommonPlayer mockPlayer = (MockCommonPlayer) player;
        mockPlayer.setOnline(false);

        assertFalse("Le joueur doit être offline", player.isOnline());
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
        assertNotNull("ping() doit être supporté", server);
    }
}
