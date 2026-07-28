package fr.farmvivi.panelautostarter.bungee.listener;

import fr.farmvivi.panelautostarter.bungee.BungeeProxy;
import fr.farmvivi.panelautostarter.bungee.BungeeServer;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collection;

public class BungeePlayerKickedEventListener implements Listener {
    private final BungeeProxy bungeeProxy;
    private final Collection<EventListener> eventListeners;

    public BungeePlayerKickedEventListener(BungeeProxy bungeeProxy, Collection<EventListener> eventListeners) {
        this.bungeeProxy = bungeeProxy;
        this.eventListeners = eventListeners;
    }

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        if (event.getKickedFrom() == null) {
            return;
        }

        CommonPlayer player = bungeeProxy.getPlayer(event.getPlayer());
        CommonServer from = bungeeProxy.getServer(event.getKickedFrom().getName());
        Component reason = toAdventure(event);

        PlayerKickedEvent kickedEvent = new PlayerKickedEvent(player, from, reason);

        for (EventListener eventListener : eventListeners) {
            eventListener.onPlayerKicked(kickedEvent);
        }

        kickedEvent.postCall();

        CommonServer redirectTo = kickedEvent.getRedirectTo();
        if (redirectTo instanceof BungeeServer bungeeServer) {
            // Annuler l'ejection et fournir une destination : BungeeCord
            // renvoie alors le joueur au lieu de le deconnecter.
            event.setCancelServer(bungeeServer.getServerInfo());
            event.setCancelled(true);
        }
    }

    private Component toAdventure(ServerKickEvent event) {
        if (event.getKickReasonComponent() == null || event.getKickReasonComponent().length == 0) {
            return null;
        }
        StringBuilder legacy = new StringBuilder();
        for (net.md_5.bungee.api.chat.BaseComponent component : event.getKickReasonComponent()) {
            legacy.append(component.toLegacyText());
        }
        return LegacyComponentSerializer.legacySection().deserialize(legacy.toString());
    }
}
