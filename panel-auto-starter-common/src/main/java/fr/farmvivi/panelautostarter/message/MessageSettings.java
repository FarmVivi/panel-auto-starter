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
     * @param sound   son accompagnant le titre
     */
    public record TitleSettings(boolean enabled, Duration fadeIn, Duration stay, Duration fadeOut,
                                SoundSpec sound) {
    }

    /**
     * Compte à rebours précédant la téléportation.
     *
     * @param enabled   active le décompte
     * @param seconds   nombre de secondes décomptées
     * @param tickSound son joué à chaque seconde
     * @param goSound   son joué au moment de la téléportation
     */
    public record CountdownSettings(boolean enabled, int seconds, SoundSpec tickSound, SoundSpec goSound) {
        /**
         * Indique si un son doit être joué à un moment quelconque du décompte.
         *
         * @return true si au moins un son est configuré
         */
        public boolean usesSound() {
            return !tickSound.isSilent() || !goSound.isSilent();
        }
    }

    /**
     * Retour adressé au joueur ramené au serveur d'attente parce que le sien
     * s'est arrêté.
     *
     * @param enabled affiche le retour
     * @param sound   son l'accompagnant
     */
    public record KickFeedbackSettings(boolean enabled, SoundSpec sound) {
    }

    /**
     * Barre d'action rappelant au joueur ce qu'il attend.
     * <p>
     * Elle est réémise à intervalle régulier : le client efface la barre
     * d'action de lui-même au bout de quelques secondes, un envoi unique ne
     * tiendrait donc pas le temps d'un démarrage.
     *
     * @param enabled      affiche la barre d'action
     * @param interval     délai entre deux rafraîchissements
     * @param showPosition affiche la place du joueur dans la file
     */
    public record ActionBarSettings(boolean enabled, Duration interval, boolean showPosition) {
    }

    private static final ActionBarSettings DEFAULT_ACTION_BAR = new ActionBarSettings(
            true, Duration.ofSeconds(1), true);

    private static final KickFeedbackSettings DEFAULT_KICK_FEEDBACK = new KickFeedbackSettings(
            true, new SoundSpec("block.note_block.bass", 1.0f, 0.8f));

    private static final TitleSettings DEFAULT_TITLE = new TitleSettings(
            true, Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500),
            new SoundSpec("block.note_block.bell", 1.0f, 1.0f));

    private static final CountdownSettings DEFAULT_COUNTDOWN = new CountdownSettings(
            true, 3,
            new SoundSpec("block.note_block.hat", 0.5f, 1.0f),
            new SoundSpec("entity.experience_orb.pickup", 0.7f, 1.2f));

    private final TitleSettings title;
    private final CountdownSettings countdown;
    private final KickFeedbackSettings kickFeedback;
    private final ActionBarSettings actionBar;

    private MessageSettings(TitleSettings title, CountdownSettings countdown,
                            KickFeedbackSettings kickFeedback, ActionBarSettings actionBar) {
        this.title = title;
        this.countdown = countdown;
        this.kickFeedback = kickFeedback;
        this.actionBar = actionBar;
    }

    /**
     * Retourne les réglages par défaut.
     *
     * @return les réglages par défaut
     */
    public static MessageSettings defaults() {
        return new MessageSettings(DEFAULT_TITLE, DEFAULT_COUNTDOWN, DEFAULT_KICK_FEEDBACK,
                DEFAULT_ACTION_BAR);
    }

    /**
     * Construit des réglages explicites, sans passer par un fichier.
     *
     * @param title     réglages du titre
     * @param countdown réglages du décompte
     * @return les réglages correspondants
     */
    public static MessageSettings of(TitleSettings title, CountdownSettings countdown) {
        return new MessageSettings(title, countdown, DEFAULT_KICK_FEEDBACK, DEFAULT_ACTION_BAR);
    }

    /**
     * Construit des réglages explicites, retour d'éjection compris.
     *
     * @param title        réglages du titre
     * @param countdown    réglages du décompte
     * @param kickFeedback réglages du retour après éjection
     * @return les réglages correspondants
     */
    public static MessageSettings of(TitleSettings title, CountdownSettings countdown,
                                     KickFeedbackSettings kickFeedback) {
        return new MessageSettings(title, countdown, kickFeedback, DEFAULT_ACTION_BAR);
    }

    /**
     * Construit des réglages explicites, barre d'action comprise.
     *
     * @param title        réglages du titre
     * @param countdown    réglages du décompte
     * @param kickFeedback réglages du retour après éjection
     * @param actionBar    réglages de la barre d'action
     * @return les réglages correspondants
     */
    public static MessageSettings of(TitleSettings title, CountdownSettings countdown,
                                     KickFeedbackSettings kickFeedback, ActionBarSettings actionBar) {
        return new MessageSettings(title, countdown, kickFeedback, actionBar);
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
                        millis(config, "queue.title.fade-out", DEFAULT_TITLE.fadeOut()),
                        SoundSpec.from(config, "queue.title.sound", DEFAULT_TITLE.sound())),
                new CountdownSettings(
                        config.getBoolean("queue.countdown.enabled", DEFAULT_COUNTDOWN.enabled()),
                        // Un decompte negatif n'a pas de sens ; 0 revient a le desactiver.
                        Math.max(0, config.getInt("queue.countdown.seconds", DEFAULT_COUNTDOWN.seconds())),
                        SoundSpec.from(config, "queue.countdown.tick-sound", DEFAULT_COUNTDOWN.tickSound()),
                        SoundSpec.from(config, "queue.countdown.go-sound", DEFAULT_COUNTDOWN.goSound())),
                new KickFeedbackSettings(
                        config.getBoolean("queue.kick-feedback.enabled", DEFAULT_KICK_FEEDBACK.enabled()),
                        SoundSpec.from(config, "queue.kick-feedback.sound", DEFAULT_KICK_FEEDBACK.sound())),
                new ActionBarSettings(
                        config.getBoolean("queue.actionbar.enabled", DEFAULT_ACTION_BAR.enabled()),
                        // Un intervalle nul ou negatif ferait tourner le
                        // planificateur en boucle ; un intervalle trop long
                        // laisserait la barre s'effacer entre deux envois, le
                        // client l'oubliant au bout de quelques secondes.
                        clamp(millis(config, "queue.actionbar.interval", DEFAULT_ACTION_BAR.interval()),
                                MIN_ACTION_BAR_INTERVAL, MAX_ACTION_BAR_INTERVAL),
                        config.getBoolean("queue.actionbar.show-position",
                                DEFAULT_ACTION_BAR.showPosition())));
    }

    private static final Duration MIN_ACTION_BAR_INTERVAL = Duration.ofMillis(250);
    private static final Duration MAX_ACTION_BAR_INTERVAL = Duration.ofSeconds(3);

    private static Duration clamp(Duration value, Duration min, Duration max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        return value.compareTo(max) > 0 ? max : value;
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

    /**
     * Retour adressé au joueur ramené au serveur d'attente.
     *
     * @return les réglages correspondants
     */
    public KickFeedbackSettings getKickFeedback() {
        return kickFeedback;
    }

    /**
     * Barre d'action rappelant au joueur ce qu'il attend.
     *
     * @return les réglages correspondants
     */
    public ActionBarSettings getActionBar() {
        return actionBar;
    }
}
