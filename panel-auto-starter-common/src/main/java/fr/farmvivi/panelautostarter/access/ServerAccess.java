package fr.farmvivi.panelautostarter.access;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;

import java.util.Locale;

/**
 * Décide si un joueur a le droit d'entrer sur un serveur géré.
 * <p>
 * L'accès se joue à trois portes, qui doivent toutes s'ouvrir :
 * <ol>
 *   <li>la restriction propre au proxy, quand il en a une — le
 *       {@code restricted} de BungeeCord, réutilisé plutôt que redéclaré ;</li>
 *   <li>la permission du plugin, {@code panelautostarter.join.<serveur>}, qui
 *       vaut sur les deux plateformes ;</li>
 *   <li>la liste blanche du serveur, quand elle est active.</li>
 * </ol>
 * Permission et liste blanche répondent à deux besoins distincts : la première
 * dit <em>quel groupe</em> a le droit d'entrer et se gère dans LuckPerms, la
 * seconde <em>quelles personnes</em> et se gère en jeu. Une liste blanche ne
 * dispense donc pas de la permission.
 * <p>
 * <strong>Ce contrôle s'applique aux événements de connexion, jamais au menu
 * de sélection.</strong> Le menu se contente de demander un serveur, comme le
 * ferait un {@code /server} : c'est ici que la demande est acceptée ou refusée,
 * de sorte qu'aucun chemin détourné — commande, hôte forcé, redirection d'un
 * autre plugin — ne contourne la règle.
 */
public final class ServerAccess {

    /**
     * Motif de la permission d'accès. Le {@code %s} reçoit le nom du serveur.
     */
    public static final String DEFAULT_PERMISSION_FORMAT = "panelautostarter.join.%s";

    /**
     * Raison d'un refus, pour que l'appelant puisse l'expliquer au joueur.
     */
    public enum Result {
        /** Le joueur peut entrer. */
        ALLOWED,
        /** Il lui manque la permission d'accès. */
        NO_PERMISSION,
        /** Il ne figure pas sur la liste blanche du serveur. */
        NOT_WHITELISTED;

        public boolean isAllowed() {
            return this == ALLOWED;
        }
    }

    private final AccessSettings settings;
    private final WhitelistStore whitelists;

    public ServerAccess(AccessSettings settings, WhitelistStore whitelists) {
        this.settings = settings;
        this.whitelists = whitelists;
    }

    /**
     * Retourne le nœud de permission attendu pour un serveur.
     *
     * @param serverName le nom du serveur
     * @return le nœud de permission
     */
    public String permissionFor(String serverName) {
        // Les noeuds de permission sont conventionnellement en minuscules, et
        // LuckPerms les compare ainsi : un serveur nomme « Survie » donnerait
        // sinon une permission que personne n'arriverait a accorder.
        return String.format(settings.permissionFormat(), serverName.toLowerCase(Locale.ROOT));
    }

    /**
     * Vérifie si un joueur peut entrer sur un serveur.
     *
     * @param player le joueur
     * @param server le serveur visé
     * @return le verdict, et sa raison en cas de refus
     */
    public Result check(CommonPlayer player, CommonServer server) {
        if (player == null || server == null || !settings.enabled()) {
            return Result.ALLOWED;
        }

        if (settings.respectProxyRestrictions() && !server.isAllowedByProxy(player)) {
            return Result.NO_PERMISSION;
        }

        if (settings.requirePermission() && !player.hasPermission(permissionFor(server.getName()))) {
            return Result.NO_PERMISSION;
        }

        if (!whitelists.get(server.getName()).allows(player.getUniqueId())) {
            return Result.NOT_WHITELISTED;
        }

        return Result.ALLOWED;
    }
}
