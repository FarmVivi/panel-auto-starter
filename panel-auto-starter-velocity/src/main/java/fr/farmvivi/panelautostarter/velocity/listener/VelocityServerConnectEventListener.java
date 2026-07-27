package fr.farmvivi.panelautostarter.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.velocity.VelocityProxy;
import fr.farmvivi.panelautostarter.velocity.VelocityServer;

import java.util.Collection;

public class VelocityServerConnectEventListener {
    private final VelocityProxy velocityProxy;
    private final Collection<EventListener> eventListeners;

    public VelocityServerConnectEventListener(VelocityProxy velocityProxy, Collection<EventListener> eventListeners) {
        this.velocityProxy = velocityProxy;
        this.eventListeners = eventListeners;
    }

    @Subscribe
    public void onServerConnect(com.velocitypowered.api.event.player.ServerPreConnectEvent event) {
        // Get the player
        CommonPlayer player = velocityProxy.getPlayer(event.getPlayer());

        // Get the target server
        CommonServer targetServer = velocityProxy.getServer(event.getOriginalServer().getServerInfo().getName());

        // Construct the event
        ServerConnectEvent serverConnectEvent = new ServerConnectEvent(player, targetServer);

        // Call the event
        for (EventListener eventListener : eventListeners) {
            eventListener.onServerConnect(serverConnectEvent);
        }

        // Post call
        serverConnectEvent.postCall();

        // Check if the event is cancelled
        if (serverConnectEvent.isCancelled()) {
            // Cancel the connection
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        } else {
            // Retrieve target server
            CommonServer target = serverConnectEvent.getTarget();

            // Get the VelocityServer
            VelocityServer velocityServer = (VelocityServer) target;

            // Set the target server
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(velocityServer.getServer()));
        }
    }
}
