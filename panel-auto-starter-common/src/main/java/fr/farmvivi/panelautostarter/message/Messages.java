package fr.farmvivi.panelautostarter.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Tous les textes destinés aux joueurs, réunis en un seul endroit.
 * <p>
 * Les regrouper ici évite qu'ils dérivent chacun de leur côté au fil des
 * listeners, et donne une hiérarchie visuelle commune : une ligne d'en-tête qui
 * annonce ce qui se passe, une ligne secondaire indentée qui précise. Le préfixe
 * et l'indentation alignent les deux, de sorte que plusieurs messages
 * successifs dans le chat se lisent comme un bloc plutôt que comme du bruit.
 */
public final class Messages {
    /**
     * Chevron discret ouvrant chaque message, pour distinguer le plugin du
     * reste du chat sans imposer un préfixe bavard.
     */
    private static final Component PREFIX = Component.text("» ", NamedTextColor.DARK_GRAY);

    /**
     * Alignement des lignes secondaires sous le texte du préfixe.
     */
    private static final Component INDENT = Component.text("  ");

    private Messages() {
    }

    private static Component server(String displayName) {
        return Component.text(displayName, NamedTextColor.YELLOW);
    }

    private static Component headline(Component content) {
        return PREFIX.append(content);
    }

    private static Component detail(Component content) {
        return INDENT.append(content.colorIfAbsent(NamedTextColor.GRAY));
    }

    // ===================== Chat =====================

    /**
     * Annonce du démarrage, avec la place du joueur dans la file.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @param queuePosition     la place du joueur, à partir de 1
     * @param queueSize         la taille de la file
     * @return le message
     */
    public static Component joinedQueue(String serverDisplayName, int queuePosition, int queueSize) {
        Component headline = headline(server(serverDisplayName)
                .append(Component.text(" démarre", NamedTextColor.GOLD))
                .append(Component.text("...", NamedTextColor.DARK_GRAY)));

        Component detail = queueSize > 1
                ? detail(Component.text("Vous êtes ")
                .append(Component.text(queuePosition + "/" + queueSize, NamedTextColor.WHITE))
                .append(Component.text(" dans la file d'attente")))
                : detail(Component.text("Vous serez téléporté dès qu'il est prêt"));

        return headline.appendNewline().append(detail);
    }

    /**
     * Annonce de la téléportation imminente.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component teleporting(String serverDisplayName) {
        return headline(Component.text("Téléportation vers ", NamedTextColor.GREEN)
                .append(server(serverDisplayName)));
    }

    /**
     * Échec de la téléportation, le serveur ne s'étant pas rendu disponible.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component teleportFailed(String serverDisplayName) {
        return headline(server(serverDisplayName)
                .append(Component.text(" n'a pas pu démarrer", NamedTextColor.RED)))
                .appendNewline()
                .append(detail(Component.text("Réessayez dans un instant")));
    }

    // ===================== Titres =====================

    /**
     * Titre affiché à l'arrivée dans la file d'attente.
     *
     * @return le titre
     */
    public static Component startingTitle() {
        return Component.text("Démarrage", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    /**
     * Sous-titre affiché à l'arrivée dans la file d'attente.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le sous-titre
     */
    public static Component startingSubtitle(String serverDisplayName) {
        return server(serverDisplayName)
                .append(Component.text(" se prépare", NamedTextColor.GRAY));
    }

    /**
     * Titre d'un tic du compte à rebours.
     * <p>
     * La couleur passe du rouge au vert à mesure que l'échéance approche, ce qui
     * rend le décompte lisible d'un coup d'œil sans lire le chiffre.
     *
     * @param secondsLeft le nombre de secondes restantes
     * @return le titre
     */
    public static Component countdownTitle(int secondsLeft) {
        NamedTextColor color = switch (secondsLeft) {
            case 1 -> NamedTextColor.GREEN;
            case 2 -> NamedTextColor.YELLOW;
            default -> NamedTextColor.RED;
        };
        return Component.text(secondsLeft, color, TextDecoration.BOLD);
    }

    /**
     * Sous-titre du compte à rebours.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le sous-titre
     */
    public static Component countdownSubtitle(String serverDisplayName) {
        return Component.text("Téléportation vers ", NamedTextColor.GRAY)
                .append(server(serverDisplayName));
    }

    /**
     * Titre affiché au moment de la téléportation.
     *
     * @return le titre
     */
    public static Component readyTitle() {
        return Component.text("C'est parti !", NamedTextColor.GREEN, TextDecoration.BOLD);
    }
}
