package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;

public class PlayerDisconnectEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;

    public PlayerDisconnectEventListener(PanelAutoStarter plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        CommonPlayer player = event.getPlayer();

        if (player == null) {
            return;
        }

        for (MinecraftServer server : plugin.getServers().values()) {
            server.getQueue().removeIf(proxiedPlayer -> proxiedPlayer.equals(player));
        }

        // Un joueur parti avant d'arriver sur le limbo laisserait sinon ses
        // messages differes en memoire indefiniment.
        plugin.getPendingMessages().discard(player);
    }
}
