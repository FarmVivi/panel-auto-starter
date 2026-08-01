package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.message.SoundSpec;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Retours en attente d'un joueur pas encore en mesure de les recevoir.
 * <p>
 * Lorsqu'un joueur rejoint le proxy directement sur un serveur éteint, la
 * redirection vers le limbo est décidée <em>avant</em> que sa connexion
 * n'atteigne l'état de jeu. Tout ce qu'on lui adresse à cet instant — message
 * de chat comme titre à l'écran — est purement et simplement perdu : le client
 * n'est pas encore en mesure de l'afficher.
 * <p>
 * Les retours sont donc mis de côté ici sous forme d'actions, puis rejoués à
 * l'arrivée effective du joueur sur un serveur, dans leur ordre d'origine.
 */
public class PendingNotifications {
    private final Map<UUID, List<Consumer<CommonPlayer>>> pending = new ConcurrentHashMap<>();

    /**
     * Met un message de chat de côté.
     *
     * @param player  le joueur
     * @param message le message à délivrer plus tard
     */
    public void queueMessage(CommonPlayer player, Component message) {
        if (message == null) {
            return;
        }
        queue(player, target -> target.sendMessage(message));
    }

    /**
     * Met un titre de côté.
     *
     * @param player   le joueur
     * @param title    le titre
     * @param subtitle le sous-titre
     * @param fadeIn   durée d'apparition
     * @param stay     durée d'affichage plein
     * @param fadeOut  durée de disparition
     */
    public void queueTitle(CommonPlayer player, Component title, Component subtitle,
                           Duration fadeIn, Duration stay, Duration fadeOut) {
        if (title == null || subtitle == null) {
            return;
        }
        queue(player, target -> target.showTitle(title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Met un son de côté.
     *
     * @param player le joueur
     * @param sound  le son à jouer plus tard
     */
    public void queueSound(CommonPlayer player, SoundSpec sound) {
        if (sound == null || sound.isSilent()) {
            return;
        }
        queue(player, target -> target.playSound(sound.name(), sound.volume(), sound.pitch()));
    }

    private void queue(CommonPlayer player, Consumer<CommonPlayer> action) {
        if (player == null) {
            return;
        }
        pending.computeIfAbsent(player.getUniqueId(), key -> new CopyOnWriteArrayList<>()).add(action);
    }

    /**
     * Rejoue puis oublie les retours en attente d'un joueur.
     *
     * @param player le joueur
     * @return le nombre de retours délivrés
     */
    /**
     * Indique si un joueur a quelque chose en attente.
     * <p>
     * Sert à ne pas superposer un message d'accueil à une explication qui
     * compte davantage — le joueur ramené d'un serveur qui vient de s'arrêter
     * doit lire pourquoi, pas un « bienvenue ».
     *
     * @param player le joueur
     * @return true si quelque chose l'attend
     */
    public boolean hasPending(CommonPlayer player) {
        return player != null && pending.containsKey(player.getUniqueId());
    }

    public int flush(CommonPlayer player) {
        if (player == null) {
            return 0;
        }
        List<Consumer<CommonPlayer>> actions = pending.remove(player.getUniqueId());
        if (actions == null) {
            return 0;
        }
        for (Consumer<CommonPlayer> action : actions) {
            action.accept(player);
        }
        return actions.size();
    }

    /**
     * Oublie les retours en attente d'un joueur sans les délivrer.
     * <p>
     * Indispensable à la déconnexion : sans cela, un joueur parti avant
     * d'arriver sur le limbo laisserait ses retours en mémoire indéfiniment.
     *
     * @param player le joueur
     */
    public void discard(CommonPlayer player) {
        if (player != null) {
            pending.remove(player.getUniqueId());
        }
    }

    /**
     * Nombre de joueurs ayant des retours en attente.
     *
     * @return le nombre de joueurs concernés
     */
    public int size() {
        return pending.size();
    }
}
