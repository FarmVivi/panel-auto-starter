package fr.farmvivi.panelautostarter.message;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Map;
import java.util.UUID;

/**
 * Textes adressés aux administrateurs.
 * <p>
 * Séparés de {@link Messages}, qui s'adresse aux joueurs : les deux publics
 * n'attendent pas la même chose. Un joueur veut savoir ce qui lui arrive, un
 * administrateur veut lire un état d'un coup d'œil.
 */
public final class AdminMessages {

    private static final Component PREFIX = Component.text("» ", NamedTextColor.DARK_GRAY);
    private static final Component INDENT = Component.text("  ");

    private AdminMessages() {
    }

    private static Component headline(Component content) {
        return PREFIX.append(content);
    }

    private static Component detail(Component content) {
        return INDENT.append(content.colorIfAbsent(NamedTextColor.GRAY));
    }

    private static Component server(String name) {
        return Component.text(name, NamedTextColor.YELLOW);
    }

    /**
     * Pastille d'état, lisible sans lire le mot qui la suit.
     *
     * @param status l'état du serveur
     * @return la pastille colorée
     */
    public static Component statusDot(MinecraftServerStatus status) {
        return switch (status) {
            case ONLINE -> Component.text("●", NamedTextColor.GREEN);
            case STARTING -> Component.text("◐", NamedTextColor.GOLD);
            default -> Component.text("○", NamedTextColor.DARK_GRAY);
        };
    }

    /**
     * Libellé d'état.
     *
     * @param status l'état du serveur
     * @return le libellé coloré
     */
    public static Component statusLabel(MinecraftServerStatus status) {
        return switch (status) {
            case ONLINE -> Component.text("en ligne", NamedTextColor.GREEN);
            case STARTING -> Component.text("démarrage", NamedTextColor.GOLD);
            default -> Component.text("éteint", NamedTextColor.GRAY);
        };
    }

    /**
     * En-tête de l'état d'ensemble.
     *
     * @param count le nombre de serveurs gérés
     * @return le message
     */
    public static Component statusHeader(int count) {
        return headline(Component.text("Serveurs gérés", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text(" (" + count + ")", NamedTextColor.DARK_GRAY)));
    }

    /**
     * Ligne d'état d'un serveur.
     *
     * @param server        le serveur
     * @param whitelistOn   true si sa liste blanche est active
     * @param whitelistSize le nombre d'inscrits
     * @return le message
     */
    public static Component statusLine(MinecraftServer server, boolean whitelistOn,
                                       int whitelistSize) {
        MinecraftServerStatus status = server.getStatus();
        Component line = INDENT.append(statusDot(status))
                .append(Component.text(" "))
                .append(server(server.getServer().getName()))
                .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                .append(statusLabel(status));

        int queued = server.getQueue().size();
        if (queued > 0) {
            line = line.append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(queued + " en file", NamedTextColor.GRAY));
        }

        if (whitelistOn) {
            line = line.append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("liste blanche " + whitelistSize, NamedTextColor.AQUA));
        }

        return line;
    }

    /**
     * Confirmation d'un démarrage demandé.
     *
     * @param serverName le nom du serveur
     * @return le message
     */
    public static Component starting(String serverName) {
        return headline(Component.text("Démarrage de ", NamedTextColor.GOLD).append(server(serverName)));
    }

    /**
     * Confirmation d'un arrêt demandé.
     *
     * @param serverName le nom du serveur
     * @return le message
     */
    public static Component stopping(String serverName) {
        return headline(Component.text("Arrêt de ", NamedTextColor.GOLD).append(server(serverName)));
    }

    /**
     * Refus opposé à une action sans objet, l'état visé étant déjà atteint.
     *
     * @param serverName le nom du serveur
     * @param state      l'état déjà atteint
     * @return le message
     */
    public static Component alreadyInState(String serverName, String state) {
        return headline(server(serverName)
                .append(Component.text(" est déjà " + state, NamedTextColor.GRAY)));
    }

    /**
     * Activation ou désactivation d'une liste blanche.
     *
     * @param serverName le nom du serveur
     * @param enabled    le nouvel état
     * @param size       le nombre d'inscrits
     * @return le message
     */
    public static Component whitelistToggled(String serverName, boolean enabled, int size) {
        Component headline = headline(Component.text("Liste blanche de ", NamedTextColor.AQUA)
                .append(server(serverName))
                .append(enabled
                        ? Component.text(" activée", NamedTextColor.GREEN)
                        : Component.text(" désactivée", NamedTextColor.GRAY)));

        // Une liste vide que l'on vient d'activer ferme le serveur a tout le
        // monde : le dire tout de suite evite un quart d'heure de perplexite.
        if (enabled && size == 0) {
            return headline.appendNewline().append(detail(Component
                    .text("Elle est vide : plus personne ne peut entrer", NamedTextColor.RED)));
        }
        return headline;
    }

    /**
     * Inscription sur une liste blanche.
     *
     * @param username   le joueur inscrit
     * @param serverName le nom du serveur
     * @param alreadyOn  true s'il y figurait déjà
     * @return le message
     */
    public static Component whitelistAdded(String username, String serverName, boolean alreadyOn) {
        if (alreadyOn) {
            return headline(Component.text(username, NamedTextColor.WHITE)
                    .append(Component.text(" figurait déjà sur la liste de ", NamedTextColor.GRAY))
                    .append(server(serverName)));
        }
        return headline(Component.text(username, NamedTextColor.WHITE)
                .append(Component.text(" ajouté à la liste de ", NamedTextColor.GREEN))
                .append(server(serverName)));
    }

    /**
     * Retrait d'une liste blanche.
     *
     * @param username   le joueur retiré
     * @param serverName le nom du serveur
     * @param wasOn      true s'il y figurait
     * @return le message
     */
    public static Component whitelistRemoved(String username, String serverName, boolean wasOn) {
        if (!wasOn) {
            return headline(Component.text(username, NamedTextColor.WHITE)
                    .append(Component.text(" ne figurait pas sur la liste de ", NamedTextColor.GRAY))
                    .append(server(serverName)));
        }
        return headline(Component.text(username, NamedTextColor.WHITE)
                .append(Component.text(" retiré de la liste de ", NamedTextColor.GOLD))
                .append(server(serverName)));
    }

    /**
     * Contenu d'une liste blanche.
     *
     * @param serverName le nom du serveur
     * @param enabled    true si elle est appliquée
     * @param players    les inscrits
     * @return le message
     */
    public static Component whitelistContents(String serverName, boolean enabled,
                                              Map<UUID, String> players) {
        Component message = headline(Component.text("Liste blanche de ", NamedTextColor.AQUA)
                .append(server(serverName))
                .append(Component.text(enabled ? " (active)" : " (inactive)",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));

        if (players.isEmpty()) {
            return message.appendNewline().append(detail(Component.text("Personne n'y figure")));
        }

        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            String name = entry.getValue() != null ? entry.getValue() : entry.getKey().toString();
            message = message.appendNewline().append(detail(Component.text("· " + name)));
        }
        return message;
    }

    /**
     * Serveur inconnu du plugin.
     *
     * @param name le nom saisi
     * @return le message
     */
    public static Component unknownServer(String name) {
        return headline(Component.text("Serveur inconnu : ", NamedTextColor.RED)
                .append(Component.text(name, NamedTextColor.WHITE)))
                .appendNewline()
                .append(detail(Component.text("Seuls les serveurs déclarés dans « servers » sont gérés")));
    }

    /**
     * Joueur introuvable.
     *
     * @param name le nom saisi
     * @return le message
     */
    public static Component unknownPlayer(String name) {
        return headline(Component.text("Joueur introuvable : ", NamedTextColor.RED)
                .append(Component.text(name, NamedTextColor.WHITE)))
                .appendNewline()
                .append(detail(Component.text("Indiquez un joueur connecté, un joueur déjà inscrit, "
                        + "ou son identifiant")));
    }

    /**
     * Droits insuffisants sur le serveur visé.
     *
     * @param serverName le nom du serveur
     * @return le message
     */
    public static Component notAdministrator(String serverName) {
        return headline(Component.text("Vous n'administrez pas ", NamedTextColor.RED)
                .append(server(serverName)));
    }

    /**
     * Aide de la commande.
     *
     * @param label le nom sous lequel la commande a été appelée
     * @return le message
     */
    public static Component usage(String label) {
        Component message = headline(Component.text("PanelAutoStarter", NamedTextColor.AQUA,
                TextDecoration.BOLD));
        String[][] lines = {
                {"menu", "ouvre le menu d'administration"},
                {"status [serveur]", "état des serveurs gérés"},
                {"start <serveur>", "démarre un serveur"},
                {"stop <serveur>", "arrête un serveur"},
                {"whitelist <serveur>", "affiche la liste blanche"},
                {"whitelist <serveur> on|off", "l'active ou la désactive"},
                {"whitelist <serveur> add|remove <joueur>", "y inscrit ou en retire"},
        };
        for (String[] line : lines) {
            message = message.appendNewline().append(INDENT
                    .append(Component.text("/" + label + " " + line[0], NamedTextColor.WHITE))
                    .append(Component.text(" — " + line[1], NamedTextColor.DARK_GRAY)));
        }
        return message;
    }
}
