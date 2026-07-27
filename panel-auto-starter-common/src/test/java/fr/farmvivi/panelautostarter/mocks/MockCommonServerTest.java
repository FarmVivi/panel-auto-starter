package fr.farmvivi.panelautostarter.mocks;

import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour MockCommonServer.
 * Vérifie que les mocks fonctionnent correctement.
 */
public class MockCommonServerTest {

    private MockCommonServer server;

    @Before
    public void setUp() {
        server = new MockCommonServer("test-server", "Test Server");
    }

    @Test
    public void testServerInitialization() {
        assertEquals("Le nom doit être test-server", "test-server", server.getName());
        assertEquals("Le display name doit être Test Server", "Test Server", server.getDisplayName());
        assertEquals("Le nombre de joueurs doit être 0", 0, server.getPlayerCount());
    }

    @Test
    public void testServerWithoutDisplayName() {
        MockCommonServer simpleServer = new MockCommonServer("simple");

        assertEquals("Le nom doit être simple", "simple", simpleServer.getName());
        assertEquals("Le display name doit être simple", "simple", simpleServer.getDisplayName());
    }

    @Test
    public void testSetPlayerCount() {
        server.setPlayerCount(5);
        assertEquals("Le nombre de joueurs doit être 5", 5, server.getPlayerCount());

        server.setPlayerCount(0);
        assertEquals("Le nombre de joueurs doit être 0", 0, server.getPlayerCount());
    }

    @Test
    public void testPingCallbackRecorded() {
        final boolean[] callbackCalled = {false};

        Callback<CommonServerPing> testCallback = result -> {
            callbackCalled[0] = true;
        };

        server.ping(testCallback);

        assertEquals("Le callback doit être enregistré", testCallback, server.getLastPingCallback());
        assertEquals("Le ping call count doit être 1", 1, server.getPingCallCount());
    }

    @Test
    public void testMultiplePingCalls() {
        Callback<CommonServerPing> callback1 = result -> {
        };
        Callback<CommonServerPing> callback2 = result -> {
        };

        server.ping(callback1);
        server.ping(callback2);

        assertEquals("Le dernier callback doit être callback2", callback2, server.getLastPingCallback());
        assertEquals("Le ping call count doit être 2", 2, server.getPingCallCount());
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

        assertTrue("Le callback doit avoir été appelé", callbackCalled[0]);
        assertEquals("Le résultat doit être le ping fourni", ping, resultCapture[0]);
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

        assertTrue("Le callback doit avoir été appelé", callbackCalled[0]);
        assertNull("Le résultat doit être null", resultCapture[0]);
    }
}
