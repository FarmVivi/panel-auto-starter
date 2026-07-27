package fr.farmvivi.panelautostarter.mocks;

import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock de CommonServerPing pour les tests unitaires.
 */
public class MockCommonServerPing implements CommonServerPing {
    private Component descriptionComponent;
    private CommonFavicon favicon;
    private int protocolVersion;
    private Component protocolName;
    private int onlinePlayers;
    private int maxPlayers;
    private List<Component> samplePlayers;

    public MockCommonServerPing() {
        this.onlinePlayers = 0;
        this.maxPlayers = 20;
        this.protocolVersion = 769; // 1.20.x
        this.protocolName = Component.text("1.20");
        this.samplePlayers = new ArrayList<>();
    }

    public MockCommonServerPing(int onlinePlayers, int maxPlayers) {
        this();
        this.onlinePlayers = onlinePlayers;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public Component getDescriptionComponent() {
        return descriptionComponent;
    }

    @Override
    public void setDescriptionComponent(Component descriptionComponent) {
        this.descriptionComponent = descriptionComponent;
    }

    @Override
    public CommonFavicon getFavicon() {
        return favicon;
    }

    @Override
    public void setFavicon(CommonFavicon favicon) {
        this.favicon = favicon;
    }

    @Override
    public int getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public Component getProtocolName() {
        return protocolName;
    }

    @Override
    public void setProtocolName(Component protocolName) {
        this.protocolName = protocolName;
    }

    @Override
    public int getOnlinePlayers() {
        return onlinePlayers;
    }

    @Override
    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    @Override
    public int getMaxPlayers() {
        return maxPlayers;
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    @Override
    public List<Component> getSamplePlayers() {
        return samplePlayers;
    }

    @Override
    public void setSamplePlayers(List<Component> samplePlayers) {
        this.samplePlayers = samplePlayers;
    }
}
