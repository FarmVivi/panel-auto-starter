package fr.farmvivi.panelautostarter.bungee.listener;

import fr.farmvivi.panelautostarter.bungee.BungeeProxy;
import fr.farmvivi.panelautostarter.bungee.BungeeServer;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class BungeeServerConnectEventListener implements Listener {
    private final BungeeProxy bungeeProxy;
    private final Collection<EventListener> eventListeners;

    public BungeeServerConnectEventListener(BungeeProxy bungeeProxy, Collection<EventListener> eventListeners) {
        this.bungeeProxy = bungeeProxy;
        this.eventListeners = eventListeners;
    }

    @EventHandler
    public void onServerConnect(net.md_5.bungee.api.event.ServerConnectEvent event) {
        // Get the player
        CommonPlayer player = bungeeProxy.getPlayer(event.getPlayer());

        // Get the target server
        CommonServer targetServer = bungeeProxy.getServer(event.getTarget().getName());

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
            event.setCancelled(true);
        } else {
            // Retrieve target server
            CommonServer target = serverConnectEvent.getTarget();

            // Get the BungeeServer
            BungeeServer bungeeServer = (BungeeServer) target;

            // Set the target server
            event.setTarget(bungeeServer.getServerInfo());
        }
    }
}
