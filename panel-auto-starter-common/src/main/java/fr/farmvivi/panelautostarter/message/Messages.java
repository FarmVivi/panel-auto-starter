package fr.farmvivi.panelautostarter.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Tous les textes destinés aux joueurs, réunis en un seul endroit.
 * <p>
 * Les regrouper ici évite qu'ils dérivent chacun de leur côté au fil des
 * listeners, et donne une hiérarchie visuelle commune : une ligne d'en-tête qui
 * annonce ce qui se passe, une ligne secondaire indentée qui précise. Le préfixe
 * et l'indentation alignent les deux, de sorte que plusieurs messages
 * successifs dans le chat se lisent comme un bloc plutôt que comme du bruit.
 * <p>
 * <strong>Le style vit ici, le texte dans les fichiers de langue.</strong> Une
 * couleur relève du design et vaut pour toutes les langues ; une phrase
 * appartient à la sienne. Les messages sont donc construits comme des
 * composants <em>traduisibles</em>, non résolus : ils ne prennent leur forme
 * définitive qu'au moment d'être remis à quelqu'un, dans la langue de
 * celui-ci.
 * <p>
 * Chaque phrase est une clé entière, jamais des fragments recollés : l'ordre
 * des mots change d'une langue à l'autre, et « {0} démarre » ne se traduit pas
 * en assemblant « {0} » puis « démarre ».
 */
public final class Messages {

    /** Préfixe commun des clés, qui distingue les nôtres de celles du jeu. */
    private static final String KEY = "panelautostarter.";

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

    private static Component text(String key, TextColor color, ComponentLike... args) {
        return Component.translatable(KEY + key, args).color(color);
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
        Component headline = headline(text("queue.starting", NamedTextColor.GOLD,
                server(serverDisplayName)));

        // La place n'est annoncee qu'a partir de deux joueurs : dire « 1/1 » a
        // quelqu'un qui attend seul n'apprend rien.
        Component detail = queueSize > 1
                ? detail(text("queue.position", NamedTextColor.GRAY,
                Component.text(queuePosition + "/" + queueSize, NamedTextColor.WHITE)))
                : detail(text("queue.soon", NamedTextColor.GRAY));

        return headline.appendNewline().append(detail);
    }

    /**
     * Annonce de la téléportation imminente.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component teleporting(String serverDisplayName) {
        return headline(text("queue.teleporting", NamedTextColor.GREEN,
                server(serverDisplayName)));
    }

    /**
     * Échec de la téléportation, le serveur ne s'étant pas rendu disponible.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component teleportFailed(String serverDisplayName) {
        return headline(text("queue.failed", NamedTextColor.RED, server(serverDisplayName)))
                .appendNewline()
                .append(detail(text("queue.failed.detail", NamedTextColor.GRAY)));
    }

    // ===================== Retour au serveur d'attente =====================

    /**
     * Annonce du retour au serveur d'attente.
     *
     * @param lobbyDisplayName le nom affiché du serveur d'attente
     * @return le message
     */
    public static Component returningToLobby(String lobbyDisplayName) {
        return headline(text("lobby.returning", NamedTextColor.GREEN, server(lobbyDisplayName)));
    }

    /**
     * Réponse au joueur qui demande à revenir là où il se trouve déjà.
     *
     * @param lobbyDisplayName le nom affiché du serveur d'attente
     * @return le message
     */
    public static Component alreadyOnLobby(String lobbyDisplayName) {
        return headline(text("lobby.already", NamedTextColor.GRAY, server(lobbyDisplayName)));
    }

    /**
     * Le serveur d'attente est introuvable : la configuration le nomme mal, ou
     * le proxy ne le déclare pas.
     *
     * @return le message
     */
    public static Component lobbyUnavailable() {
        return headline(text("lobby.unavailable", NamedTextColor.RED))
                .appendNewline()
                .append(detail(text("lobby.unavailable.detail", NamedTextColor.GRAY)));
    }

    // ===================== Accès refusé =====================

    /**
     * Refus opposé à un joueur qui n'a pas la permission d'entrer.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le message
     */
    public static Component accessDenied(String serverDisplayName) {
        return headline(text("access.denied", NamedTextColor.RED, server(serverDisplayName)))
                .appendNewline()
                .append(detail(text("access.denied.detail", NamedTextColor.GRAY)));
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
        return headline(text("access.whitelist", NamedTextColor.RED, server(serverDisplayName)))
                .appendNewline()
                .append(detail(text("access.whitelist.detail", NamedTextColor.GRAY)));
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
                ? text("actionbar.starting", NamedTextColor.GOLD)
                : text("actionbar.ready", NamedTextColor.GREEN));

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
        return text("title.starting", NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    }

    /**
     * Sous-titre affiché à l'arrivée dans la file d'attente.
     *
     * @param serverDisplayName le nom affiché du serveur
     * @return le sous-titre
     */
    public static Component startingSubtitle(String serverDisplayName) {
        return text("title.starting.subtitle", NamedTextColor.GRAY, server(serverDisplayName));
    }

    /**
     * Titre d'un tic du compte à rebours.
     * <p>
     * La couleur passe du rouge au vert à mesure que l'échéance approche, ce qui
     * rend le décompte lisible d'un coup d'œil sans lire le chiffre. Un chiffre
     * ne se traduisant pas, il n'y a ici aucune clé.
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
        return text("title.countdown.subtitle", NamedTextColor.GRAY, server(serverDisplayName));
    }

    /**
     * Titre affiché au moment de la téléportation.
     *
     * @return le titre
     */
    public static Component readyTitle() {
        return text("title.ready", NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
    }

    // ===================== Serveur arrêté sous les pieds du joueur =====================

    /**
     * Chat adressé au joueur ramené au serveur d'attente parce que le sien
     * s'est arrêté.
     *
     * @param serverDisplayName le nom affiché du serveur disparu
     * @param queueDisplayName  le nom affiché du serveur d'attente
     * @return le message
     */
    public static Component sentBackToQueue(String serverDisplayName, String queueDisplayName) {
        return headline(text("kicked.headline", NamedTextColor.RED, server(serverDisplayName)))
                .appendNewline()
                .append(detail(text("kicked.detail", NamedTextColor.GRAY,
                        server(queueDisplayName))));
    }

    /**
     * Titre affiché au joueur ramené au serveur d'attente.
     *
     * @return le titre
     */
    public static Component serverStoppedTitle() {
        return text("kicked.title", NamedTextColor.RED).decorate(TextDecoration.BOLD);
    }

    /**
     * Sous-titre affiché au joueur ramené au serveur d'attente.
     *
     * @param serverDisplayName le nom affiché du serveur disparu
     * @return le sous-titre
     */
    public static Component serverStoppedSubtitle(String serverDisplayName) {
        return text("kicked.subtitle", NamedTextColor.GRAY, server(serverDisplayName));
    }
}
