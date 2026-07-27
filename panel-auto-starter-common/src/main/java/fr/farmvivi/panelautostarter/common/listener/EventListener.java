package fr.farmvivi.panelautostarter.common.listener;

import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;

public interface EventListener {
    void onPlayerDisconnect(PlayerDisconnectEvent event);

    void onProxyPing(ProxyPingEvent event);

    void onServerConnect(ServerConnectEvent event);

    void onServerConnected(ServerConnectedEvent event);
}
