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
 * Menu d'administration.
 * <p>
 * Contrairement au menu de sélection, celui-ci <em>filtre</em> : il ne montre
 * que les serveurs dont son lecteur est responsable. La différence n'est pas
 * une incohérence. Le menu de sélection cache un serveur que tout le monde peut
 * déjà nommer par commande, ce qui ne protégerait rien ; le menu
 * d'administration, lui, refléterait autrement des serveurs sur lesquels son
 * lecteur ne peut rien, et lui annoncerait l'existence d'une infrastructure qui
 * ne le regarde pas.
 * <p>
 * Les entrées restent des commandes, soumises aux mêmes contrôles : le menu ne
 * peut rien faire que son utilisateur n'aurait pu taper lui-même.
 */
public final class AdminMenu {

    private AdminMenu() {
    }

    /**
     * Construit le menu pour un administrateur.
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
            items.addAll(entriesFor(server, whitelists.get(name), label));
        }

        if (items.isEmpty()) {
            items.add(MenuItem.info("minecraft:barrier",
                    Component.text("Vous n'administrez aucun serveur", NamedTextColor.GRAY),
                    List.of()));
        }

        return new Menu(Component.text("Administration", NamedTextColor.AQUA, TextDecoration.BOLD),
                items);
    }

    private static List<MenuItem> entriesFor(MinecraftServer server, Whitelist whitelist,
                                             String label) {
        String name = server.getServer().getName();
        MinecraftServerStatus status = server.getStatus();
        List<MenuItem> items = new ArrayList<>();

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

        items.add(MenuItem.info(icon(status), AdminMessages.statusDot(status)
                .append(Component.text(" "))
                .append(Component.text(name, NamedTextColor.YELLOW)), lore));

        // Une seule action possible a la fois : proposer « demarrer » sur un
        // serveur allume, ou l'inverse, n'aboutirait qu'a un refus.
        if (status.equals(MinecraftServerStatus.OFFLINE)) {
            items.add(MenuItem.of("minecraft:lime_dye",
                    Component.text("   Démarrer", NamedTextColor.GREEN),
                    List.of(), label + " start " + name));
        } else if (status.equals(MinecraftServerStatus.ONLINE)) {
            items.add(MenuItem.of("minecraft:red_dye",
                    Component.text("   Arrêter", NamedTextColor.RED),
                    List.of(), label + " stop " + name));
        } else {
            items.add(MenuItem.info("minecraft:orange_dye",
                    Component.text("   Démarrage en cours", NamedTextColor.GOLD), List.of()));
        }

        items.add(MenuItem.of("minecraft:paper",
                whitelist.isEnabled()
                        ? Component.text("   Désactiver la liste blanche", NamedTextColor.GOLD)
                        : Component.text("   Activer la liste blanche", NamedTextColor.AQUA),
                List.of(Component.text(whitelist.size() + " inscrit"
                        + (whitelist.size() > 1 ? "s" : ""), NamedTextColor.DARK_GRAY)),
                label + " whitelist " + name + (whitelist.isEnabled() ? " off" : " on")));

        items.add(MenuItem.of("minecraft:book",
                Component.text("   Voir la liste blanche", NamedTextColor.GRAY),
                List.of(), label + " whitelist " + name + " list"));

        return items;
    }

    private static String icon(MinecraftServerStatus status) {
        return switch (status) {
            case ONLINE -> "minecraft:lime_concrete";
            case STARTING -> "minecraft:orange_concrete";
            default -> "minecraft:gray_concrete";
        };
    }
}
