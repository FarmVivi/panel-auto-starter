package fr.farmvivi.panelautostarter.bungee.ping;

import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.LinkedList;
import java.util.List;

public class BungeeServerPing implements CommonServerPing {
    private ServerPing serverPing;

    public BungeeServerPing(ServerPing serverPing) {
        this.serverPing = serverPing;
    }

    public ServerPing toBungeeServerPing() {
        return serverPing;
    }

    @Override
    public Component getDescriptionComponent() {
        return LegacyComponentSerializer.legacySection().deserialize(serverPing.getDescriptionComponent().toLegacyText());
    }

    @Override
    public void setDescriptionComponent(Component descriptionComponent) {
        String legacyMessage = LegacyComponentSerializer.legacySection().serialize(descriptionComponent);
        serverPing = new ServerPing(serverPing.getVersion(), serverPing.getPlayers(), new TextComponent(legacyMessage), serverPing.getFaviconObject());
    }

    @Override
    public CommonFavicon getFavicon() {
        return new BungeeFavicon(serverPing.getFaviconObject());
    }

    @Override
    public void setFavicon(CommonFavicon favicon) {
        if (favicon == null) {
            serverPing = new ServerPing(serverPing.getVersion(), serverPing.getPlayers(), serverPing.getDescriptionComponent(), null);
        } else if (favicon instanceof BungeeFavicon bungeeFavicon) {
            serverPing = new ServerPing(serverPing.getVersion(), serverPing.getPlayers(), serverPing.getDescriptionComponent(), bungeeFavicon.getFavicon());
        }
    }

    @Override
    public int getProtocolVersion() {
        int protocolVersion = -1;

        if (serverPing.getVersion() != null) {
            protocolVersion = serverPing.getVersion().getProtocol();
        }

        return protocolVersion;
    }

    @Override
    public void setProtocolVersion(int protocolVersion) {
        String protocolName = "";

        if (serverPing.getVersion() != null && serverPing.getVersion().getName() != null) {
            protocolName = serverPing.getVersion().getName();
        }

        serverPing = new ServerPing(new ServerPing.Protocol(protocolName, protocolVersion), serverPing.getPlayers(), serverPing.getDescriptionComponent(), serverPing.getFaviconObject());
    }

    @Override
    public Component getProtocolName() {
        String protocolName = "";

        if (serverPing.getVersion() != null && serverPing.getVersion().getName() != null) {
            protocolName = serverPing.getVersion().getName();
        }

        return LegacyComponentSerializer.legacySection().deserialize(protocolName);
    }

    @Override
    public void setProtocolName(Component protocolName) {
        int protocolVersion = -1;

        if (serverPing.getVersion() != null && serverPing.getVersion().getProtocol() != -1) {
            protocolVersion = serverPing.getVersion().getProtocol();
        }

        serverPing = new ServerPing(new ServerPing.Protocol(LegacyComponentSerializer.legacySection().serialize(protocolName), protocolVersion), serverPing.getPlayers(), serverPing.getDescriptionComponent(), serverPing.getFaviconObject());
    }

    @Override
    public int getOnlinePlayers() {
        return serverPing.getPlayers().getOnline();
    }

    @Override
    public void setOnlinePlayers(int onlinePlayers) {
        serverPing = new ServerPing(serverPing.getVersion(), new ServerPing.Players(onlinePlayers, serverPing.getPlayers().getMax(), serverPing.getPlayers().getSample()), serverPing.getDescriptionComponent(), serverPing.getFaviconObject());
    }

    @Override
    public int getMaxPlayers() {
        return serverPing.getPlayers().getMax();
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        serverPing = new ServerPing(serverPing.getVersion(), new ServerPing.Players(serverPing.getPlayers().getOnline(), maxPlayers, serverPing.getPlayers().getSample()), serverPing.getDescriptionComponent(), serverPing.getFaviconObject());
    }

    @Override
    public List<Component> getSamplePlayers() {
        List<Component> samplePlayers = new LinkedList<>();

        for (ServerPing.PlayerInfo playerInfo : serverPing.getPlayers().getSample()) {
            samplePlayers.add(LegacyComponentSerializer.legacySection().deserialize(playerInfo.getName()));
        }

        return samplePlayers;
    }

    @Override
    public void setSamplePlayers(List<Component> samplePlayers) {
        List<ServerPing.PlayerInfo> playerInfos = new LinkedList<>();

        for (Component samplePlayer : samplePlayers) {
            playerInfos.add(new ServerPing.PlayerInfo(LegacyComponentSerializer.legacySection().serialize(samplePlayer), ""));
        }

        serverPing = new ServerPing(serverPing.getVersion(), new ServerPing.Players(serverPing.getPlayers().getOnline(), serverPing.getPlayers().getMax(), playerInfos.toArray(new ServerPing.PlayerInfo[0])), serverPing.getDescriptionComponent(), serverPing.getFaviconObject());
    }
}
