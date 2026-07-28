package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;

/**
 * Rattrape les joueurs éjectés d'un serveur géré et les renvoie vers le serveur
 * d'attente.
 * <p>
 * Quand un serveur géré s'arrête, le sort de ses joueurs dépendait jusqu'ici de
 * la manière dont il avait coupé la connexion : une fermeture brutale déclenche
 * le repli automatique du proxy, une déconnexion propre le laisse déconnecter le
 * joueur. D'où un comportement erratique — renvoyé au limbo une fois sur deux.
 * <p>
 * Le plugin sait quels serveurs il gère et où patientent les joueurs : il impose
 * donc la destination. La raison annoncée par le serveur reste transmise, pour
 * ne pas masquer une éjection volontaire.
 */
public class PlayerKickedEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;

    public PlayerKickedEventListener(PanelAutoStarter plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerKicked(PlayerKickedEvent event) {
        if (!plugin.getConfig().getBoolean("queue.catch-kicks", true)) {
            return;
        }

        CommonServer from = event.getFrom();
        if (from == null || !plugin.getServers().containsKey(from)) {
            // Serveur non gere par le plugin : ce n'est pas son affaire.
            return;
        }

        CommonServer queueServer = plugin.getProxy().getServer(plugin.getConfig().getString("queue.server"));
        if (queueServer == null || queueServer.equals(from)) {
            // Rediriger vers le serveur d'ou l'on vient d'etre ejecte
            // enfermerait le joueur dans une boucle.
            return;
        }

        event.setRedirectTo(queueServer);
    }
}
