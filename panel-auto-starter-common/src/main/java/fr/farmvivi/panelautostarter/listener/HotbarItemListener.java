package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.menu.HotbarItem;

/**
 * Pose l'objet d'ouverture du menu à l'arrivée sur le serveur d'attente.
 * <p>
 * Uniquement là. L'objet n'existe que chez le client — le proxy ne possède pas
 * l'inventaire du joueur — et le premier serveur qui touche à cet inventaire
 * réaffirme le sien, faisant disparaître le nôtre. Un serveur d'attente n'en
 * fait rien, un serveur de jeu si : le poser ailleurs donnerait un objet
 * clignotant.
 * <p>
 * La pose se fait à l'arrivée effective et non à la demande de connexion : la
 * connexion n'a pas encore atteint l'état de jeu, et le paquet serait perdu —
 * le même travers que celui des messages différés.
 */
public class HotbarItemListener extends EventAdapter {

    private final HotbarItem item;
    private final String limboServerName;

    /**
     * @param item            l'objet à poser
     * @param limboServerName le nom du serveur d'attente
     */
    public HotbarItemListener(HotbarItem item, String limboServerName) {
        this.item = item;
        this.limboServerName = limboServerName;
    }

    @Override
    public void onServerConnected(ServerConnectedEvent event) {
        CommonServer server = event.getServer();
        boolean onLimbo = server != null && limboServerName != null
                && limboServerName.equalsIgnoreCase(server.getName());

        if (onLimbo) {
            item.give(event.getPlayer());
        } else {
            // Ailleurs, l'objet n'a plus lieu d'etre — et surtout, continuer a
            // croire le joueur porteur ferait intercepter ses clics dans
            // l'inventaire d'un serveur de jeu.
            item.forget(event.getPlayer().getUniqueId());
        }
    }

    @Override
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event.getPlayer() != null) {
            item.forget(event.getPlayer().getUniqueId());
        }
    }
}
