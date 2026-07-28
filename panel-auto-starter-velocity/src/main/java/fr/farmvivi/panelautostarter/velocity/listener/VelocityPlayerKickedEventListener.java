package fr.farmvivi.panelautostarter.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.velocity.VelocityProxy;
import fr.farmvivi.panelautostarter.velocity.VelocityServer;
import net.kyori.adventure.text.Component;

import java.util.Collection;

public class VelocityPlayerKickedEventListener {
    private final VelocityProxy velocityProxy;
    private final Collection<EventListener> eventListeners;

    public VelocityPlayerKickedEventListener(VelocityProxy velocityProxy, Collection<EventListener> eventListeners) {
        this.velocityProxy = velocityProxy;
        this.eventListeners = eventListeners;
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        // Une ejection survenue pendant la phase de connexion initiale ne peut
        // pas etre redirigee : le joueur n'a pas encore de serveur courant.
        if (event.kickedDuringLogin()) {
            return;
        }

        CommonPlayer player = velocityProxy.getPlayer(event.getPlayer());
        CommonServer from = velocityProxy.getServer(event.getServer().getServerInfo().getName());
        Component reason = event.getServerKickReason().orElse(null);

        PlayerKickedEvent kickedEvent = new PlayerKickedEvent(player, from, reason);

        for (EventListener eventListener : eventListeners) {
            eventListener.onPlayerKicked(kickedEvent);
        }

        kickedEvent.postCall();

        CommonServer redirectTo = kickedEvent.getRedirectTo();
        if (redirectTo instanceof VelocityServer velocityServer) {
            // La raison annoncee par le serveur est transmise au joueur : une
            // ejection volontaire ne doit pas etre masquee par la redirection.
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                    velocityServer.getServer(),
                    reason != null ? reason : Component.empty()));
        }
    }
}
