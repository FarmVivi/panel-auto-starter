package fr.farmvivi.panelautostarter.velocity.ping;

import com.velocitypowered.api.proxy.server.ServerPing;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class VelocityServerPing implements CommonServerPing {
    private final ServerPing.Builder serverPingBuilder;

    public VelocityServerPing(ServerPing serverPing) {
        this.serverPingBuilder = serverPing.asBuilder();
    }

    public ServerPing toVelocityServerPing() {
        return serverPingBuilder.build();
    }

    @Override
    public Component getDescriptionComponent() {
        return serverPingBuilder.getDescriptionComponent().orElse(null);
    }

    @Override
    public void setDescriptionComponent(Component descriptionComponent) {
        serverPingBuilder.description(descriptionComponent);
    }

    @Override
    public CommonFavicon getFavicon() {
        return serverPingBuilder.getFavicon().map(VelocityFavicon::new).orElse(null);
    }

    @Override
    public void setFavicon(CommonFavicon favicon) {
        if (favicon == null) {
            serverPingBuilder.clearFavicon();
        } else if (favicon instanceof VelocityFavicon velocityFavicon) {
            serverPingBuilder.favicon(velocityFavicon.getFavicon());
        }
    }

    @Override
    public int getProtocolVersion() {
        int protocolVersion = -1;

        if (serverPingBuilder.getVersion() != null) {
            protocolVersion = serverPingBuilder.getVersion().getProtocol();
        }

        return protocolVersion;
    }

    @Override
    public void setProtocolVersion(int protocolVersion) {
        String protocolName = "";

        if (serverPingBuilder.getVersion() != null && serverPingBuilder.getVersion().getName() != null) {
            protocolName = serverPingBuilder.getVersion().getName();
        }

        serverPingBuilder.version(new ServerPing.Version(protocolVersion, protocolName));
    }

    @Override
    public Component getProtocolName() {
        String protocolName = "";

        if (serverPingBuilder.getVersion() != null && serverPingBuilder.getVersion().getName() != null) {
            protocolName = serverPingBuilder.getVersion().getName();
        }

        return LegacyComponentSerializer.legacySection().deserialize(protocolName);
    }

    @Override
    public void setProtocolName(Component protocolName) {
        int protocolVersion = -1;

        if (serverPingBuilder.getVersion() != null) {
            protocolVersion = serverPingBuilder.getVersion().getProtocol();
        }

        serverPingBuilder.version(new ServerPing.Version(protocolVersion, LegacyComponentSerializer.legacySection().serialize(protocolName)));
    }

    @Override
    public int getOnlinePlayers() {
        return serverPingBuilder.getOnlinePlayers();
    }

    @Override
    public void setOnlinePlayers(int onlinePlayers) {
        serverPingBuilder.onlinePlayers(onlinePlayers);
    }

    @Override
    public int getMaxPlayers() {
        return serverPingBuilder.getMaximumPlayers();
    }

    @Override
    public void setMaxPlayers(int maxPlayers) {
        serverPingBuilder.maximumPlayers(maxPlayers);
    }

    @Override
    public List<Component> getSamplePlayers() {
        List<Component> samplePlayers = new LinkedList<>();

        for (ServerPing.SamplePlayer samplePlayer : serverPingBuilder.getSamplePlayers()) {
            samplePlayers.add(LegacyComponentSerializer.legacySection().deserialize(samplePlayer.getName()));
        }

        return samplePlayers;
    }

    @Override
    public void setSamplePlayers(List<Component> samplePlayers) {
        List<ServerPing.SamplePlayer> samplePlayersList = new LinkedList<>();

        for (Component samplePlayer : samplePlayers) {
            samplePlayersList.add(new ServerPing.SamplePlayer(LegacyComponentSerializer.legacySection().serialize(samplePlayer), UUID.randomUUID()));
        }

        serverPingBuilder.samplePlayers(samplePlayersList.toArray(new ServerPing.SamplePlayer[0]));
    }
}
