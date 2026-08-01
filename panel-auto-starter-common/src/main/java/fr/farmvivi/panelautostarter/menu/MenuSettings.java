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
public record MenuSettings(boolean enabled, String renderer, String command, String serverCommand) {

    /** Choisit le meilleur rendu disponible. */
    public static final String RENDERER_AUTO = "auto";

    /**
     * Réglages par défaut.
     *
     * @return les réglages par défaut
     */
    public static MenuSettings defaults() {
        return new MenuSettings(true, RENDERER_AUTO, "servers", "server");
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
                        fallback.serverCommand()));
    }

    /**
     * Un nom de commande vide empêcherait tout enregistrement : mieux vaut le
     * défaut qu'une commande qu'on ne peut pas taper.
     */
    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
