package fr.farmvivi.panelautostarter.access;

import net.md_5.bungee.config.Configuration;

/**
 * Réglages du contrôle d'accès aux serveurs gérés.
 *
 * @param enabled                  applique le contrôle d'accès
 * @param requirePermission        exige la permission propre au plugin
 * @param respectProxyRestrictions réutilise la restriction déclarée au proxy,
 *                                 quand la plateforme en possède une
 * @param permissionFormat         motif du nœud de permission, {@code %s}
 *                                 recevant le nom du serveur
 */
public record AccessSettings(boolean enabled, boolean requirePermission,
                             boolean respectProxyRestrictions, String permissionFormat) {

    /**
     * Réglages par défaut.
     * <p>
     * {@code requirePermission} est faux : activer d'emblée une permission que
     * personne n'a accordée fermerait tous les serveurs d'une installation
     * existante à la première mise à jour. Le contrôle est en place, mais ne
     * refuse rien tant que l'administrateur n'a rien demandé.
     *
     * @return les réglages par défaut
     */
    public static AccessSettings defaults() {
        return new AccessSettings(true, false, true, ServerAccess.DEFAULT_PERMISSION_FORMAT);
    }

    /**
     * Lit les réglages depuis la configuration.
     *
     * @param config la configuration, éventuellement null
     * @return les réglages, complétés par les défauts
     */
    public static AccessSettings from(Configuration config) {
        if (config == null) {
            return defaults();
        }
        AccessSettings fallback = defaults();

        String format = config.getString("access.permission-format", fallback.permissionFormat());
        if (format == null || !format.contains("%s")) {
            // Sans le %s, tous les serveurs partageraient la meme permission et
            // la restriction par serveur ne voudrait plus rien dire.
            format = fallback.permissionFormat();
        }

        return new AccessSettings(
                config.getBoolean("access.enabled", fallback.enabled()),
                config.getBoolean("access.require-permission", fallback.requirePermission()),
                config.getBoolean("access.respect-proxy-restrictions",
                        fallback.respectProxyRestrictions()),
                format);
    }
}
