package fr.farmvivi.panelautostarter.mocks;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Mock de CommonPlayer pour les tests unitaires.
 */
public class MockCommonPlayer implements CommonPlayer {
    private final String username;
    private final UUID uuid;
    private final String displayName;
    private boolean online;
    private CommonServer currentServer;
    private Component lastMessage;
    private String lastStringMessage;
    private int connectToServerCallCount = 0;

    public MockCommonPlayer(String username) {
        this.username = username;
        this.uuid = UUID.randomUUID();
        this.displayName = username;
        this.online = true;
        this.currentServer = null;
    }

    public MockCommonPlayer(String username, UUID uuid) {
        this.username = username;
        this.uuid = uuid;
        this.displayName = username;
        this.online = true;
        this.currentServer = null;
    }

    public MockCommonPlayer(String username, UUID uuid, String displayName) {
        this.username = username;
        this.uuid = uuid;
        this.displayName = displayName;
        this.online = true;
        this.currentServer = null;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public void sendMessage(String message) {
        this.lastStringMessage = message;
    }

    @Override
    public void sendMessage(Component message) {
        this.lastMessage = message;
    }

    @Override
    public void connectToServer(CommonServer server) {
        this.currentServer = server;
        connectToServerCallCount++;
    }

    @Override
    public CommonServer getServer() {
        return currentServer;
    }

    // Getters pour les tests
    public Component getLastMessage() {
        return lastMessage;
    }

    public String getLastStringMessage() {
        return lastStringMessage;
    }

    public int getConnectToServerCallCount() {
        return connectToServerCallCount;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setCurrentServer(CommonServer server) {
        this.currentServer = server;
    }
}
