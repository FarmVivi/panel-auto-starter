package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.access.AdminAccess;
import fr.farmvivi.panelautostarter.access.Whitelist;
import fr.farmvivi.panelautostarter.access.WhitelistStore;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.AdminMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        items.add(MenuItem.of("minecraft:book",
                Component.text("Voir la liste blanche", NamedTextColor.GRAY),
                List.of(), label + " whitelist " + name + " list"));

        items.add(MenuItem.of("minecraft:arrow",
                Component.text("Retour", NamedTextColor.GRAY),
                List.of(), label + " menu"));

        return new Menu(Component.text(name, NamedTextColor.AQUA, TextDecoration.BOLD), items);
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
