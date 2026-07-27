package fr.farmvivi.panelautostarter.common.listener;

import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;

public class EventAdapter implements EventListener {
    /**
     * Default no-op implementation for subclasses that don't need this event.
     * Subclasses can override this method to handle player disconnect events.
     */
    @Override
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // This is a default implementation for EventAdapter pattern
    }

    /**
     * Default no-op implementation for subclasses that don't need this event.
     * Subclasses can override this method to handle proxy ping events.
     */
    @Override
    public void onProxyPing(ProxyPingEvent event) {
        // This is a default implementation for EventAdapter pattern
    }

    /**
     * Default no-op implementation for subclasses that don't need this event.
     * Subclasses can override this method to handle server connect events.
     */
    @Override
    public void onServerConnect(ServerConnectEvent event) {
        // This is a default implementation for EventAdapter pattern
    }
}
