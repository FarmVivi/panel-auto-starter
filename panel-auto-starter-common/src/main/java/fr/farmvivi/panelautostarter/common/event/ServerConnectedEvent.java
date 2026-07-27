package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;

/**
 * Déclenché une fois le joueur effectivement arrivé sur un serveur.
 * <p>
 * À distinguer de {@link ServerConnectEvent}, qui précède la connexion et
 * permet encore de la rediriger. Ici la connexion est établie : c'est le
 * premier moment où l'on peut réellement écrire au joueur lors d'une première
 * arrivée sur le proxy.
 */
public class ServerConnectedEvent extends Event {
    private final CommonPlayer player;
    private final CommonServer server;

    public ServerConnectedEvent(CommonPlayer player, CommonServer server) {
        this.player = player;
        this.server = server;
    }

    public CommonPlayer getPlayer() {
        return player;
    }

    public CommonServer getServer() {
        return server;
    }
}
