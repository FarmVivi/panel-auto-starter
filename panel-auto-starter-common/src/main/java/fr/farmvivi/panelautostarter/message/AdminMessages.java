package fr.farmvivi.panelautostarter.message;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Map;
import java.util.UUID;

/**
 * Textes adressés aux administrateurs.
 * <p>
 * Séparés de {@link Messages}, qui s'adresse aux joueurs : les deux publics
 * n'attendent pas la même chose. Un joueur veut savoir ce qui lui arrive, un
 * administrateur veut lire un état d'un coup d'œil.
 * <p>
 * Traduits de la même façon, et pour la même raison : un administrateur n'est
 * pas plus tenu de lire le français qu'un joueur.
 */
public final class AdminMessages {

    private static final String KEY = "panelautostarter.admin.";

    private static final Component PREFIX = Component.text("» ", NamedTextColor.DARK_GRAY);
    private static final Component INDENT = Component.text("  ");
    private static final Component SEPARATOR = Component.text(" · ", NamedTextColor.DARK_GRAY);

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

    private static Component text(String key, TextColor color, ComponentLike... args) {
        return Component.translatable(KEY + key, args).color(color);
    }

    /**
     * Pastille d'état, lisible sans lire le mot qui la suit. Un symbole ne se
     * traduit pas.
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
            case ONLINE -> text("state.online", NamedTextColor.GREEN);
            case STARTING -> text("state.starting", NamedTextColor.GOLD);
            default -> text("state.offline", NamedTextColor.GRAY);
        };
    }

    /**
     * En-tête de l'état d'ensemble.
     *
     * @param count le nombre de serveurs gérés
     * @return le message
     */
    public static Component statusHeader(int count) {
        return headline(text("status.header", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD)
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
            line = line.append(SEPARATOR).append(text("status.queued", NamedTextColor.GRAY,
                    Component.text(queued)));
        }

        if (whitelistOn) {
            line = line.append(SEPARATOR).append(text("status.whitelist", NamedTextColor.AQUA,
                    Component.text(whitelistSize)));
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
        return headline(text("starting", NamedTextColor.GOLD, server(serverName)));
    }

    /**
     * Confirmation d'un arrêt demandé.
     *
     * @param serverName le nom du serveur
     * @return le message
     */
    public static Component stopping(String serverName) {
        return headline(text("stopping", NamedTextColor.GOLD, server(serverName)));
    }

    /**
     * Refus opposé à une action sans objet, l'état visé étant déjà atteint.
     * <p>
     * L'état est passé comme composant traduisible plutôt que comme mot :
     * l'insérer tel quel obligerait chaque langue à s'accommoder de la
     * grammaire de la nôtre.
     *
     * @param serverName le nom du serveur
     * @param state      l'état déjà atteint
     * @return le message
     */
    public static Component alreadyInState(String serverName, Component state) {
        return headline(text("already", NamedTextColor.GRAY, server(serverName), state));
    }

    /**
     * Libellé d'un état déjà atteint, pour {@link #alreadyInState}.
     *
     * @param status l'état
     * @return le libellé
     */
    public static Component stateWord(MinecraftServerStatus status) {
        return statusLabel(status);
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
        Component headline = headline(enabled
                ? text("whitelist.enabled", NamedTextColor.GREEN, server(serverName))
                : text("whitelist.disabled", NamedTextColor.GRAY, server(serverName)));

        // Une liste vide que l'on vient d'activer ferme le serveur a tout le
        // monde : le dire tout de suite evite un quart d'heure de perplexite.
        if (enabled && size == 0) {
            return headline.appendNewline()
                    .append(detail(text("whitelist.empty", NamedTextColor.RED)));
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
        Component player = Component.text(username, NamedTextColor.WHITE);
        return headline(alreadyOn
                ? text("whitelist.already", NamedTextColor.GRAY, player, server(serverName))
                : text("whitelist.added", NamedTextColor.GREEN, player, server(serverName)));
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
        Component player = Component.text(username, NamedTextColor.WHITE);
        return headline(wasOn
                ? text("whitelist.removed", NamedTextColor.GOLD, player, server(serverName))
                : text("whitelist.absent", NamedTextColor.GRAY, player, server(serverName)));
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
        Component message = headline(text("whitelist.header", NamedTextColor.AQUA,
                server(serverName))
                .append(enabled
                        ? text("whitelist.active", NamedTextColor.GREEN)
                        : text("whitelist.inactive", NamedTextColor.DARK_GRAY)));

        if (players.isEmpty()) {
            return message.appendNewline()
                    .append(detail(text("whitelist.nobody", NamedTextColor.GRAY)));
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
        return headline(text("unknown.server", NamedTextColor.RED,
                Component.text(name, NamedTextColor.WHITE)))
                .appendNewline()
                .append(detail(text("unknown.server.detail", NamedTextColor.GRAY)));
    }

    /**
     * Joueur introuvable.
     *
     * @param name le nom saisi
     * @return le message
     */
    public static Component unknownPlayer(String name) {
        return headline(text("unknown.player", NamedTextColor.RED,
                Component.text(name, NamedTextColor.WHITE)))
                .appendNewline()
                .append(detail(text("unknown.player.detail", NamedTextColor.GRAY)));
    }

    /**
     * Droits insuffisants sur le serveur visé.
     *
     * @param serverName le nom du serveur
     * @return le message
     */
    public static Component notAdministrator(Component serverName) {
        return headline(text("denied", NamedTextColor.RED, serverName));
    }

    /**
     * Désignation du réseau entier, pour un refus qui ne vise aucun serveur en
     * particulier.
     *
     * @return le libellé
     */
    public static Component thisNetwork() {
        return text("network", NamedTextColor.RED);
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

        // La syntaxe reste en clair — un nom de sous-commande ne se traduit pas,
        // il se tape — seule son explication est traduite.
        String[][] lines = {
                {"menu", "usage.menu"},
                {"status [<server>]", "usage.status"},
                {"start <server>", "usage.start"},
                {"stop <server>", "usage.stop"},
                {"whitelist <server>", "usage.whitelist"},
                {"whitelist <server> on|off", "usage.whitelist.toggle"},
                {"whitelist <server> add|remove <player>", "usage.whitelist.edit"},
        };
        for (String[] line : lines) {
            message = message.appendNewline().append(INDENT
                    .append(Component.text("/" + label + " " + line[0], NamedTextColor.WHITE))
                    .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                    .append(text(line[1], NamedTextColor.DARK_GRAY)));
        }
        return message;
    }
}
