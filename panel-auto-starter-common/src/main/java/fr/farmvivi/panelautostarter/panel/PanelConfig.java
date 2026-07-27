package fr.farmvivi.panelautostarter.panel;

import net.md_5.bungee.config.Configuration;

/**
 * Paramètres de connexion au panel, lus depuis le fichier de configuration.
 * <p>
 * Deux formats sont acceptés :
 * <pre>
 * panel:                       # format 3.x
 *   type: pterodactyl
 *   url: https://panel.exemple.com
 *   token: ptlc_xxx
 *
 * pterodactyl:                 # format 2.x, deprecie
 *   url: https://panel.exemple.com
 *   token: ptlc_xxx
 * </pre>
 * L'ancien format reste lu pour ne pas casser les installations existantes,
 * mais {@link #isLegacyFormat()} permet à l'appelant d'émettre un avertissement.
 * Cette classe ne journalise rien elle-même, afin de rester testable sans
 * logger.
 */
public final class PanelConfig {
    /**
     * Section du format 3.x.
     */
    static final String SECTION = "panel";
    /**
     * Section du format 2.x, conservée en lecture seule pour la migration.
     */
    static final String LEGACY_SECTION = "pterodactyl";

    private final PanelType type;
    private final String url;
    private final String token;
    private final boolean legacyFormat;

    private PanelConfig(PanelType type, String url, String token, boolean legacyFormat) {
        this.type = type;
        this.url = url;
        this.token = token;
        this.legacyFormat = legacyFormat;
    }

    /**
     * Lit les paramètres de connexion, en acceptant l'ancien format.
     *
     * @param config la configuration chargée
     * @return les paramètres résolus
     * @throws IllegalStateException    si aucune des deux sections n'est présente
     * @throws IllegalArgumentException si une valeur est absente ou invalide
     */
    public static PanelConfig from(Configuration config) {
        if (config != null && config.contains(SECTION + ".url")) {
            String rawType = config.getString(SECTION + ".type", PanelType.PTERODACTYL.name().toLowerCase());
            return new PanelConfig(
                    PanelType.from(rawType),
                    require(config.getString(SECTION + ".url"), SECTION + ".url"),
                    require(config.getString(SECTION + ".token"), SECTION + ".token"),
                    false);
        }

        if (config != null && config.contains(LEGACY_SECTION + ".url")) {
            // Format 2.x : le panel ne pouvait etre que Pterodactyl.
            return new PanelConfig(
                    PanelType.PTERODACTYL,
                    require(config.getString(LEGACY_SECTION + ".url"), LEGACY_SECTION + ".url"),
                    require(config.getString(LEGACY_SECTION + ".token"), LEGACY_SECTION + ".token"),
                    true);
        }

        throw new IllegalStateException(
                "Configuration du panel introuvable : le fichier config.yml doit contenir une section '"
                        + SECTION + "' avec 'url' et 'token'. Voir MIGRATION.md.");
    }

    private static String require(String value, String path) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("La valeur '" + path + "' est absente ou vide dans config.yml");
        }
        return value.trim();
    }

    /**
     * Message d'avertissement à journaliser quand l'ancien format est utilisé.
     *
     * @return le message de dépréciation
     */
    public static String legacyFormatWarning() {
        return "La section '" + LEGACY_SECTION + ":' de config.yml est depreciee et sera retiree dans une "
                + "version future. Renommez-la en '" + SECTION + ":' et ajoutez 'type: pterodactyl' "
                + "(ou 'type: pelican'). Voir MIGRATION.md.";
    }

    public PanelType getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }

    /**
     * Indique si la configuration provient de l'ancien format 2.x.
     *
     * @return true si l'ancienne section a été utilisée
     */
    public boolean isLegacyFormat() {
        return legacyFormat;
    }
}
