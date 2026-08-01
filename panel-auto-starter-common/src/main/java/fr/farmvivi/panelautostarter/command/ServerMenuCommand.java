package fr.farmvivi.panelautostarter.command;

import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.command.CommonCommand;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.menu.ServerMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Ouvre le menu de sélection des serveurs.
 * <p>
 * Ouverte à tous, sans permission : le menu ne fait que présenter ce que la
 * commande de connexion du proxy permet déjà de demander, et c'est l'événement
 * de connexion qui décide qui entre où.
 */
public final class ServerMenuCommand implements CommonCommand {

    private final PanelAutoStarter plugin;
    private final String name;

    public ServerMenuCommand(PanelAutoStarter plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void execute(CommonCommandSource source, String[] args) {
        CommonPlayer player = source.asPlayer();
        if (player == null) {
            source.sendMessage(Component.text("Cette commande s'adresse à un joueur.",
                    NamedTextColor.RED));
            return;
        }

        boolean opened = plugin.getMenuService().open(player, ServerMenu.build(
                plugin.getServers().values(), player,
                plugin.getMenuService().getSettings().serverCommand()));

        if (!opened) {
            source.sendMessage(Component.text("Aucun affichage de menu n'est disponible.",
                    NamedTextColor.RED));
        }
    }
}
