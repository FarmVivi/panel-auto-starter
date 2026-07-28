package fr.farmvivi.panelautostarter.message;

import net.md_5.bungee.config.Configuration;

import java.time.Duration;

/**
 * Réglages des retours visuels et sonores adressés aux joueurs.
 */
public final class MessageSettings {

    /**
     * Titre affiché à l'arrivée dans la file d'attente.
     *
     * @param enabled affiche le titre
     * @param fadeIn  durée d'apparition
     * @param stay    durée d'affichage plein
     * @param fadeOut durée de disparition
     */
    public record TitleSettings(boolean enabled, Duration fadeIn, Duration stay, Duration fadeOut) {
    }

    /**
     * Compte à rebours précédant la téléportation.
     *
     * @param enabled   active le décompte
     * @param seconds   nombre de secondes décomptées
     * @param tickSound son joué à chaque seconde, vide pour aucun
     * @param goSound   son joué au moment de la téléportation, vide pour aucun
     */
    public record CountdownSettings(boolean enabled, int seconds, String tickSound, String goSound) {
        /**
         * Indique si un son doit être joué à un moment quelconque du décompte.
         *
         * @return true si au moins un son est configuré
         */
        public boolean usesSound() {
            return !tickSound.isBlank() || !goSound.isBlank();
        }
    }

    private static final TitleSettings DEFAULT_TITLE = new TitleSettings(
            true, Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500));

    private static final CountdownSettings DEFAULT_COUNTDOWN = new CountdownSettings(
            true, 3, "block.note_block.hat", "block.note_block.pling");

    private final TitleSettings title;
    private final CountdownSettings countdown;

    private MessageSettings(TitleSettings title, CountdownSettings countdown) {
        this.title = title;
        this.countdown = countdown;
    }

    /**
     * Retourne les réglages par défaut.
     *
     * @return les réglages par défaut
     */
    public static MessageSettings defaults() {
        return new MessageSettings(DEFAULT_TITLE, DEFAULT_COUNTDOWN);
    }

    /**
     * Construit des réglages explicites, sans passer par un fichier.
     *
     * @param title     réglages du titre
     * @param countdown réglages du décompte
     * @return les réglages correspondants
     */
    public static MessageSettings of(TitleSettings title, CountdownSettings countdown) {
        return new MessageSettings(title, countdown);
    }

    /**
     * Lit les réglages depuis la configuration.
     *
     * @param config la configuration, éventuellement null
     * @return les réglages, complétés par les défauts
     */
    public static MessageSettings from(Configuration config) {
        if (config == null) {
            return defaults();
        }
        return new MessageSettings(
                new TitleSettings(
                        config.getBoolean("queue.title.enabled", DEFAULT_TITLE.enabled()),
                        millis(config, "queue.title.fade-in", DEFAULT_TITLE.fadeIn()),
                        millis(config, "queue.title.stay", DEFAULT_TITLE.stay()),
                        millis(config, "queue.title.fade-out", DEFAULT_TITLE.fadeOut())),
                new CountdownSettings(
                        config.getBoolean("queue.countdown.enabled", DEFAULT_COUNTDOWN.enabled()),
                        // Un decompte negatif n'a pas de sens ; 0 revient a le desactiver.
                        Math.max(0, config.getInt("queue.countdown.seconds", DEFAULT_COUNTDOWN.seconds())),
                        config.getString("queue.countdown.tick-sound", DEFAULT_COUNTDOWN.tickSound()).trim(),
                        config.getString("queue.countdown.go-sound", DEFAULT_COUNTDOWN.goSound()).trim()));
    }

    private static Duration millis(Configuration config, String path, Duration fallback) {
        return Duration.ofMillis(Math.max(0, config.getInt(path, (int) fallback.toMillis())));
    }

    public TitleSettings getTitle() {
        return title;
    }

    public CountdownSettings getCountdown() {
        return countdown;
    }
}
