package fr.farmvivi.panelautostarter.command;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.access.AdminAccess;
import fr.farmvivi.panelautostarter.access.Whitelist;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.command.CommonCommand;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.AdminMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Commande d'administration : état, démarrage, arrêt et listes blanches.
 * <p>
 * Chaque action vérifie les droits <em>sur le serveur visé</em> : la permission
 * globale n'est pas exigée, un responsable de serveur devant pouvoir gérer le
 * sien sans avoir la main sur les autres. La commande n'affiche d'ailleurs que
 * les serveurs que son auteur administre.
 */
public final class AdminCommand implements CommonCommand {

    private static final String NAME = "panelautostarter";

    private final PanelAutoStarter plugin;

    public AdminCommand(PanelAutoStarter plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<String> getAliases() {
        return List.of("pas");
    }

    @Override
    public void execute(CommonCommandSource source, String[] args) {
        if (!AdminAccess.canAdministerAnything(source, managedServerNames())) {
            source.sendMessage(AdminMessages.notAdministrator("ce réseau"));
            return;
        }

        if (args.length == 0) {
            source.sendMessage(AdminMessages.usage("pas"));
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> status(source, args);
            case "start" -> startOrStop(source, args, true);
            case "stop" -> startOrStop(source, args, false);
            case "whitelist" -> whitelist(source, args);
            default -> source.sendMessage(AdminMessages.usage("pas"));
        }
    }

    // ===================== Sous-commandes =====================

    private void status(CommonCommandSource source, String[] args) {
        List<MinecraftServer> servers = new ArrayList<>();
        if (args.length >= 2) {
            MinecraftServer server = resolveServer(source, args[1]);
            if (server == null) {
                return;
            }
            servers.add(server);
        } else {
            for (MinecraftServer server : plugin.getServers().values()) {
                if (AdminAccess.canAdminister(source, server.getServer().getName())) {
                    servers.add(server);
                }
            }
        }

        source.sendMessage(AdminMessages.statusHeader(servers.size()));
        for (MinecraftServer server : servers) {
            Whitelist whitelist = plugin.getWhitelistStore().get(server.getServer().getName());
            source.sendMessage(AdminMessages.statusLine(server, whitelist.isEnabled(),
                    whitelist.size()));
        }
    }

    private void startOrStop(CommonCommandSource source, String[] args, boolean start) {
        if (args.length < 2) {
            source.sendMessage(AdminMessages.usage("pas"));
            return;
        }
        MinecraftServer server = resolveServer(source, args[1]);
        if (server == null) {
            return;
        }

        String name = server.getServer().getName();
        MinecraftServerStatus status = server.getStatus();

        if (start) {
            if (!status.equals(MinecraftServerStatus.OFFLINE)) {
                source.sendMessage(AdminMessages.alreadyInState(name,
                        status.equals(MinecraftServerStatus.ONLINE) ? "en ligne" : "en démarrage"));
                return;
            }
            server.start();
            source.sendMessage(AdminMessages.starting(name));
        } else {
            if (!status.equals(MinecraftServerStatus.ONLINE)) {
                source.sendMessage(AdminMessages.alreadyInState(name, "arrêté"));
                return;
            }
            server.stop();
            source.sendMessage(AdminMessages.stopping(name));
        }

        plugin.getLogger().info(source.getName() + " a demande "
                + (start ? "le demarrage" : "l'arret") + " de " + name);
    }

    private void whitelist(CommonCommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(AdminMessages.usage("pas"));
            return;
        }
        MinecraftServer server = resolveServer(source, args[1]);
        if (server == null) {
            return;
        }

        String name = server.getServer().getName();
        Whitelist whitelist = plugin.getWhitelistStore().get(name);

        if (args.length == 2) {
            source.sendMessage(AdminMessages.whitelistContents(name, whitelist.isEnabled(),
                    whitelist.getPlayers()));
            return;
        }

        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "on", "off" -> {
                whitelist.setEnabled(args[2].equalsIgnoreCase("on"));
                persist();
                source.sendMessage(AdminMessages.whitelistToggled(name, whitelist.isEnabled(),
                        whitelist.size()));
            }
            case "list" -> source.sendMessage(AdminMessages.whitelistContents(name,
                    whitelist.isEnabled(), whitelist.getPlayers()));
            case "add", "remove" -> {
                if (args.length < 4) {
                    source.sendMessage(AdminMessages.usage("pas"));
                    return;
                }
                editWhitelist(source, whitelist, name, args[2], args[3]);
            }
            default -> source.sendMessage(AdminMessages.usage("pas"));
        }
    }

    private void editWhitelist(CommonCommandSource source, Whitelist whitelist, String serverName,
                               String action, String target) {
        UUID uuid = resolveUuid(whitelist, target);
        if (uuid == null) {
            source.sendMessage(AdminMessages.unknownPlayer(target));
            return;
        }

        if (action.equalsIgnoreCase("add")) {
            String username = resolveUsername(uuid, target);
            boolean added = whitelist.add(uuid, username);
            persist();
            source.sendMessage(AdminMessages.whitelistAdded(username, serverName, !added));
        } else {
            String username = resolveUsername(uuid, whitelist.getPlayers().get(uuid));
            boolean removed = whitelist.remove(uuid);
            persist();
            source.sendMessage(AdminMessages.whitelistRemoved(username, serverName, removed));
        }
    }

    // ===================== Resolutions =====================

    /**
     * Retrouve un serveur géré et vérifie les droits de l'auteur dessus.
     * <p>
     * Les deux vont ensemble : répondre « serveur inconnu » à qui n'a pas les
     * droits, ou l'inverse, laisserait deviner ce qui existe.
     *
     * @return le serveur, ou null si le message d'erreur a déjà été envoyé
     */
    private MinecraftServer resolveServer(CommonCommandSource source, String name) {
        for (MinecraftServer server : plugin.getServers().values()) {
            if (server.getServer().getName().equalsIgnoreCase(name)) {
                if (!AdminAccess.canAdminister(source, server.getServer().getName())) {
                    source.sendMessage(AdminMessages.notAdministrator(server.getServer().getName()));
                    return null;
                }
                return server;
            }
        }
        source.sendMessage(AdminMessages.unknownServer(name));
        return null;
    }

    /**
     * Retrouve l'identifiant d'un joueur.
     * <p>
     * Trois pistes, dans l'ordre : un identifiant saisi tel quel, un joueur
     * connecté, puis un joueur déjà inscrit sur la liste. La dernière permet de
     * retirer quelqu'un qui n'est pas là — sans elle, on ne pourrait retirer que
     * les joueurs connectés, ce qui est exactement l'inverse du besoin.
     * <p>
     * Aucune interrogation de l'API Mojang : elle ajouterait un appel réseau sur
     * le chemin d'une commande, et échouerait sur un serveur hors ligne.
     *
     * @return l'identifiant, ou null s'il reste introuvable
     */
    private UUID resolveUuid(Whitelist whitelist, String target) {
        try {
            return UUID.fromString(target);
        } catch (IllegalArgumentException ignored) {
            // Ce n'etait pas un identifiant : on continue avec les autres pistes.
        }

        CommonPlayer online = plugin.getProxy().getPlayer(target);
        if (online != null) {
            return online.getUniqueId();
        }

        for (Map.Entry<UUID, String> entry : whitelist.getPlayers().entrySet()) {
            if (target.equalsIgnoreCase(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String resolveUsername(UUID uuid, String fallback) {
        CommonPlayer online = plugin.getProxy().getPlayer(uuid);
        if (online != null) {
            return online.getUsername();
        }
        return fallback != null ? fallback : uuid.toString();
    }

    private void persist() {
        plugin.getWhitelistStore().save();
    }

    private List<String> managedServerNames() {
        List<String> names = new ArrayList<>();
        for (MinecraftServer server : plugin.getServers().values()) {
            names.add(server.getServer().getName());
        }
        return names;
    }

    // ===================== Completion =====================

    @Override
    public List<String> suggest(CommonCommandSource source, String[] args) {
        if (!AdminAccess.canAdministerAnything(source, managedServerNames())) {
            return List.of();
        }

        if (args.length <= 1) {
            return filter(List.of("status", "start", "stop", "whitelist"), last(args));
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            List<String> administered = new ArrayList<>();
            for (String name : managedServerNames()) {
                if (AdminAccess.canAdminister(source, name)) {
                    administered.add(name);
                }
            }
            return filter(administered, last(args));
        }

        if (sub.equals("whitelist") && args.length == 3) {
            return filter(List.of("on", "off", "list", "add", "remove"), last(args));
        }

        if (sub.equals("whitelist") && args.length == 4) {
            List<String> names = new ArrayList<>();
            for (CommonPlayer player : plugin.getProxy().getPlayers()) {
                names.add(player.getUsername());
            }
            return filter(names, last(args));
        }

        return List.of();
    }

    private static String last(String[] args) {
        return args.length == 0 ? "" : args[args.length - 1];
    }

    private static List<String> filter(List<String> candidates, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(candidate);
            }
        }
        return matches;
    }
}
