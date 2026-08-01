package fr.farmvivi.panelautostarter.command;

import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Réglages de la commande de retour au serveur d'attente.
 *
 * @param enabled enregistre la commande
 * @param name    son nom, sans barre oblique
 * @param aliases les autres noms sous lesquels elle répond
 */
public record LobbyCommandSettings(boolean enabled, String name, List<String> aliases) {

    public LobbyCommandSettings {
        aliases = List.copyOf(aliases);
    }

    /**
     * Réglages par défaut.
     *
     * @return les réglages par défaut
     */
    public static LobbyCommandSettings defaults() {
        return new LobbyCommandSettings(true, "lobby", List.of("hub"));
    }

    /**
     * Lit les réglages depuis la configuration.
     *
     * @param config la configuration, éventuellement null
     * @return les réglages, complétés par les défauts
     */
    public static LobbyCommandSettings from(Configuration config) {
        if (config == null) {
            return defaults();
        }
        LobbyCommandSettings fallback = defaults();

        String name = config.getString("queue.back-command.name", fallback.name());
        if (name == null || name.isBlank()) {
            // Un nom vide empecherait tout enregistrement : mieux vaut le defaut
            // qu'une commande qu'on ne peut pas taper.
            name = fallback.name();
        }
        name = name.trim();

        List<String> aliases = new ArrayList<>();
        List<String> configured = config.contains("queue.back-command.aliases")
                ? config.getStringList("queue.back-command.aliases")
                : fallback.aliases();
        for (String alias : configured) {
            // Un alias identique au nom ferait refuser l'enregistrement entier
            // sur certains proxys, pour un doublon sans intérêt.
            if (alias != null && !alias.isBlank()
                    && !alias.trim().equalsIgnoreCase(name)) {
                aliases.add(alias.trim().toLowerCase(Locale.ROOT));
            }
        }

        return new LobbyCommandSettings(
                config.getBoolean("queue.back-command.enabled", fallback.enabled()),
                name, aliases);
    }
}
