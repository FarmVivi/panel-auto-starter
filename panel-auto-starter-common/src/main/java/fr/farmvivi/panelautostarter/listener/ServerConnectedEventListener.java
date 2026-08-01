package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.SoundSpec;

/**
 * Délivre les messages mis de côté, une fois le joueur réellement arrivé sur un
 * serveur et donc en mesure de les afficher, et accueille celui qui arrive sur
 * le serveur d'attente sans rien attendre de particulier.
 */
public class ServerConnectedEventListener extends EventAdapter {

    private final PendingNotifications pendingNotifications;
    private final PanelAutoStarter plugin;

    public ServerConnectedEventListener(PendingNotifications pendingNotifications) {
        this(pendingNotifications, null);
    }

    /**
     * @param pendingNotifications les messages différés
     * @param plugin               le plugin, pour l'accueil ; null pour s'en passer
     */
    public ServerConnectedEventListener(PendingNotifications pendingNotifications,
                                        PanelAutoStarter plugin) {
        this.pendingNotifications = pendingNotifications;
        this.plugin = plugin;
    }

    @Override
    public void onServerConnected(ServerConnectedEvent event) {
        CommonPlayer player = event.getPlayer();

        // L'accueil est decide AVANT la delivrance : celle-ci vide le registre,
        // et l'on ne saurait plus ensuite qu'une explication attendait le
        // joueur.
        boolean welcome = deservesWelcome(player, event.getServer());

        pendingNotifications.flush(player);

        if (welcome) {
            showWelcome(player);
        }
    }

    /**
     * Décide si le joueur mérite un accueil.
     * <p>
     * Trois conditions, et chacune écarte un cas où le titre serait déplacé :
     * il faut qu'il arrive sur le serveur d'attente, qu'il n'y attende aucun
     * serveur — sinon il vient d'y être déposé le temps d'un démarrage, et le
     * titre de mise en file dit déjà ce qui lui arrive — et que rien ne
     * l'attende par ailleurs, un joueur ramené d'un serveur qui vient de
     * s'arrêter devant lire pourquoi plutôt qu'un « bienvenue ».
     */
    private boolean deservesWelcome(CommonPlayer player, CommonServer server) {
        if (plugin == null || player == null || server == null) {
            return false;
        }
        if (!plugin.getMessageSettings().getWelcome().enabled()) {
            return false;
        }

        String limboName = plugin.getConfig().getString("queue.server");
        if (limboName == null || !limboName.equalsIgnoreCase(server.getName())) {
            return false;
        }

        return plugin.getQueueCoordinator().queuedServer(player) == null
                && !pendingNotifications.hasPending(player);
    }

    private void showWelcome(CommonPlayer player) {
        MessageSettings.WelcomeSettings welcome = plugin.getMessageSettings().getWelcome();
        player.showTitle(welcome.title(), welcome.subtitle(),
                welcome.fadeIn(), welcome.stay(), welcome.fadeOut());

        SoundSpec sound = welcome.sound();
        if (sound != null && !sound.isSilent()) {
            player.playSound(sound.name(), sound.volume(), sound.pitch());
        }
    }
}
