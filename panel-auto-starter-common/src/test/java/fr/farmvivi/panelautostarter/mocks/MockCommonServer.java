package fr.farmvivi.panelautostarter.mocks;

import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;

/**
 * Mock de CommonServer pour les tests unitaires.
 */
public class MockCommonServer implements CommonServer {
    private final String name;
    private final String displayName;
    private int playerCount;
    private CommonServerPing lastPingResult;
    private Callback<CommonServerPing> lastPingCallback;
    private int pingCallCount = 0;

    public MockCommonServer(String name) {
        this.name = name;
        this.displayName = name;
        this.playerCount = 0;
    }

    public MockCommonServer(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.playerCount = 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int getPlayerCount() {
        return playerCount;
    }

    @Override
    public void ping(Callback<CommonServerPing> callback) {
        this.lastPingCallback = callback;
        pingCallCount++;
    }

    // Getters et setters pour les tests
    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public void setPingResult(CommonServerPing result) {
        this.lastPingResult = result;
    }

    public Callback<CommonServerPing> getLastPingCallback() {
        return lastPingCallback;
    }

    public int getPingCallCount() {
        return pingCallCount;
    }

    public CommonServerPing getLastPingResult() {
        return lastPingResult;
    }

    /**
     * Simule une réponse de ping.
     */
    public void simulatePingResponse(CommonServerPing result) {
        setPingResult(result);
        if (lastPingCallback != null) {
            lastPingCallback.done(result);
        }
    }

    /**
     * Simule une réponse null (serveur hors ligne).
     */
    public void simulatePingTimeout() {
        setPingResult(null);
        if (lastPingCallback != null) {
            lastPingCallback.done(null);
        }
    }
}
