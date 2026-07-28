package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Locale;

/**
 * Renvoie vers le serveur d'attente les joueurs qu'un serveur géré vient de
 * perdre, quand ce serveur s'arrête ou tombe.
 * <p>
 * Sans cela, leur sort dépend de la manière dont le serveur a coupé la
 * connexion : une fermeture brutale déclenche le repli automatique du proxy, une
 * déconnexion propre le laisse déconnecter le joueur. D'où un comportement
 * erratique — renvoyé au limbo une fois sur deux.
 * <p>
 * Une exclusion volontaire n'est en revanche pas rattrapée : un joueur qu'un
 * administrateur vient d'expulser doit rester dehors, avec son motif.
 */
public class PlayerKickedEventListener extends EventAdapter {
    static final String SHUTDOWN_REASONS_PATH = "queue.shutdown-reasons";

    /**
     * Motifs annoncés par un serveur qui s'éteint, reconnus sans configuration.
     */
    static final List<String> DEFAULT_SHUTDOWN_REASONS = List.of(
            "Server closed",
            "Server is restarting",
            "Serveur fermé",
            "Multiplayer is disabled");

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
        if (from == null) {
            return;
        }

        MinecraftServer managed = plugin.getServers().get(from);
        if (managed == null) {
            // Serveur non gere par le plugin : ce n'est pas son affaire.
            return;
        }

        if (!looksLikeShutdown(managed, event.getReason())) {
            // Exclusion volontaire : le joueur doit rester dehors et lire son motif.
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
     * Distingue un serveur qui disparaît d'un administrateur qui expulse.
     * <p>
     * Trois indices, du plus fiable au moins :
     * <ul>
     *   <li>le plugin sait qu'il a demandé l'arrêt, ou que le serveur ne répond
     *       plus — cas certain ;</li>
     *   <li>aucun motif n'accompagne l'éjection : la connexion a été coupée sans
     *       que rien ne soit dit, ce qu'un plantage produit ;</li>
     *   <li>le motif figure parmi ceux annoncés à l'extinction. Un arrêt lancé
     *       depuis le panel envoie « Server closed » avant que la surveillance
     *       n'ait constaté quoi que ce soit ; c'est le seul indice disponible à
     *       cet instant, d'où cette liste, ajustable selon la langue du serveur.</li>
     * </ul>
     */
    private boolean looksLikeShutdown(MinecraftServer managed, Component reason) {
        if (managed.isShuttingDownOrDown()) {
            return true;
        }
        if (reason == null) {
            return true;
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(reason)
                .toLowerCase(Locale.ROOT).trim();
        if (plain.isEmpty()) {
            return true;
        }

        // Le config.yml n'est copie qu'a la premiere installation : sans defaut
        // code en dur, une installation existante lirait une liste vide et ne
        // reconnaitrait jamais un arret lance depuis le panel. On ne retombe sur
        // le defaut que si la cle est absente, pour respecter une liste
        // volontairement videe.
        List<String> patterns = plugin.getConfig().contains(SHUTDOWN_REASONS_PATH)
                ? plugin.getConfig().getStringList(SHUTDOWN_REASONS_PATH)
                : DEFAULT_SHUTDOWN_REASONS;
        for (String pattern : patterns) {
            if (!pattern.isBlank() && plain.contains(pattern.toLowerCase(Locale.ROOT).trim())) {
                return true;
            }
        }
        return false;
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
