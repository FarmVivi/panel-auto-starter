package fr.farmvivi.panelautostarter.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.velocity.ping.VelocityServerPing;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;

public class VelocityProxyPingEventListener {
    private final Collection<EventListener> eventListeners;

    public VelocityProxyPingEventListener(Collection<EventListener> eventListeners) {
        this.eventListeners = eventListeners;
    }

    @Subscribe
    public void onProxyPing(com.velocitypowered.api.event.proxy.ProxyPingEvent event) {
        String host = null;

        // Get the host
        Optional<InetSocketAddress> virtualHost = event.getConnection().getVirtualHost();
        if (virtualHost.isPresent()) {
            host = virtualHost.get().getHostString();
        }

        // Construct the event
        ProxyPingEvent proxyPingEvent = new ProxyPingEvent(host, new VelocityServerPing(event.getPing()));

        // Call the event
        for (EventListener eventListener : eventListeners) {
            eventListener.onProxyPing(proxyPingEvent);
        }

        // Post call
        proxyPingEvent.postCall();

        // Retrieve ping
        CommonServerPing response = proxyPingEvent.getResponse();

        if (response instanceof VelocityServerPing velocityServerPing) {
            // Set the ping
            event.setPing(velocityServerPing.toVelocityServerPing());
        }
    }
}
