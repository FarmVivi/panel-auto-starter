package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Messages en attente d'un joueur pas encore en mesure de les recevoir.
 * <p>
 * Lorsqu'un joueur rejoint le proxy directement sur un serveur éteint, la
 * redirection vers le limbo est décidée <em>avant</em> que sa connexion
 * n'atteigne l'état de jeu. Un message envoyé à cet instant est purement et
 * simplement perdu : le client n'est pas encore en mesure d'afficher du chat.
 * <p>
 * Les messages sont donc mis de côté ici, puis délivrés à l'arrivée effective
 * du joueur sur un serveur.
 */
public class PendingMessages {
    private final Map<UUID, List<Component>> messages = new ConcurrentHashMap<>();

    /**
     * Met un message de côté pour un joueur.
     *
     * @param player  le joueur
     * @param message le message à délivrer plus tard
     */
    public void queue(CommonPlayer player, Component message) {
        if (player == null || message == null) {
            return;
        }
        messages.computeIfAbsent(player.getUniqueId(), key -> new CopyOnWriteArrayList<>()).add(message);
    }

    /**
     * Délivre puis oublie les messages en attente d'un joueur.
     *
     * @param player le joueur
     * @return le nombre de messages délivrés
     */
    public int flush(CommonPlayer player) {
        if (player == null) {
            return 0;
        }
        List<Component> pending = messages.remove(player.getUniqueId());
        if (pending == null) {
            return 0;
        }
        for (Component message : pending) {
            player.sendMessage(message);
        }
        return pending.size();
    }

    /**
     * Oublie les messages en attente d'un joueur sans les délivrer.
     * <p>
     * Indispensable à la déconnexion : sans cela, un joueur parti avant
     * d'arriver sur le limbo laisserait ses messages en mémoire indéfiniment.
     *
     * @param player le joueur
     */
    public void discard(CommonPlayer player) {
        if (player != null) {
            messages.remove(player.getUniqueId());
        }
    }

    /**
     * Nombre de joueurs ayant des messages en attente.
     *
     * @return le nombre de joueurs concernés
     */
    public int size() {
        return messages.size();
    }
}
