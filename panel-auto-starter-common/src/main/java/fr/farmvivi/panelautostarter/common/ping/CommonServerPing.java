package fr.farmvivi.panelautostarter.common.ping;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface CommonServerPing {
    Component getDescriptionComponent();

    void setDescriptionComponent(Component descriptionComponent);

    CommonFavicon getFavicon();

    void setFavicon(CommonFavicon favicon);

    int getProtocolVersion();

    void setProtocolVersion(int protocolVersion);

    Component getProtocolName();

    void setProtocolName(Component protocolName);

    int getOnlinePlayers();

    void setOnlinePlayers(int onlinePlayers);

    int getMaxPlayers();

    void setMaxPlayers(int maxPlayers);

    List<Component> getSamplePlayers();

    void setSamplePlayers(List<Component> samplePlayers);
}
