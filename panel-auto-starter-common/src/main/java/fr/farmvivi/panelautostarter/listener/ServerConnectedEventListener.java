package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.PendingMessages;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;

/**
 * Délivre les messages mis de côté, une fois le joueur réellement arrivé sur un
 * serveur et donc en mesure de les afficher.
 */
public class ServerConnectedEventListener extends EventAdapter {
    private final PendingMessages pendingMessages;

    public ServerConnectedEventListener(PendingMessages pendingMessages) {
        this.pendingMessages = pendingMessages;
    }

    @Override
    public void onServerConnected(ServerConnectedEvent event) {
        pendingMessages.flush(event.getPlayer());
    }
}
