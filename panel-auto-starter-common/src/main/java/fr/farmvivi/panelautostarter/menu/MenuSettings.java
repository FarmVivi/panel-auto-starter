package fr.farmvivi.panelautostarter.menu;

import net.md_5.bungee.config.Configuration;

import java.util.Locale;

/**
 * Réglages des menus.
 *
 * @param enabled       enregistre la commande de menu
 * @param renderer      rendu souhaité : {@code auto}, {@code chat} ou le nom
 *                      d'un rendu en inventaire
 * @param command       nom de la commande ouvrant le menu des serveurs
 * @param serverCommand commande de connexion du proxy, appelée au clic
 */
public record MenuSettings(boolean enabled, String renderer, String command, String serverCommand,
                           CompassSettings compass) {

    /**
     * Boussole posée dans la barre d'action rapide du serveur d'attente.
     *
     * @param enabled la pose
     * @param slot    la case, de 0 à 8
     * @param item    l'identifiant de l'objet
     * @param name    son nom affiché
     */
    public record CompassSettings(boolean enabled, int slot, String item, String name) {

        /**
         * Réglages par défaut.
         *
         * @return les réglages par défaut
         */
        public static CompassSettings defaults() {
            return new CompassSettings(true, 4, "minecraft:compass", "Choisir un serveur");
        }
    }

    /** Choisit le meilleur rendu disponible. */
    public static final String RENDERER_AUTO = "auto";

    /**
     * Réglages par défaut.
     *
     * @return les réglages par défaut
     */
    public static MenuSettings defaults() {
        return new MenuSettings(true, RENDERER_AUTO, "servers", "server",
                CompassSettings.defaults());
    }

    /**
     * Lit les réglages depuis la configuration.
     *
     * @param config la configuration, éventuellement null
     * @return les réglages, complétés par les défauts
     */
    public static MenuSettings from(Configuration config) {
        if (config == null) {
            return defaults();
        }
        MenuSettings fallback = defaults();
        return new MenuSettings(
                config.getBoolean("menu.enabled", fallback.enabled()),
                config.getString("menu.renderer", fallback.renderer())
                        .trim().toLowerCase(Locale.ROOT),
                blankToDefault(config.getString("menu.command", fallback.command()),
                        fallback.command()),
                blankToDefault(config.getString("menu.server-command", fallback.serverCommand()),
                        fallback.serverCommand()),
                new CompassSettings(
                        config.getBoolean("menu.compass.enabled", fallback.compass().enabled()),
                        // Hors de la barre d'action rapide, la case n'existe
                        // pas : on ramene plutot que d'ecrire dans le vide.
                        Math.max(0, Math.min(8,
                                config.getInt("menu.compass.slot", fallback.compass().slot()))),
                        blankToDefault(config.getString("menu.compass.item",
                                fallback.compass().item()), fallback.compass().item()),
                        blankToDefault(config.getString("menu.compass.name",
                                fallback.compass().name()), fallback.compass().name())));
    }

    /**
     * Un nom de commande vide empêcherait tout enregistrement : mieux vaut le
     * défaut qu'une commande qu'on ne peut pas taper.
     */
    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
