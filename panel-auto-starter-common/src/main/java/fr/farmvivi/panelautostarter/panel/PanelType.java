package fr.farmvivi.panelautostarter.panel;

/**
 * Type de panel de gestion de serveurs supporté.
 */
public enum PanelType {
    PTERODACTYL,
    PELICAN;

    /**
     * Résout un type de panel depuis sa valeur de configuration, sans tenir
     * compte de la casse ni des espaces.
     *
     * @param value la valeur lue dans le fichier de configuration
     * @return le type correspondant
     * @throws IllegalArgumentException si la valeur ne correspond à aucun panel
     */
    public static PanelType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Le type de panel ne peut pas être null");
        }
        String normalized = value.trim().toUpperCase();
        for (PanelType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Type de panel inconnu : '" + value + "'. Valeurs acceptées : pterodactyl, pelican");
    }
}
