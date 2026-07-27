package fr.farmvivi.panelautostarter.bungee.listener;

import fr.farmvivi.panelautostarter.bungee.BungeeProxy;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class BungeeServerConnectedEventListener implements Listener {
    private final BungeeProxy bungeeProxy;
    private final Collection<EventListener> eventListeners;

    public BungeeServerConnectedEventListener(BungeeProxy bungeeProxy, Collection<EventListener> eventListeners) {
        this.bungeeProxy = bungeeProxy;
        this.eventListeners = eventListeners;
    }

    @EventHandler
    public void onServerConnected(net.md_5.bungee.api.event.ServerConnectedEvent event) {
        // Get the player
        CommonPlayer player = bungeeProxy.getPlayer(event.getPlayer());

        // Get the server the player has just landed on
        CommonServer server = bungeeProxy.getServer(event.getServer().getInfo().getName());

        // Construct the event
        ServerConnectedEvent serverConnectedEvent = new ServerConnectedEvent(player, server);

        // Call the event
        for (EventListener eventListener : eventListeners) {
            eventListener.onServerConnected(serverConnectedEvent);
        }

        // Post call
        serverConnectedEvent.postCall();
    }
}
