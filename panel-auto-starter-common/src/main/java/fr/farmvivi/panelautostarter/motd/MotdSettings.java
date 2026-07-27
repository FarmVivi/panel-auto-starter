package fr.farmvivi.panelautostarter.motd;

import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Réglages d'affichage du MOTD, indépendants pour chaque état du serveur.
 * <p>
 * Chaque champ de la réponse au ping se règle séparément, ce qui permet de
 * reconstituer aussi bien le comportement de la 2.x qu'un affichage qui
 * n'altère rien. Les combinaisons courantes sont documentées dans
 * {@code config.yml}.
 * <p>
 * Une valeur inconnue n'empêche jamais le plugin de démarrer : le défaut
 * s'applique et l'anomalie est signalée via {@link #getWarnings()}. Un réglage
 * cosmétique mal orthographié ne doit pas priver les joueurs de leurs serveurs.
 */
public final class MotdSettings {

    /**
     * Origine de la description affichée.
     */
    public enum Description {
        /** Le MOTD du serveur, mémorisé lors de son dernier passage en ligne. */
        CACHED,
        /** Un texte généré par le plugin, indiquant l'état du serveur. */
        PLUGIN,
        /** La réponse du proxy, laissée telle quelle. */
        PROXY
    }

    /**
     * Origine de l'icône affichée.
     */
    public enum Favicon {
        /** Le favicon du serveur, mémorisé puis décoré selon les réglages. */
        CACHED,
        /** L'image fournie avec le plugin pour cet état. */
        BUNDLED,
        /** La réponse du proxy, laissée telle quelle. */
        PROXY
    }

    /**
     * Nombre de joueurs connectés affiché.
     */
    public enum Players {
        /** Zéro : le serveur n'accueille personne. */
        ZERO,
        /** La taille de la file d'attente. */
        QUEUE,
        /** La réponse du proxy, laissée telle quelle. */
        PROXY
    }

    /**
     * Nombre maximum de joueurs affiché.
     */
    public enum MaxPlayers {
        /** Zéro, ce qui donne un « 0/0 » signalant l'indisponibilité. */
        ZERO,
        /** Le maximum mémorisé, ce qui donne un « 0/20 » d'apparence normale. */
        CACHED,
        /** La réponse du proxy, laissée telle quelle. */
        PROXY
    }

    /**
     * Contenu affiché au survol du compteur de joueurs.
     */
    public enum Hover {
        /** Une explication générée par le plugin, et la file d'attente s'il y en a une. */
        PLUGIN,
        /** Rien. */
        NONE,
        /** La réponse du proxy, laissée telle quelle. */
        PROXY
    }

    /**
     * Étiquette rouge affichée <em>à la place</em> du compteur de joueurs.
     * <p>
     * Le client Minecraft remplace le compteur par le nom de version quand
     * celle-ci ne correspond pas à la sienne. La 2.x détournait ce mécanisme
     * pour écrire « Hors-ligne » en rouge. Les deux occupent le même
     * emplacement : activer l'étiquette rend le compteur invisible.
     */
    public enum VersionLabel {
        /** Aucune étiquette : le compteur de joueurs reste visible. */
        NONE,
        /** L'étiquette d'état remplace le compteur. */
        TEXT
    }

    /**
     * Réglages appliqués à un état donné.
     *
     * @param description    origine de la description
     * @param favicon        origine de l'icône
     * @param faviconGrayscale désature l'icône ; ne s'applique qu'à {@link Favicon#CACHED}
     * @param faviconBadge   superpose la pastille d'état ; ne s'applique qu'à {@link Favicon#CACHED}
     * @param players        nombre de joueurs connectés
     * @param maxPlayers     nombre maximum de joueurs
     * @param hover          contenu au survol
     * @param versionLabel   étiquette de version
     */
    public record State(Description description, Favicon favicon,
                        boolean faviconGrayscale, boolean faviconBadge,
                        Players players, MaxPlayers maxPlayers,
                        Hover hover, VersionLabel versionLabel) {
    }

    private static final State DEFAULT_OFFLINE = new State(
            Description.CACHED, Favicon.CACHED, true, true,
            Players.ZERO, MaxPlayers.ZERO, Hover.PLUGIN, VersionLabel.NONE);

    private static final State DEFAULT_STARTING = new State(
            Description.CACHED, Favicon.CACHED, false, true,
            Players.ZERO, MaxPlayers.ZERO, Hover.PLUGIN, VersionLabel.NONE);

    private final State offline;
    private final State starting;
    private final List<String> warnings;

    private MotdSettings(State offline, State starting, List<String> warnings) {
        this.offline = offline;
        this.starting = starting;
        this.warnings = Collections.unmodifiableList(warnings);
    }

    /**
     * Retourne les réglages par défaut, sans lire de configuration.
     *
     * @return les réglages par défaut
     */
    public static MotdSettings defaults() {
        return new MotdSettings(DEFAULT_OFFLINE, DEFAULT_STARTING, new ArrayList<>());
    }

    /**
     * Construit des réglages explicites, sans passer par un fichier.
     *
     * @param offline  réglages de l'état hors-ligne
     * @param starting réglages de l'état démarrage
     * @return les réglages correspondants
     */
    public static MotdSettings of(State offline, State starting) {
        return new MotdSettings(offline, starting, new ArrayList<>());
    }

    /**
     * Réglages par défaut de l'état hors-ligne, utiles comme point de départ.
     *
     * @return les réglages par défaut hors-ligne
     */
    public static State defaultOfflineState() {
        return DEFAULT_OFFLINE;
    }

    /**
     * Réglages par défaut de l'état démarrage, utiles comme point de départ.
     *
     * @return les réglages par défaut en démarrage
     */
    public static State defaultStartingState() {
        return DEFAULT_STARTING;
    }

    /**
     * Lit les réglages depuis la configuration.
     *
     * @param config la configuration, éventuellement null
     * @return les réglages, complétés par les défauts
     */
    public static MotdSettings from(Configuration config) {
        List<String> warnings = new ArrayList<>();
        if (config == null) {
            return new MotdSettings(DEFAULT_OFFLINE, DEFAULT_STARTING, warnings);
        }
        return new MotdSettings(
                readState(config, "motd.offline", DEFAULT_OFFLINE, warnings),
                readState(config, "motd.starting", DEFAULT_STARTING, warnings),
                warnings);
    }

    private static State readState(Configuration config, String path, State fallback, List<String> warnings) {
        return new State(
                readEnum(config, path + ".description", Description.class, fallback.description(), warnings),
                readEnum(config, path + ".favicon", Favicon.class, fallback.favicon(), warnings),
                config.getBoolean(path + ".favicon-grayscale", fallback.faviconGrayscale()),
                config.getBoolean(path + ".favicon-badge", fallback.faviconBadge()),
                readEnum(config, path + ".players", Players.class, fallback.players(), warnings),
                readEnum(config, path + ".max-players", MaxPlayers.class, fallback.maxPlayers(), warnings),
                readEnum(config, path + ".hover", Hover.class, fallback.hover(), warnings),
                readEnum(config, path + ".version-label", VersionLabel.class, fallback.versionLabel(), warnings));
    }

    private static <E extends Enum<E>> E readEnum(Configuration config, String path, Class<E> type,
                                                  E fallback, List<String> warnings) {
        String raw = config.getString(path, null);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (E candidate : type.getEnumConstants()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        warnings.add("Valeur inconnue pour '" + path + "' : '" + raw.trim() + "'. "
                + "Valeurs acceptees : " + accepted(type) + ". Valeur par defaut appliquee : " + label(fallback) + ".");
        return fallback;
    }

    private static <E extends Enum<E>> String accepted(Class<E> type) {
        StringBuilder sb = new StringBuilder();
        for (E candidate : type.getEnumConstants()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(label(candidate));
        }
        return sb.toString();
    }

    private static String label(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Retourne les réglages applicables à un état.
     *
     * @param status l'état du serveur
     * @return les réglages correspondants, ou null si l'état n'est pas géré ici
     */
    public State forStatus(MinecraftServerStatus status) {
        return switch (status) {
            case OFFLINE -> offline;
            case STARTING -> starting;
            case ONLINE -> null;
        };
    }

    public State getOffline() {
        return offline;
    }

    public State getStarting() {
        return starting;
    }

    /**
     * Anomalies rencontrées à la lecture, chacune ayant été remplacée par son
     * défaut.
     *
     * @return les avertissements, éventuellement vides
     */
    public List<String> getWarnings() {
        return warnings;
    }
}
