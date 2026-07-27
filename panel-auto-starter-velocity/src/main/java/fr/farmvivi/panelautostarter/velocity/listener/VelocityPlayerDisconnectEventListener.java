package fr.farmvivi.panelautostarter.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.velocity.VelocityProxy;

import java.util.Collection;

public class VelocityPlayerDisconnectEventListener {
    private final VelocityProxy velocityProxy;
    private final Collection<EventListener> eventListeners;

    public VelocityPlayerDisconnectEventListener(VelocityProxy velocityProxy, Collection<EventListener> eventListeners) {
        this.velocityProxy = velocityProxy;
        this.eventListeners = eventListeners;
    }

    @Subscribe
    public void onPlayerDisconnect(com.velocitypowered.api.event.connection.DisconnectEvent event) {
        // Get the player
        CommonPlayer player = velocityProxy.getPlayer(event.getPlayer());

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
