package fr.farmvivi.panelautostarter.access;

import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;

import java.util.Locale;

/**
 * Droits d'administration, globaux ou limités à un serveur.
 * <p>
 * Un réseau confie souvent un serveur à quelqu'un sans lui donner la main sur
 * les autres : le responsable du serveur créatif gère sa liste blanche et
 * l'allume quand il veut, sans pouvoir éteindre la survie. D'où deux niveaux —
 * {@code panelautostarter.admin} pour l'ensemble,
 * {@code panelautostarter.admin.<serveur>} pour un seul.
 */
public final class AdminAccess {

    /** Droit sur l'ensemble des serveurs gérés. */
    public static final String ADMIN = "panelautostarter.admin";

    /** Préfixe du droit portant sur un seul serveur. */
    public static final String ADMIN_SERVER_PREFIX = ADMIN + ".";

    private AdminAccess() {
    }

    /**
     * Retourne le nœud accordant l'administration d'un serveur.
     *
     * @param serverName le nom du serveur
     * @return le nœud de permission
     */
    public static String permissionFor(String serverName) {
        return ADMIN_SERVER_PREFIX + serverName.toLowerCase(Locale.ROOT);
    }

    /**
     * Indique si l'auteur peut administrer un serveur donné.
     *
     * @param source     l'auteur de la commande
     * @param serverName le serveur visé
     * @return true s'il en a le droit
     */
    public static boolean canAdminister(CommonCommandSource source, String serverName) {
        if (source == null) {
            return false;
        }
        return source.hasPermission(ADMIN)
                || (serverName != null && source.hasPermission(permissionFor(serverName)));
    }

    /**
     * Indique si l'auteur administre au moins un serveur.
     * <p>
     * Sert à décider s'il a quelque chose à faire de la commande, avant même de
     * savoir de quel serveur il sera question.
     *
     * @param source      l'auteur
     * @param serverNames les serveurs gérés
     * @return true s'il en administre au moins un
     */
    public static boolean canAdministerAnything(CommonCommandSource source,
                                                Iterable<String> serverNames) {
        if (source == null) {
            return false;
        }
        if (source.hasPermission(ADMIN)) {
            return true;
        }
        for (String serverName : serverNames) {
            if (source.hasPermission(permissionFor(serverName))) {
                return true;
            }
        }
        return false;
    }
}
