package fr.farmvivi.panelautostarter;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MinecraftServer {
    private final PanelAutoStarter plugin;

    private final CommonServer server;
    private final ClientServer pterodactylServer;
    private final List<CommonPlayer> queue = new LinkedList<>();
    private MinecraftServerStatus status = MinecraftServerStatus.OFFLINE;
    private CommonServerPing serverPing;
    private long lastBusyTime = System.currentTimeMillis();
    private long serverStartedTime = 0;
    private long lastTeleportTime = 0;

    public MinecraftServer(PanelAutoStarter plugin, CommonServer server, ClientServer pterodactylServer) {
        this.plugin = plugin;
        this.server = server;
        this.pterodactylServer = pterodactylServer;

        scheduleServerStatusCheck();
    }

    /**
     * Scheduler adaptatif : utilise un refresh rate plus rapide pendant le démarrage
     */
    private void scheduleServerStatusCheck() {
        checkServerStatusAndReschedule();
    }

    private void checkServerStatusAndReschedule() {
        // Obtenir les paramètres de configuration
        long normalCheckInterval = plugin.getConfig().getLong("server-start.check-interval-normal", 15);
        long startupCheckInterval = plugin.getConfig().getLong("server-start.check-interval-startup", 3);

        // Déterminer l'intervalle à utiliser
        long checkInterval = status.equals(MinecraftServerStatus.STARTING) ? startupCheckInterval : normalCheckInterval;

        // Faire le check
        server.ping(result -> {
            long curMillis = System.currentTimeMillis();
            updateServerStatus(result, curMillis);
            handleQueueAndShutdown(result, curMillis);

            // Re-scheduler avec l'intervalle approprié
            this.plugin.getProxy().schedule(plugin.getPlugin(), 
                this::checkServerStatusAndReschedule, 
                checkInterval, 
                TimeUnit.SECONDS);
        });
    }

    private void updateServerStatus(CommonServerPing result, long curMillis) {
        if (serverPing == null && result != null) {
            if (pterodactylServer.retrieveUtilization().execute().getState() == UtilizationState.STARTING) {
                return;
            }
            status = MinecraftServerStatus.ONLINE;
            serverStartedTime = curMillis;  // Enregistrer l'heure de démarrage
            logServerStatus("en ligne", NamedTextColor.GREEN);
        } else if (shouldSetOffline(result, curMillis)) {
            status = MinecraftServerStatus.OFFLINE;
            logServerStatus("hors-ligne", NamedTextColor.RED);
            clearQueue();
            serverStartedTime = 0;
        }
        serverPing = result;
    }

    private boolean shouldSetOffline(CommonServerPing result, long curMillis) {
        return (serverPing != null && result == null && !status.equals(MinecraftServerStatus.STARTING)) ||
                (status.equals(MinecraftServerStatus.STARTING) && lastBusyTime < curMillis);
    }

    private void logServerStatus(String statusMessage, NamedTextColor color) {
        plugin.getLogger().info(Component.text(server.getName()).color(NamedTextColor.YELLOW)
                .append(Component.text(" " + statusMessage).color(color)));
    }

    private void clearQueue() {
        if (!queue.isEmpty()) {
            Iterator<CommonPlayer> iterator = queue.iterator();
            while (iterator.hasNext()) {
                CommonPlayer player = iterator.next();
                plugin.getLogger().warning("Impossible de téléporter " + player.getUsername() + " sur " + server.getName());
                player.sendMessage(Component.text("Téléportation sur le serveur ").color(NamedTextColor.RED)
                        .append(Component.text(server.getDisplayName()).color(NamedTextColor.YELLOW))
                        .append(Component.text(" impossible !").color(NamedTextColor.RED)));
                iterator.remove();
            }
        }
    }

    private void handleQueueAndShutdown(CommonServerPing result, long curMillis) {
        if (status.equals(MinecraftServerStatus.ONLINE)) {
            handleQueue(curMillis);
            updateLastBusyTime(result, curMillis);
            if (curMillis - lastBusyTime > 5 * 60 * 1000) {
                stop();
            }
        }
    }

    /**
     * Gère la file d'attente avec un délai configurable entre chaque téléportation
     * et un délai d'attente avant de commencer les téléportations après le démarrage du serveur
     */
    private void handleQueue(long curMillis) {
        if (queue.isEmpty()) {
            return;
        }

        // Obtenir les paramètres de configuration
        long waitBeforeTeleport = plugin.getConfig().getLong("server-start.wait-before-teleport", 5) * 1000;
        long teleportDelay = plugin.getConfig().getLong("server-start.teleport-delay", 1) * 1000;

        // Attendre un certain temps après le démarrage avant de TP les joueurs
        if (curMillis - serverStartedTime < waitBeforeTeleport) {
            return;
        }

        // Vérifier si on peut TP le prochain joueur (délai minimum entre les TP)
        if (curMillis - lastTeleportTime < teleportDelay) {
            return;
        }

        // Trouver le premier joueur à téléporter (pas déjà sur le serveur)
        CommonPlayer playerToTeleport = null;
        int playerIndex = -1;
        
        for (int i = 0; i < queue.size(); i++) {
            CommonPlayer player = queue.get(i);
            if (!player.getServer().equals(server)) {
                playerToTeleport = player;
                playerIndex = i;
                break;
            }
        }

        // Si on a trouvé un joueur à téléporter
        if (playerToTeleport != null) {
            teleportPlayer(playerToTeleport);
            queue.remove(playerIndex);
            lastTeleportTime = curMillis;
        } else {
            // Nettoyer les joueurs déjà sur le serveur
            queue.removeIf(player -> player.getServer().equals(server));
        }
    }

    private void teleportPlayer(CommonPlayer player) {
        plugin.getLogger().info("Téléportation de " + player.getUsername() + " sur " + server.getName());
        player.sendMessage(Component.text("Téléportation sur le serveur ").color(NamedTextColor.GREEN)
                .append(Component.text(server.getDisplayName()).color(NamedTextColor.YELLOW))
                .append(Component.text("...").color(NamedTextColor.GREEN)));
        player.connectToServer(server);
    }

    private void updateLastBusyTime(CommonServerPing result, long curMillis) {
        if (result != null && result.getOnlinePlayers() > 0 && curMillis > lastBusyTime) {
            lastBusyTime = curMillis;
        }
    }

    public synchronized void start() {
        if (!status.equals(MinecraftServerStatus.OFFLINE)) {
            return;
        }
        this.plugin.getLogger().info("Démarrage de " + server.getName() + "...");
        this.pterodactylServer.start().executeAsync();
        this.lastBusyTime = System.currentTimeMillis() + 15 * 60 * 1000;
        this.status = MinecraftServerStatus.STARTING;
        this.lastTeleportTime = 0;  // Réinitialiser le timer de téléportation
    }

    public synchronized void stop() {
        if (!status.equals(MinecraftServerStatus.ONLINE)) {
            return;
        }
        this.plugin.getLogger().info("Arrêt de " + server.getName() + "...");
        this.pterodactylServer.stop().executeAsync();
        this.lastBusyTime = System.currentTimeMillis();
    }

    public CommonServer getServer() {
        return server;
    }

    public List<CommonPlayer> getQueue() {
        return queue;
    }

    public MinecraftServerStatus getStatus() {
        return status;
    }

    public CommonServerPing getServerPing() {
        return serverPing;
    }
}
