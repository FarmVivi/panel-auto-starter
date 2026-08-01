package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.message.AdminMessages;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Menu de sélection d'un serveur.
 * <p>
 * <strong>Il est volontairement bête.</strong> Il liste les serveurs et
 * transmet la demande, exactement comme le ferait un {@code /server} tapé à la
 * main. Il ne filtre pas selon les permissions et ne cache rien : c'est
 * l'événement de connexion qui accepte ou refuse, et lui seul.
 * <p>
 * Le raisonnement tient en une phrase : filtrer ici ne protégerait rien, la
 * commande restant ouverte, mais donnerait l'illusion d'une protection — la
 * pire des situations, parce qu'on cesse alors de vérifier là où il le faut. Le
 * joueur qui clique un serveur interdit reçoit le même refus motivé que s'il
 * avait tapé la commande.
 */
public final class ServerMenu {

    private ServerMenu() {
    }

    private static Component text(String key, NamedTextColor color,
                                  net.kyori.adventure.text.ComponentLike... args) {
        return Component.translatable("panelautostarter." + key, args).color(color);
    }

    /**
     * Construit le menu.
     *
     * @param servers      les serveurs gérés
     * @param viewer       le joueur à qui il s'adresse
     * @param serverCommand la commande de connexion du proxy, sans barre oblique
     * @return le menu
     */
    public static Menu build(Collection<MinecraftServer> servers, CommonPlayer viewer,
                             String serverCommand) {
        List<MenuItem> items = new ArrayList<>();

        for (MinecraftServer server : servers) {
            items.add(item(server, viewer, serverCommand));
        }

        if (items.isEmpty()) {
            items.add(MenuItem.info("minecraft:barrier",
                    text("menu.servers.none", NamedTextColor.GRAY),
                    List.of(text("menu.servers.none.detail", NamedTextColor.DARK_GRAY))));
        }

        return new Menu(text("menu.servers.title", NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD), items);
    }

    private static MenuItem item(MinecraftServer server, CommonPlayer viewer, String serverCommand) {
        String name = server.getServer().getName();
        MinecraftServerStatus status = server.getStatus();
        boolean here = server.getServer().equals(viewer.getServer());

        Component label = AdminMessages.statusDot(status)
                .append(Component.text(" "))
                .append(Component.text(server.getServer().getDisplayName(),
                        here ? NamedTextColor.GRAY : NamedTextColor.YELLOW))
                .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                .append(AdminMessages.statusLabel(status));

        List<Component> lore = new ArrayList<>();

        // Le MOTD retenu, et non celui du ping courant : c'est justement quand
        // le serveur est eteint qu'il a quelque chose a dire, et le cache le
        // conserve pour ce moment-la.
        ServerMotd motd = server.getMotd();
        if (motd != null && !motd.isEmpty()) {
            Component description = motd.getDescription();
            if (description != null) {
                lore.add(description);
                lore.add(Component.empty());
            }
        }

        CommonServerPing ping = server.getServerPing();
        if (status.equals(MinecraftServerStatus.ONLINE) && ping != null) {
            lore.add(text("menu.servers.online", NamedTextColor.GRAY,
                    Component.text(ping.getOnlinePlayers())));
        } else if (!status.equals(MinecraftServerStatus.ONLINE)) {
            lore.add(text("menu.servers.start", NamedTextColor.GRAY));
        }

        int queued = server.getQueue().size();
        if (queued > 0) {
            lore.add(text("menu.waiting", NamedTextColor.DARK_GRAY, Component.text(queued)));
        }

        if (here) {
            // Proposer de rejoindre l'endroit ou l'on est deja n'a pas de sens.
            lore.add(text("menu.servers.here", NamedTextColor.DARK_GRAY));
            return MenuItem.info(icon(status), label, lore);
        }

        return MenuItem.of(icon(status), label, lore, serverCommand + " " + name);
    }

    private static String icon(MinecraftServerStatus status) {
        return switch (status) {
            case ONLINE -> "minecraft:lime_concrete";
            case STARTING -> "minecraft:orange_concrete";
            default -> "minecraft:gray_concrete";
        };
    }
}
