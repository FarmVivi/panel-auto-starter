package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.access.AdminAccess;
import fr.farmvivi.panelautostarter.access.Whitelist;
import fr.farmvivi.panelautostarter.access.WhitelistStore;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.AdminMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menu d'administration, en deux niveaux.
 * <p>
 * Le premier liste les serveurs, un par entrée ; le second offre les actions
 * d'un serveur donné. Tout mettre à plat marchait tant qu'il n'y avait qu'un
 * serveur, et devenait illisible dès le deuxième : quatre entrées par serveur
 * se suivant sans séparation, on ne savait plus laquelle appartenait à quoi.
 * Avec un seul serveur le premier niveau n'apprend rien, mais il reste : une
 * disposition qui change selon le nombre de serveurs se réapprend à chaque
 * fois.
 * <p>
 * Contrairement au menu de sélection, celui-ci <em>filtre</em>. La différence
 * n'est pas une incohérence : le menu de sélection cacherait un serveur que
 * tout le monde peut déjà nommer par commande, ce qui ne protégerait rien ; le
 * menu d'administration, lui, refléterait des serveurs sur lesquels son lecteur
 * ne peut rien, et lui annoncerait une infrastructure qui ne le regarde pas.
 * <p>
 * Les entrées restent des commandes, soumises aux mêmes contrôles : le menu ne
 * peut rien faire que son utilisateur n'aurait pu taper lui-même.
 */
public final class AdminMenu {

    private AdminMenu() {
    }

    /**
     * Construit le menu racine : un serveur par entrée.
     *
     * @param servers    les serveurs gérés
     * @param whitelists les listes blanches
     * @param source     le demandeur
     * @param label      le nom de la commande d'administration, sans barre oblique
     * @return le menu
     */
    public static Menu build(Collection<MinecraftServer> servers, WhitelistStore whitelists,
                             CommonCommandSource source, String label) {
        List<MenuItem> items = new ArrayList<>();

        for (MinecraftServer server : servers) {
            String name = server.getServer().getName();
            if (!AdminAccess.canAdminister(source, name)) {
                continue;
            }
            items.add(serverEntry(server, whitelists.get(name), label));
        }

        if (items.isEmpty()) {
            items.add(MenuItem.info("minecraft:barrier",
                    Component.text("Vous n'administrez aucun serveur", NamedTextColor.GRAY),
                    List.of()));
        }

        return new Menu(Component.text("Administration", NamedTextColor.AQUA, TextDecoration.BOLD),
                items);
    }

    /**
     * Construit le menu d'un serveur : ses actions.
     *
     * @param server    le serveur
     * @param whitelist sa liste blanche
     * @param label     le nom de la commande d'administration
     * @return le menu
     */
    public static Menu buildForServer(MinecraftServer server, Whitelist whitelist, String label) {
        String name = server.getServer().getName();
        MinecraftServerStatus status = server.getStatus();
        List<MenuItem> items = new ArrayList<>();

        // Une seule action possible a la fois : proposer « demarrer » sur un
        // serveur allume, ou l'inverse, n'aboutirait qu'a un refus.
        if (status.equals(MinecraftServerStatus.OFFLINE)) {
            items.add(MenuItem.of("minecraft:lime_dye",
                    Component.text("Démarrer", NamedTextColor.GREEN),
                    List.of(Component.text("Le serveur est éteint", NamedTextColor.GRAY)),
                    label + " start " + name));
        } else if (status.equals(MinecraftServerStatus.ONLINE)) {
            items.add(MenuItem.of("minecraft:red_dye",
                    Component.text("Arrêter", NamedTextColor.RED),
                    List.of(Component.text("Les joueurs présents seront ramenés",
                            NamedTextColor.GRAY)),
                    label + " stop " + name));
        } else {
            items.add(MenuItem.info("minecraft:orange_dye",
                    Component.text("Démarrage en cours", NamedTextColor.GOLD),
                    List.of(Component.text("Patientez", NamedTextColor.GRAY))));
        }

        items.add(MenuItem.of("minecraft:paper",
                whitelist.isEnabled()
                        ? Component.text("Désactiver la liste blanche", NamedTextColor.GOLD)
                        : Component.text("Activer la liste blanche", NamedTextColor.AQUA),
                List.of(Component.text(whitelist.size() + " inscrit"
                        + (whitelist.size() > 1 ? "s" : ""), NamedTextColor.DARK_GRAY)),
                label + " whitelist " + name + (whitelist.isEnabled() ? " off" : " on")));

        items.add(MenuItem.of("minecraft:player_head",
                Component.text("Inscrire un joueur", NamedTextColor.GREEN),
                List.of(Component.text("Parmi les joueurs connectés", NamedTextColor.DARK_GRAY)),
                label + " menu " + name + " add"));

        items.add(MenuItem.of("minecraft:book",
                Component.text("Voir et retirer", NamedTextColor.GRAY),
                List.of(Component.text(whitelist.size() + " inscrit"
                        + (whitelist.size() > 1 ? "s" : ""), NamedTextColor.DARK_GRAY)),
                label + " menu " + name + " remove"));

        items.add(MenuItem.of("minecraft:arrow",
                Component.text("Retour", NamedTextColor.GRAY),
                List.of(), label + " menu"));

        return new Menu(Component.text(name, NamedTextColor.AQUA, TextDecoration.BOLD), items);
    }

    /**
     * Menu d'inscription : les joueurs connectés qui ne figurent pas encore sur
     * la liste.
     * <p>
     * Choisir dans une liste plutôt que saisir un pseudo — au clavier, ou dans
     * une enclume détournée — évite la faute de frappe, qui sur une liste
     * blanche ne se voit pas : on inscrit un nom qui n'existe pas et on croit
     * l'affaire réglée jusqu'à ce que l'intéressé se plaigne. Un joueur absent
     * reste inscriptible par la commande, qui accepte aussi un identifiant.
     *
     * @param serverName le serveur
     * @param whitelist  sa liste blanche
     * @param online     les joueurs connectés
     * @param label      le nom de la commande d'administration
     * @return le menu
     */
    public static Menu buildWhitelistAdd(String serverName, Whitelist whitelist,
                                         Collection<CommonPlayer> online, String label) {
        List<MenuItem> items = new ArrayList<>();

        for (CommonPlayer player : online) {
            if (whitelist.contains(player.getUniqueId())) {
                continue;
            }
            items.add(MenuItem.head(player.getSkin(),
                    Component.text(player.getUsername(), NamedTextColor.WHITE),
                    List.of(Component.text("Inscrire sur " + serverName, NamedTextColor.GRAY)),
                    label + " whitelist " + serverName + " add " + player.getUsername()));
        }

        if (items.isEmpty()) {
            items.add(MenuItem.info("minecraft:barrier",
                    Component.text("Personne à inscrire", NamedTextColor.GRAY),
                    List.of(Component.text("Tous les joueurs connectés y figurent déjà",
                            NamedTextColor.DARK_GRAY))));
        }

        items.add(MenuItem.of("minecraft:arrow", Component.text("Retour", NamedTextColor.GRAY),
                List.of(), label + " menu " + serverName));

        return new Menu(Component.text("Inscrire — " + serverName, NamedTextColor.AQUA,
                TextDecoration.BOLD), items);
    }

    /**
     * Menu de retrait : les joueurs inscrits, présents ou non.
     *
     * @param serverName le serveur
     * @param whitelist  sa liste blanche
     * @param online     les joueurs connectés, pour leur apparence
     * @param label      le nom de la commande d'administration
     * @return le menu
     */
    public static Menu buildWhitelistRemove(String serverName, Whitelist whitelist,
                                            Collection<CommonPlayer> online, String label) {
        List<MenuItem> items = new ArrayList<>();

        // Les joueurs presents, indexes pour retrouver leur apparence. Ceux qui
        // sont absents n'en ont pas de connue.
        Map<UUID, CommonPlayer> onlineByUuid = new java.util.HashMap<>();
        for (CommonPlayer player : online) {
            onlineByUuid.put(player.getUniqueId(), player);
        }

        for (Map.Entry<UUID, String> entry : whitelist.getPlayers().entrySet()) {
            // Le pseudo enregistre suffit a la commande de retrait, et se lit
            // mieux qu'un identifiant ; celui-ci prend le relais s'il manque.
            String target = entry.getValue() != null && !entry.getValue().isBlank()
                    ? entry.getValue() : entry.getKey().toString();
            // Un inscrit absent du proxy n'a pas d'apparence connue : tete
            // generique plutot que rien.
            CommonPlayer present = onlineByUuid.get(entry.getKey());
            items.add(MenuItem.head(present == null ? null : present.getSkin(),
                    Component.text(target, NamedTextColor.WHITE),
                    List.of(Component.text("Retirer de " + serverName, NamedTextColor.RED)),
                    label + " whitelist " + serverName + " remove " + target));
        }

        if (items.isEmpty()) {
            items.add(MenuItem.info("minecraft:barrier",
                    Component.text("Personne n'y figure", NamedTextColor.GRAY), List.of()));
        }

        items.add(MenuItem.of("minecraft:arrow", Component.text("Retour", NamedTextColor.GRAY),
                List.of(), label + " menu " + serverName));

        return new Menu(Component.text("Liste blanche — " + serverName, NamedTextColor.AQUA,
                TextDecoration.BOLD), items);
    }

    private static MenuItem serverEntry(MinecraftServer server, Whitelist whitelist, String label) {
        String name = server.getServer().getName();
        MinecraftServerStatus status = server.getStatus();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("État : ", NamedTextColor.GRAY)
                .append(AdminMessages.statusLabel(status)));
        if (!server.getQueue().isEmpty()) {
            lore.add(Component.text(server.getQueue().size() + " en attente", NamedTextColor.GRAY));
        }
        lore.add(Component.text("Liste blanche : ", NamedTextColor.GRAY)
                .append(whitelist.isEnabled()
                        ? Component.text("active (" + whitelist.size() + ")", NamedTextColor.GREEN)
                        : Component.text("inactive", NamedTextColor.DARK_GRAY)));

        return MenuItem.of(icon(status), AdminMessages.statusDot(status)
                        .append(Component.text(" "))
                        .append(Component.text(name, NamedTextColor.YELLOW)),
                lore, label + " menu " + name);
    }

    private static String icon(MinecraftServerStatus status) {
        return switch (status) {
            case ONLINE -> "minecraft:lime_concrete";
            case STARTING -> "minecraft:orange_concrete";
            default -> "minecraft:gray_concrete";
        };
    }
}
