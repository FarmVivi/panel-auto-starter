package fr.farmvivi.panelautostarter.bungee.listener;

import fr.farmvivi.panelautostarter.bungee.BungeeProxy;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class BungeePlayerDisconnectEventListener implements Listener {
    private final BungeeProxy bungeeProxy;
    private final Collection<EventListener> eventListeners;

    public BungeePlayerDisconnectEventListener(BungeeProxy bungeeProxy, Collection<EventListener> eventListeners) {
        this.bungeeProxy = bungeeProxy;
        this.eventListeners = eventListeners;
    }

    @EventHandler
    public void onPlayerDisconnect(net.md_5.bungee.api.event.PlayerDisconnectEvent event) {
        // Get the player
        CommonPlayer player = bungeeProxy.getPlayer(event.getPlayer());

        // Construct the event
        PlayerDisconnectEvent playerDisconnectEvent = new PlayerDisconnectEvent(player);

        // Call the event
        for (EventListener eventListener : eventListeners) {
            eventListener.onPlayerDisconnect(playerDisconnectEvent);
        }

        // Post call
        playerDisconnectEvent.postCall();
    }
}
