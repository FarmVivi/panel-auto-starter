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

    // ===================== Retour au serveur d'attente =====================

    /**
     * Annonce du retour au serveur d'attente.
     *
     * @param lobbyDisplayName le nom affiché du serveur d'attente
     * @return le message
     */
    public static Component returningToLobby(String lobbyDisplayName) {
        return headline(Component.text("Retour vers ", NamedTextColor.GREEN)
                .append(server(lobbyDisplayName)));
    }

    /**
     * Réponse au joueur qui demande à revenir là où il se trouve déjà.
     *
     * @param lobbyDisplayName le nom affiché du serveur d'attente
     * @return le message
     */
    public static Component alreadyOnLobby(String lobbyDisplayName) {
        return headline(Component.text("Vous êtes déjà sur ", NamedTextColor.GRAY)
                .append(server(lobbyDisplayName)));
    }

    /**
     * Le serveur d'attente est introuvable : la configuration le nomme mal, ou
     * le proxy ne le déclare pas.
     *
     * @return le message
     */
    public static Component lobbyUnavailable() {
        return headline(Component.text("Le serveur d'attente est introuvable", NamedTextColor.RED))
                .appendNewline()
                .append(detail(Component.text("Signalez-le à un administrateur")));
    }

    // ===================== Accès refusé =====================

    /**
     * Refus opposé à un joueur qui n'a pas la permission d'entrer.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component accessDenied(String serverDisplayName) {
        return headline(Component.text("Accès refusé à ", NamedTextColor.RED)
                .append(server(serverDisplayName)))
                .appendNewline()
                .append(detail(Component.text("Vous n'avez pas la permission d'y entrer")));
    }

    /**
     * Refus opposé à un joueur absent de la liste blanche.
     * <p>
     * Distinct du refus de permission : une liste blanche se corrige en
     * demandant à être ajouté, là où une permission manquante relève des
     * groupes. Confondre les deux enverrait le joueur frapper à la mauvaise
     * porte.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component notWhitelisted(String serverDisplayName) {
        return headline(server(serverDisplayName)
                .append(Component.text(" est sur liste blanche", NamedTextColor.RED)))
                .appendNewline()
                .append(detail(Component.text("Demandez à un administrateur de vous y ajouter")));
    }

    // ===================== Barre d'action =====================

    /**
     * Séparateur des segments de la barre d'action. Assez discret pour ne pas
     * concurrencer l'information qu'il sépare.
     */
    private static final Component SEPARATOR = Component.text("  ·  ", NamedTextColor.DARK_GRAY);

    /**
     * Images successives de l'indicateur d'activité.
     * <p>
     * La barre d'action étant réémise à intervalle régulier, faire tourner un
     * point donne au joueur la seule information qu'un texte fixe ne peut pas
     * lui donner : que le plugin travaille toujours pour lui. Les caractères
     * sont choisis dans la police par défaut du jeu, contrairement aux
     * fuseaux braille habituels que le client rend en carrés.
     */
    private static final String[] SPINNER = {"●○○", "○●○", "○○●", "○●○"};

    /**
     * Ligne affichée dans la barre d'action tant que le joueur attend.
     *
     * @param serverDisplayName le nom affiché du serveur attendu
     * @param position          la place du joueur, à partir de 1
     * @param size              la taille de la file
     * @param starting          true si le serveur est en cours de démarrage,
     *                          false s'il est prêt et que la téléportation approche
     * @param showPosition      affiche la place dans la file
     * @param frame             le numéro de rafraîchissement, qui anime l'indicateur
     * @return la ligne à afficher
     */
    public static Component queueActionBar(String serverDisplayName, int position, int size,
                                           boolean starting, boolean showPosition, int frame) {
        Component line = Component.text(SPINNER[Math.floorMod(frame, SPINNER.length)],
                        starting ? NamedTextColor.GOLD : NamedTextColor.GREEN)
                .append(Component.text(" "))
                .append(server(serverDisplayName));

        line = line.append(SEPARATOR).append(starting
                ? Component.text("démarrage", NamedTextColor.GOLD)
                : Component.text("prêt", NamedTextColor.GREEN));

        // Annoncer « 1/1 » à quelqu'un qui attend seul n'apprend rien et occupe
        // de la place pour rien.
        if (showPosition && size > 1) {
            line = line.append(SEPARATOR)
                    .append(Component.text(position, NamedTextColor.WHITE))
                    .append(Component.text("/" + size, NamedTextColor.GRAY));
        }

        return line;
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

    // ===================== Serveur arrêté sous les pieds du joueur =====================

    /**
     * Chat adressé au joueur ramené au serveur d'attente parce que le sien
     * s'est arrêté.
     *
     * @param serverDisplayName  le nom affiché du serveur disparu
     * @param queueDisplayName   le nom affiché du serveur d'attente
     * @return le message
     */
    public static Component sentBackToQueue(String serverDisplayName, String queueDisplayName) {
        return headline(server(serverDisplayName)
                .append(Component.text(" s'est arrêté", NamedTextColor.RED)))
                .appendNewline()
                .append(detail(Component.text("Vous avez été ramené sur ")
                        .append(server(queueDisplayName))));
    }

    /**
     * Titre affiché au joueur ramené au serveur d'attente.
     *
     * @return le titre
     */
    public static Component serverStoppedTitle() {
        return Component.text("Serveur arrêté", NamedTextColor.RED, TextDecoration.BOLD);
    }

    /**
     * Sous-titre affiché au joueur ramené au serveur d'attente.
     *
     * @param serverDisplayName le nom affiché du serveur disparu
     * @return le sous-titre
     */
    public static Component serverStoppedSubtitle(String serverDisplayName) {
        return server(serverDisplayName)
                .append(Component.text(" n'est plus disponible", NamedTextColor.GRAY));
    }
}
