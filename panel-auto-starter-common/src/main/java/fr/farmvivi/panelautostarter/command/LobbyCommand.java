package fr.farmvivi.panelautostarter.command;

import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.command.CommonCommand;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * Ramène un joueur au serveur d'attente.
 * <p>
 * Ouverte à tous, sans permission : le serveur d'attente est l'endroit où le
 * proxy dépose déjà tout le monde, y revenir n'accorde rien.
 * <p>
 * Elle <strong>quitte la file</strong> au passage. Revenir au serveur d'attente
 * est la façon naturelle de renoncer à un serveur qui met du temps à démarrer ;
 * y rester inscrit ferait téléporter le joueur alors qu'il vient de dire, par
 * son geste, qu'il ne l'attendait plus.
 */
public final class LobbyCommand implements CommonCommand {

    private final PanelAutoStarter plugin;
    private final LobbyCommandSettings settings;

    public LobbyCommand(PanelAutoStarter plugin, LobbyCommandSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    @Override
    public String getName() {
        return settings.name();
    }

    @Override
    public List<String> getAliases() {
        return settings.aliases();
    }

    @Override
    public void execute(CommonCommandSource source, String[] args) {
        CommonPlayer player = source.asPlayer();
        if (player == null) {
            source.sendMessage(Component.translatable("panelautostarter.command.player-only",
                    NamedTextColor.RED));
            return;
        }

        String limboName = plugin.getConfig().getString("queue.server");
        CommonServer limbo = limboName == null ? null : plugin.getProxy().getServer(limboName);
        if (limbo == null) {
            source.sendMessage(Messages.lobbyUnavailable());
            return;
        }

        if (limbo.equals(player.getServer())) {
            // Le renvoyer la ou il est provoquerait un aller-retour visible a
            // l'ecran pour rien.
            source.sendMessage(Messages.alreadyOnLobby(limbo.getDisplayName()));
            return;
        }

        plugin.getQueueCoordinator().leaveAll(player);
        player.sendMessage(Messages.returningToLobby(limbo.getDisplayName()));
        player.connectToServer(limbo);
    }
}
