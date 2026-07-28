package fr.farmvivi.panelautostarter.access;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Liste blanche d'un serveur : qui a le droit d'y entrer, en plus de la
 * permission.
 * <p>
 * Les joueurs sont indexés par identifiant et non par pseudo : un pseudo se
 * change, et une liste blanche qui laisse entrer le nouveau porteur d'un ancien
 * pseudo ne remplit pas son office. Le pseudo est tout de même conservé, pour
 * que le fichier reste lisible et que l'interface d'administration puisse
 * afficher autre chose qu'un identifiant.
 * <p>
 * Lue depuis les événements de connexion et modifiée depuis les commandes
 * d'administration, d'où la synchronisation.
 */
public final class Whitelist {

    private final String serverName;
    private boolean enabled;
    private final Map<UUID, String> players = new LinkedHashMap<>();

    public Whitelist(String serverName) {
        this(serverName, false);
    }

    public Whitelist(String serverName, boolean enabled) {
        this.serverName = serverName;
        this.enabled = enabled;
    }

    public String getServerName() {
        return serverName;
    }

    /**
     * Indique si la liste blanche filtre les entrées.
     * <p>
     * Désactivée, elle laisse tout le monde passer <em>sans être vidée</em> :
     * un administrateur peut la suspendre le temps d'un événement et la
     * rétablir ensuite sans avoir à la ressaisir.
     *
     * @return true si elle est appliquée
     */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Indique si un joueur figure sur la liste, qu'elle soit appliquée ou non.
     *
     * @param uuid l'identifiant du joueur
     * @return true s'il y figure
     */
    public synchronized boolean contains(UUID uuid) {
        return uuid != null && players.containsKey(uuid);
    }

    /**
     * Indique si la liste laisse passer un joueur.
     *
     * @param uuid l'identifiant du joueur
     * @return true si elle est désactivée ou si le joueur y figure
     */
    public synchronized boolean allows(UUID uuid) {
        return !enabled || contains(uuid);
    }

    /**
     * Inscrit un joueur.
     *
     * @param uuid     son identifiant
     * @param username son pseudo, à titre indicatif
     * @return true s'il vient d'être ajouté, false s'il y figurait déjà
     */
    public synchronized boolean add(UUID uuid, String username) {
        if (uuid == null) {
            return false;
        }
        // Le pseudo est rafraichi au passage : celui qui etait enregistre peut
        // dater d'avant un changement de nom.
        return players.put(uuid, username) == null;
    }

    /**
     * Retire un joueur.
     *
     * @param uuid son identifiant
     * @return true s'il y figurait
     */
    public synchronized boolean remove(UUID uuid) {
        return uuid != null && players.remove(uuid) != null;
    }

    /**
     * Retourne les joueurs inscrits, du plus ancien au plus récent.
     *
     * @return une copie de la liste
     */
    public synchronized Map<UUID, String> getPlayers() {
        return new LinkedHashMap<>(players);
    }

    public synchronized int size() {
        return players.size();
    }
}
