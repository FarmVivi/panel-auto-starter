package fr.farmvivi.panelautostarter.mocks;

import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour MockCommonServer.
 * Vérifie que les mocks fonctionnent correctement.
 */
public class MockCommonServerTest {

    private MockCommonServer server;

    @BeforeEach
    public void setUp() {
        server = new MockCommonServer("test-server", "Test Server");
    }

    @Test
    public void testServerInitialization() {
        assertEquals("test-server", server.getName(), "Le nom doit être test-server");
        assertEquals("Test Server", server.getDisplayName(), "Le display name doit être Test Server");
        assertEquals(0, server.getPlayerCount(), "Le nombre de joueurs doit être 0");
    }

    @Test
    public void testServerWithoutDisplayName() {
        MockCommonServer simpleServer = new MockCommonServer("simple");

        assertEquals("simple", simpleServer.getName(), "Le nom doit être simple");
        assertEquals("simple", simpleServer.getDisplayName(), "Le display name doit être simple");
    }

    @Test
    public void testSetPlayerCount() {
        server.setPlayerCount(5);
        assertEquals(5, server.getPlayerCount(), "Le nombre de joueurs doit être 5");

        server.setPlayerCount(0);
        assertEquals(0, server.getPlayerCount(), "Le nombre de joueurs doit être 0");
    }

    @Test
    public void testPingCallbackRecorded() {
        final boolean[] callbackCalled = {false};

        Callback<CommonServerPing> testCallback = result -> {
            callbackCalled[0] = true;
        };

        server.ping(testCallback);

        assertEquals(testCallback, server.getLastPingCallback(), "Le callback doit être enregistré");
        assertEquals(1, server.getPingCallCount(), "Le ping call count doit être 1");
    }

    @Test
    public void testMultiplePingCalls() {
        Callback<CommonServerPing> callback1 = result -> {
        };
        Callback<CommonServerPing> callback2 = result -> {
        };

        server.ping(callback1);
        server.ping(callback2);

        assertEquals(callback2, server.getLastPingCallback(), "Le dernier callback doit être callback2");
        assertEquals(2, server.getPingCallCount(), "Le ping call count doit être 2");
    }

    @Test
    public void testSimulatePingResponse() {
        final boolean[] callbackCalled = {false};
        final CommonServerPing[] resultCapture = {null};

        Callback<CommonServerPing> testCallback = result -> {
            callbackCalled[0] = true;
            resultCapture[0] = result;
        };

        server.ping(testCallback);
        
        MockCommonServerPing ping = new MockCommonServerPing(5, 20);
        server.simulatePingResponse(ping);

        assertTrue(callbackCalled[0], "Le callback doit avoir été appelé");
        assertEquals(ping, resultCapture[0], "Le résultat doit être le ping fourni");
    }

    @Test
    public void testSimulatePingTimeout() {
        final boolean[] callbackCalled = {false};
        final CommonServerPing[] resultCapture = {new MockCommonServerPing()};

        Callback<CommonServerPing> testCallback = result -> {
            callbackCalled[0] = true;
            resultCapture[0] = result;
        };

        server.ping(testCallback);
        server.simulatePingTimeout();

        assertTrue(callbackCalled[0], "Le callback doit avoir été appelé");
        assertNull(resultCapture[0], "Le résultat doit être null");
    }
}
