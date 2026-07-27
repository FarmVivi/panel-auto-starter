package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

public class ProxyPingEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;
    private final Map<String, CommonServer> forcedHosts;
    private final CommonFavicon offlineFavicon;
    private final CommonFavicon startingFavicon;

    public ProxyPingEventListener(PanelAutoStarter plugin) {
        this.plugin = plugin;
        this.forcedHosts = new TreeMap<>();
        for (Map.Entry<String, String> entry : plugin.getProxy().getForcedHosts().entrySet()) {
            forcedHosts.put(entry.getKey(), plugin.getProxy().getServer(entry.getValue()));
        }
        CommonFavicon offlineFaviconTemp;
        CommonFavicon startingFaviconTemp;
        try {
            offlineFaviconTemp = plugin.getProxy().createFavicon(ImageIO.read(plugin.getResourceAsStream("img/offline.png")));
            startingFaviconTemp = plugin.getProxy().createFavicon(ImageIO.read(plugin.getResourceAsStream("img/starting.png")));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de lire les favicon !", e);
            offlineFaviconTemp = null;
            startingFaviconTemp = null;
        }
        this.offlineFavicon = offlineFaviconTemp;
        this.startingFavicon = startingFaviconTemp;
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
            case OFFLINE -> handleOfflineServer(response, commonServer);
            case STARTING -> handleStartingServer(response, server, commonServer);
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

    private void handleOfflineServer(CommonServerPing response, CommonServer commonServer) {
        List<Component> players = buildOfflinePlayerList(commonServer);
        response.setMaxPlayers(0);
        response.setOnlinePlayers(0);
        response.setSamplePlayers(players);
        response.setDescriptionComponent(buildOfflineDescription(commonServer));
        response.setProtocolVersion(-1);
        response.setProtocolName(Component.text("Hors-ligne").color(NamedTextColor.RED));
        if (offlineFavicon != null) {
            response.setFavicon(offlineFavicon);
        }
    }

    private void handleStartingServer(CommonServerPing response, MinecraftServer server, CommonServer commonServer) {
        int nbPlayers = server.getQueue().size();
        List<Component> players = buildStartingPlayerList(nbPlayers, server, commonServer);
        response.setMaxPlayers(-1);
        response.setOnlinePlayers(nbPlayers);
        response.setSamplePlayers(players);
        response.setDescriptionComponent(buildStartingDescription(commonServer));
        response.setProtocolVersion(-1);
        response.setProtocolName(Component.text("Démarrage...").color(NamedTextColor.GOLD));
        if (startingFavicon != null) {
            response.setFavicon(startingFavicon);
        }
    }

    private List<Component> buildOfflinePlayerList(CommonServer commonServer) {
        List<Component> players = new LinkedList<>();
        players.add(Component.text("Hors-ligne").color(NamedTextColor.RED));
        players.add(Component.text(""));
        players.add(Component.text("Connectez-vous").color(NamedTextColor.GRAY));
        players.add(Component.text("pour démarrer").color(NamedTextColor.GRAY));
        players.add(Component.text(commonServer.getDisplayName()).color(NamedTextColor.YELLOW));
        return players;
    }

    private Component buildOfflineDescription(CommonServer commonServer) {
        return Component.text("Serveur ")
            .append(Component.text(commonServer.getDisplayName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" hors-ligne").color(NamedTextColor.RED))
            .appendNewline()
            .append(Component.text("Connectez-vous pour le démarrer !").color(NamedTextColor.GRAY));
    }

    private List<Component> buildStartingPlayerList(int nbPlayers, MinecraftServer server, CommonServer commonServer) {
        List<Component> players = new LinkedList<>();
        players.add(Component.text("Démarrage...").color(NamedTextColor.GOLD));
        players.add(Component.text(""));
        players.add(Component.text("Connectez-vous").color(NamedTextColor.GRAY));
        players.add(Component.text("pour rejoindre la").color(NamedTextColor.GRAY));
        players.add(Component.text("file d'attente de").color(NamedTextColor.GRAY));
        players.add(Component.text(commonServer.getDisplayName()).color(NamedTextColor.YELLOW));
        
        if (nbPlayers > 0) {
            players.add(Component.text(""));
            players.add(Component.text("File d'attente :").color(NamedTextColor.GRAY));
            for (CommonPlayer player : server.getQueue()) {
                players.add(Component.text(player.getDisplayName()).color(NamedTextColor.GRAY));
            }
        }
        return players;
    }

    private Component buildStartingDescription(CommonServer commonServer) {
        return Component.text("Serveur ")
            .append(Component.text(commonServer.getDisplayName()).color(NamedTextColor.YELLOW))
            .append(Component.text(" en démarrage...").color(NamedTextColor.GOLD))
            .appendNewline()
            .append(Component.text("Connectez-vous pour rejoindre la file d'attente !").color(NamedTextColor.GRAY));
    }
}