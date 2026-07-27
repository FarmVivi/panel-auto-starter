package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Adapte la réponse au ping des serveurs gérés.
 * <p>
 * Le plugin n'embarque plus aucun favicon : chaque serveur réutilise le sien,
 * conservé lors de son dernier passage en ligne et décoré d'une pastille d'état.
 * Une icône générique écraserait l'identité visuelle du serveur, et un serveur
 * jamais vu en ligne est simplement laissé tel que le proxy le présente.
 */
public class ProxyPingEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;
    private final Map<String, CommonServer> forcedHosts;

    public ProxyPingEventListener(PanelAutoStarter plugin) {
        this.plugin = plugin;
        this.forcedHosts = new TreeMap<>();
        for (Map.Entry<String, String> entry : plugin.getProxy().getForcedHosts().entrySet()) {
            forcedHosts.put(entry.getKey(), plugin.getProxy().getServer(entry.getValue()));
        }
    }

    @Override
    public void onProxyPing(ProxyPingEvent event) {
        String host = event.getHost();
        if (!isValidHost(host)) {
            return;
        }
        
        CommonServer commonServer = forcedHosts.get(host);
        if (!plugin.getServers().containsKey(commonServer)) {
            return;
        }
        
        CommonServerPing response = event.getResponse();
        MinecraftServer server = plugin.getServers().get(commonServer);

        // Signale que ce serveur est regarde, pour que la surveillance reste
        // rapprochee meme s'il est eteint.
        server.notifyWatched();

        switch (server.getStatus()) {
            case ONLINE -> handleOnlineServer(response, server);
            case OFFLINE -> handleOfflineServer(response, server);
            case STARTING -> handleStartingServer(response, server);
        }

        event.setResponse(response);
    }

    private boolean isValidHost(String host) {
        return host != null && !host.isEmpty() && forcedHosts.containsKey(host);
    }

    /**
     * Serveur en ligne : on recopie le ping du backend dans la reponse du proxy.
     * <p>
     * Cela rend inutile le {@code ping-passthrough} de Velocity, qui ne peut pas
     * distinguer l'adresse du proxy d'un forced host : regle sur {@code "all"} il
     * fait remonter le MOTD du premier serveur de la liste {@code try} y compris
     * quand on ping le proxy lui-meme. En servant le backend ici, le proxy peut
     * repasser en {@code ping-passthrough = "disabled"} et garder son propre MOTD
     * sur son adresse principale.
     * <p>
     * La donnee vient du cache du plugin, deja alimente par la surveillance : ce
     * chemin ne declenche donc aucun appel reseau bloquant.
     * <p>
     * La version de protocole n'est volontairement pas recopiee : elle indique au
     * client s'il peut se connecter, et c'est le proxy qui le sait, pas le
     * backend. Recopier celle du backend afficherait "version incompatible" a des
     * clients que le proxy sait pourtant servir.
     */
    private void handleOnlineServer(CommonServerPing response, MinecraftServer server) {
        CommonServerPing backendPing = server.peekServerPing();

        if (backendPing == null) {
            // Aucun ping abouti pour l'instant : on laisse la reponse du proxy.
            return;
        }

        response.setDescriptionComponent(backendPing.getDescriptionComponent());
        response.setOnlinePlayers(backendPing.getOnlinePlayers());
        response.setMaxPlayers(backendPing.getMaxPlayers());
        response.setSamplePlayers(backendPing.getSamplePlayers());

        CommonFavicon backendFavicon = backendPing.getFavicon();
        if (backendFavicon != null) {
            response.setFavicon(backendFavicon);
        }
    }

    /**
     * Serveur hors-ligne : on ressort son MOTD conservé, en désaturant son
     * favicon et en y ajoutant une pastille d'arrêt.
     * <p>
     * La version de protocole n'est plus forcée à -1 comme le faisait la 2.x.
     * Ce détournement affichait un texte rouge <em>à la place</em> du compteur
     * de joueurs : les deux occupent le même emplacement, et on veut désormais
     * afficher « 0 joueur ». L'explication passe donc par le survol.
     */
    private void handleOfflineServer(CommonServerPing response, MinecraftServer server) {
        ServerMotd motd = server.getMotd();
        if (motd.isEmpty()) {
            // Serveur jamais vu en ligne : rien de pertinent a afficher, on
            // laisse le proxy se presenter comme il l'entend.
            return;
        }

        applyCachedMotd(response, motd, MinecraftServerStatus.OFFLINE);
        response.setOnlinePlayers(0);
        response.setMaxPlayers(0);
        response.setSamplePlayers(buildOfflineHover(server.getServer()));
    }

    /**
     * Serveur en démarrage : même MOTD conservé, pastille ambre, et la file
     * d'attente listée au survol.
     */
    private void handleStartingServer(CommonServerPing response, MinecraftServer server) {
        ServerMotd motd = server.getMotd();
        if (motd.isEmpty()) {
            return;
        }

        applyCachedMotd(response, motd, MinecraftServerStatus.STARTING);
        response.setOnlinePlayers(0);
        response.setMaxPlayers(0);
        response.setSamplePlayers(buildStartingHover(server));
    }

    private void applyCachedMotd(CommonServerPing response, ServerMotd motd, MinecraftServerStatus status) {
        Component description = motd.getDescription();
        if (description != null) {
            response.setDescriptionComponent(description);
        }

        CommonFavicon favicon = motd.getFavicon(status);
        if (favicon != null) {
            response.setFavicon(favicon);
        }
    }

    private List<Component> buildOfflineHover(CommonServer commonServer) {
        List<Component> lines = new LinkedList<>();
        lines.add(Component.text(commonServer.getDisplayName()).color(NamedTextColor.YELLOW)
                .append(Component.text(" est hors-ligne").color(NamedTextColor.RED)));
        lines.add(Component.text(""));
        lines.add(Component.text("Connectez-vous pour le démarrer").color(NamedTextColor.GRAY));
        return lines;
    }

    private List<Component> buildStartingHover(MinecraftServer server) {
        List<Component> lines = new LinkedList<>();
        lines.add(Component.text(server.getServer().getDisplayName()).color(NamedTextColor.YELLOW)
                .append(Component.text(" démarre...").color(NamedTextColor.GOLD)));
        lines.add(Component.text(""));

        List<CommonPlayer> queue = server.getQueue();
        if (queue.isEmpty()) {
            lines.add(Component.text("Connectez-vous pour rejoindre").color(NamedTextColor.GRAY));
            lines.add(Component.text("la file d'attente").color(NamedTextColor.GRAY));
        } else {
            lines.add(Component.text("File d'attente (" + queue.size() + ") :").color(NamedTextColor.GRAY));
            for (CommonPlayer player : queue) {
                lines.add(Component.text(player.getDisplayName()).color(NamedTextColor.GRAY));
            }
        }
        return lines;
    }
}
