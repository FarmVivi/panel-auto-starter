package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.Messages;

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
    private final PendingNotifications pendingNotifications;

    public PlayerKickedEventListener(PanelAutoStarter plugin) {
        this(plugin, plugin.getPendingNotifications());
    }

    PlayerKickedEventListener(PanelAutoStarter plugin, PendingNotifications pendingNotifications) {
        this.plugin = plugin;
        this.pendingNotifications = pendingNotifications;
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
        announceTo(event.getPlayer(), from, queueServer);
    }

    /**
     * Prévient le joueur de ce qui vient de lui arriver.
     * <p>
     * Le retour est mis de côté plutôt qu'envoyé tout de suite : le joueur est
     * en cours de transfert, sa connexion au serveur d'attente n'est pas encore
     * établie. Il lui sera délivré à son arrivée, par le même mécanisme que les
     * messages d'entrée en file.
     */
    private void announceTo(CommonPlayer player, CommonServer from, CommonServer queueServer) {
        MessageSettings.KickFeedbackSettings feedback = plugin.getMessageSettings().getKickFeedback();
        if (player == null || !feedback.enabled()) {
            return;
        }

        MessageSettings.TitleSettings title = plugin.getMessageSettings().getTitle();
        if (title.enabled()) {
            pendingNotifications.queueTitle(player, Messages.serverStoppedTitle(),
                    Messages.serverStoppedSubtitle(from.getDisplayName()),
                    title.fadeIn(), title.stay(), title.fadeOut());
        }
        pendingNotifications.queueSound(player, feedback.sound());
        pendingNotifications.queueMessage(player,
                Messages.sentBackToQueue(from.getDisplayName(), queueServer.getDisplayName()));
    }
}
