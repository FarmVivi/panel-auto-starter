package fr.farmvivi.panelautostarter.queue;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.Messages;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Arbitre les files de tous les serveurs gérés.
 * <p>
 * Chaque {@link MinecraftServer} possède sa propre file et ne connaît qu'elle.
 * Deux règles ne peuvent donc s'énoncer qu'ici, au-dessus d'eux :
 * <ul>
 *   <li><strong>un joueur n'attend qu'un serveur à la fois</strong> — demander
 *       un nouveau serveur le retire de la file précédente, faute de quoi il
 *       resterait annoncé sur des files qu'il a abandonnées et fausserait les
 *       positions affichées aux autres ;</li>
 *   <li>la barre d'action de chaque joueur en attente est réémise à intervalle
 *       régulier, une seule tâche parcourant l'ensemble des files plutôt qu'une
 *       tâche par serveur.</li>
 * </ul>
 */
public final class QueueCoordinator {

    private final Supplier<Collection<MinecraftServer>> servers;
    private final Supplier<MessageSettings> messageSettings;

    /**
     * Numéro de rafraîchissement, qui fait tourner l'indicateur d'activité.
     * Volontairement global : tous les joueurs voient la même image au même
     * instant, ce qui coûte un seul compteur au lieu d'un par file.
     */
    private int frame = 0;

    public QueueCoordinator(PanelAutoStarter plugin) {
        this(() -> plugin.getServers().values(), plugin::getMessageSettings);
    }

    QueueCoordinator(Supplier<Collection<MinecraftServer>> servers,
                     Supplier<MessageSettings> messageSettings) {
        this.servers = servers;
        this.messageSettings = messageSettings;
    }

    /**
     * Place un joueur dans la file d'un serveur, en le retirant de toute autre.
     * <p>
     * L'opération est synchronisée pour que la garantie « une seule file »
     * tienne face à deux demandes simultanées : sans cela, deux threads
     * pourraient chacun nettoyer les files de l'autre avant d'inscrire la
     * leur, et le joueur se retrouverait dans deux files — précisément ce que
     * cette méthode existe pour empêcher.
     * <p>
     * L'ordre compte : on retire d'abord, on inscrit ensuite. L'inverse
     * laisserait une fenêtre pendant laquelle le retrait effacerait
     * l'inscription qu'on vient de faire.
     *
     * @param target le serveur attendu
     * @param player le joueur
     * @return true s'il vient d'être ajouté, false s'il attendait déjà ce serveur
     */
    public synchronized boolean join(MinecraftServer target, CommonPlayer player) {
        if (target == null || player == null) {
            return false;
        }

        for (MinecraftServer other : servers.get()) {
            if (other != target) {
                other.dequeue(player);
            }
        }

        return target.enqueue(player);
    }

    /**
     * Retire un joueur de toutes les files.
     *
     * @param player le joueur
     */
    public synchronized void leaveAll(CommonPlayer player) {
        if (player == null) {
            return;
        }
        for (MinecraftServer server : servers.get()) {
            server.dequeue(player);
        }
    }

    /**
     * Retourne le serveur qu'un joueur attend.
     *
     * @param player le joueur
     * @return le serveur attendu, ou null s'il n'attend rien
     */
    public MinecraftServer queuedServer(CommonPlayer player) {
        if (player == null) {
            return null;
        }
        for (MinecraftServer server : servers.get()) {
            if (server.isQueued(player)) {
                return server;
            }
        }
        return null;
    }

    /**
     * Réémet la barre d'action de tous les joueurs en attente.
     * <p>
     * À appeler périodiquement : le client efface la barre d'action de
     * lui-même au bout de quelques secondes, un envoi unique ne tiendrait pas
     * le temps d'un démarrage.
     */
    public void tickActionBar() {
        MessageSettings.ActionBarSettings settings = messageSettings.get().getActionBar();
        if (!settings.enabled()) {
            return;
        }

        int currentFrame;
        synchronized (this) {
            currentFrame = frame++;
        }

        for (MinecraftServer server : servers.get()) {
            // Pendant le décompte, le titre occupe déjà l'écran et dit la même
            // chose en plus grand : doubler l'information la rend confuse.
            if (server.isShowingTeleportTitles()) {
                continue;
            }

            List<CommonPlayer> queue = List.copyOf(server.getQueue());
            if (queue.isEmpty()) {
                continue;
            }

            boolean starting = !server.getStatus().equals(MinecraftServerStatus.ONLINE);
            String displayName = server.getServer().getDisplayName();

            for (int i = 0; i < queue.size(); i++) {
                CommonPlayer player = queue.get(i);
                if (!player.isOnline()) {
                    continue;
                }
                player.sendActionBar(Messages.queueActionBar(displayName, i + 1, queue.size(),
                        starting, settings.showPosition(), currentFrame));
            }
        }
    }
}
