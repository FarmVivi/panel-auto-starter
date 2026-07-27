package fr.farmvivi.panelautostarter.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.velocity.VelocityProxy;

import java.util.Collection;

public class VelocityServerConnectedEventListener {
    private final VelocityProxy velocityProxy;
    private final Collection<EventListener> eventListeners;

    public VelocityServerConnectedEventListener(VelocityProxy velocityProxy, Collection<EventListener> eventListeners) {
        this.velocityProxy = velocityProxy;
        this.eventListeners = eventListeners;
    }

    @Subscribe
    public void onServerConnected(ServerPostConnectEvent event) {
        // Get the player
        CommonPlayer player = velocityProxy.getPlayer(event.getPlayer());

        // Construct the event with the server the player has just landed on
        ServerConnectedEvent serverConnectedEvent = new ServerConnectedEvent(player, player.getServer());

        // Call the event
        for (EventListener eventListener : eventListeners) {
            eventListener.onServerConnected(serverConnectedEvent);
        }

        // Post call
        serverConnectedEvent.postCall();
    }
}
