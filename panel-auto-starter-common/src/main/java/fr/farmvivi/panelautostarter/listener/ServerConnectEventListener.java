package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.Messages;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import net.kyori.adventure.text.Component;

public class ServerConnectEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;
    private final CommonServer limboServer;
    private final PendingNotifications pendingNotifications;

    public ServerConnectEventListener(PanelAutoStarter plugin) {
        this(plugin, plugin.getPendingNotifications());
    }

    ServerConnectEventListener(PanelAutoStarter plugin, PendingNotifications pendingNotifications) {
        this.plugin = plugin;
        this.pendingNotifications = pendingNotifications;
        this.limboServer = plugin.getProxy().getServer(plugin.getConfig().getString("queue.server"));
    }

    @Override
    public void onServerConnect(ServerConnectEvent event) {
        CommonServer target = event.getTarget();
        CommonPlayer player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (plugin.getServers().containsKey(target)) {
            MinecraftServer server = plugin.getServers().get(target);
            MinecraftServerStatus serverStatus = server.getStatus();
            if (serverStatus.equals(MinecraftServerStatus.OFFLINE) || serverStatus.equals(MinecraftServerStatus.STARTING)) {
                // Un joueur deja en jeu est simplement retenu ; un joueur qui
                // arrive sur le proxy est redirige vers le limbo.
                boolean joiningTheProxy = player.getServer() == null;
                if (joiningTheProxy) {
                    event.setTarget(limboServer);
                } else {
                    event.setCancelled(true);
                }
                server.enqueue(player);
                if (serverStatus.equals(MinecraftServerStatus.OFFLINE)) {
                    server.start();
                }

                String serverName = server.getServer().getDisplayName();
                Component message = Messages.joinedQueue(serverName,
                        server.getQueue().indexOf(player) + 1, server.getQueue().size());

                MessageSettings.TitleSettings title = plugin.getMessageSettings().getTitle();

                if (joiningTheProxy) {
                    // La connexion n'a pas encore atteint l'etat de jeu : ni le
                    // chat ni le titre ne peuvent etre affiches, ils seraient
                    // perdus. On les rejoue a l'arrivee effective sur le limbo.
                    if (title.enabled()) {
                        pendingNotifications.queueTitle(player, Messages.startingTitle(),
                                Messages.startingSubtitle(serverName),
                                title.fadeIn(), title.stay(), title.fadeOut());
                        pendingNotifications.queueSound(player, title.sound());
                    }
                    pendingNotifications.queueMessage(player, message);
                } else {
                    if (title.enabled()) {
                        player.showTitle(Messages.startingTitle(), Messages.startingSubtitle(serverName),
                                title.fadeIn(), title.stay(), title.fadeOut());
                        if (!title.sound().isSilent()) {
                            player.playSound(title.sound().name(), title.sound().volume(), title.sound().pitch());
                        }
                    }
                    player.sendMessage(message);
                }
            }
        }
    }
}
