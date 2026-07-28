package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingMessages;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.Messages;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServerConnectEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;
    private final CommonServer limboServer;
    private final PendingMessages pendingMessages;

    public ServerConnectEventListener(PanelAutoStarter plugin) {
        this(plugin, plugin.getPendingMessages());
    }

    ServerConnectEventListener(PanelAutoStarter plugin, PendingMessages pendingMessages) {
        this.plugin = plugin;
        this.pendingMessages = pendingMessages;
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
                server.getQueue().add(player);
                if (serverStatus.equals(MinecraftServerStatus.OFFLINE)) {
                    server.start();
                }

                String serverName = server.getServer().getDisplayName();
                Component message = Messages.joinedQueue(serverName,
                        server.getQueue().indexOf(player) + 1, server.getQueue().size());

                MessageSettings.TitleSettings title = plugin.getMessageSettings().getTitle();
                if (title.enabled()) {
                    // Le titre part immediatement meme pour un joueur qui arrive :
                    // contrairement au chat, il est mis en file par le client et
                    // s'affiche des l'entree en jeu.
                    player.showTitle(Messages.startingTitle(), Messages.startingSubtitle(serverName),
                            title.fadeIn(), title.stay(), title.fadeOut());
                }

                if (joiningTheProxy) {
                    // La connexion n'a pas encore atteint l'etat de jeu : un
                    // message envoye maintenant serait perdu, le client n'etant
                    // pas en mesure d'afficher du chat. On le delivre a l'arrivee
                    // effective sur le limbo.
                    pendingMessages.queue(player, message);
                } else {
                    player.sendMessage(message);
                }
            }
        }
    }
}
