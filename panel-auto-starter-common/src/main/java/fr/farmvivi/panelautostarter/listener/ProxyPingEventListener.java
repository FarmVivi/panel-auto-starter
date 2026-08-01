package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.i18n.Translations;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.listener.EventAdapter;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.motd.BundledFavicons;
import fr.farmvivi.panelautostarter.motd.MotdSettings;
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
 * Chaque champ de la réponse — description, icône, compteurs, survol, étiquette
 * de version — se règle indépendamment dans {@code config.yml}, et séparément
 * pour l'état hors-ligne et l'état démarrage. Cette classe applique ces
 * réglages ; elle ne décide de rien par elle-même.
 */
public class ProxyPingEventListener extends EventAdapter {
    private final PanelAutoStarter plugin;
    private final Map<String, CommonServer> forcedHosts;
    private final MotdSettings settings;
    private final BundledFavicons bundledFavicons;

    public ProxyPingEventListener(PanelAutoStarter plugin) {
        this(plugin, plugin.getMotdSettings(),
                new BundledFavicons(plugin::getResourceAsStream, plugin.getProxy(),
                        message -> plugin.getLogger().warning(message)));
    }

    ProxyPingEventListener(PanelAutoStarter plugin, MotdSettings settings, BundledFavicons bundledFavicons) {
        this.plugin = plugin;
        this.settings = settings;
        this.bundledFavicons = bundledFavicons;
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

        MinecraftServerStatus status = server.getStatus();
        if (status == MinecraftServerStatus.ONLINE) {
            handleOnlineServer(response, server);
        } else {
            handleUnavailableServer(response, server, status);
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
     * quand on ping le proxy lui-meme.
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
     * Serveur hors-ligne ou en démarrage : chaque champ suit son réglage.
     */
    private void handleUnavailableServer(CommonServerPing response, MinecraftServer server,
                                         MinecraftServerStatus status) {
        MotdSettings.State state = settings.forStatus(status);
        ServerMotd motd = server.getMotd();

        applyDescription(response, state, motd, server.getServer(), status);
        applyFavicon(response, state, motd, status);
        applyPlayerCounts(response, state, motd, server);
        applyHover(response, state, server, status);
        applyVersionLabel(response, state, status);
    }

    private void applyDescription(CommonServerPing response, MotdSettings.State state, ServerMotd motd,
                                  CommonServer commonServer, MinecraftServerStatus status) {
        switch (state.description()) {
            case CACHED -> {
                Component cached = motd.getDescription();
                if (cached != null) {
                    response.setDescriptionComponent(cached);
                }
            }
            case PLUGIN -> response.setDescriptionComponent(buildDescription(commonServer, status));
            case PROXY -> {
                // Rien : la reponse du proxy est conservee.
            }
        }
    }

    private void applyFavicon(CommonServerPing response, MotdSettings.State state, ServerMotd motd,
                              MinecraftServerStatus status) {
        CommonFavicon favicon = switch (state.favicon()) {
            case CACHED -> motd.getFavicon(status);
            case BUNDLED -> bundledFavicons.get(status);
            case PROXY -> null;
        };
        if (favicon != null) {
            response.setFavicon(favicon);
        }
    }

    private void applyPlayerCounts(CommonServerPing response, MotdSettings.State state, ServerMotd motd,
                                   MinecraftServer server) {
        switch (state.players()) {
            case ZERO -> response.setOnlinePlayers(0);
            case QUEUE -> response.setOnlinePlayers(server.getQueue().size());
            case PROXY -> {
                // Rien.
            }
        }
        switch (state.maxPlayers()) {
            case ZERO -> response.setMaxPlayers(0);
            case CACHED -> response.setMaxPlayers(motd.getMaxPlayers());
            case PROXY -> {
                // Rien.
            }
        }
    }

    private void applyHover(CommonServerPing response, MotdSettings.State state, MinecraftServer server,
                            MinecraftServerStatus status) {
        switch (state.hover()) {
            case PLUGIN -> response.setSamplePlayers(buildHover(server, status));
            case NONE -> response.setSamplePlayers(new LinkedList<>());
            case PROXY -> {
                // Rien.
            }
        }
    }

    /**
     * L'étiquette de version remplace le compteur de joueurs côté client : une
     * version de protocole volontairement invalide fait afficher son libellé en
     * rouge, à l'emplacement du compteur.
     */
    private void applyVersionLabel(CommonServerPing response, MotdSettings.State state,
                                   MinecraftServerStatus status) {
        if (state.versionLabel() != MotdSettings.VersionLabel.TEXT) {
            return;
        }
        response.setProtocolVersion(-1);
        response.setProtocolName(status == MinecraftServerStatus.OFFLINE
                ? motd("motd.label.offline", NamedTextColor.RED)
                : motd("motd.label.starting", NamedTextColor.GOLD));
    }

    /**
     * Texte de MOTD, traduit tout de suite dans la langue de repli.
     * <p>
     * Un ping de liste de serveurs ne dit rien de la langue de celui qui l'a
     * émis — le client ne l'annonce qu'une fois connecté. Il n'y a donc pas de
     * meilleure réponse que la langue configurée, et rien ne servirait de
     * différer la traduction.
     */
    private static Component motd(String key, NamedTextColor color,
                                  net.kyori.adventure.text.ComponentLike... args) {
        return Translations.renderDefault(
                Component.translatable("panelautostarter." + key, args).color(color));
    }

    private Component buildDescription(CommonServer commonServer, MinecraftServerStatus status) {
        Component name = Component.text(commonServer.getDisplayName())
                .color(NamedTextColor.YELLOW);

        if (status == MinecraftServerStatus.OFFLINE) {
            return motd("motd.offline", NamedTextColor.RED, name)
                    .appendNewline()
                    .append(motd("motd.offline.hint", NamedTextColor.GRAY));
        }
        return motd("motd.starting", NamedTextColor.GOLD, name)
                .appendNewline()
                .append(motd("motd.starting.hint", NamedTextColor.GRAY));
    }

    private List<Component> buildHover(MinecraftServer server, MinecraftServerStatus status) {
        return status == MinecraftServerStatus.OFFLINE
                ? buildOfflineHover(server.getServer())
                : buildStartingHover(server);
    }

    private List<Component> buildOfflineHover(CommonServer commonServer) {
        List<Component> lines = new LinkedList<>();
        lines.add(motd("motd.hover.offline", NamedTextColor.RED,
                Component.text(commonServer.getDisplayName()).color(NamedTextColor.YELLOW)));
        lines.add(Component.text(""));
        lines.add(motd("motd.hover.offline.hint", NamedTextColor.GRAY));
        return lines;
    }

    private List<Component> buildStartingHover(MinecraftServer server) {
        List<Component> lines = new LinkedList<>();
        lines.add(motd("motd.hover.starting", NamedTextColor.GOLD,
                Component.text(server.getServer().getDisplayName()).color(NamedTextColor.YELLOW)));
        lines.add(Component.text(""));

        List<CommonPlayer> queue = server.getQueue();
        if (queue.isEmpty()) {
            lines.add(motd("motd.hover.starting.hint", NamedTextColor.GRAY));
        } else {
            lines.add(motd("motd.hover.queue", NamedTextColor.GRAY,
                    Component.text(queue.size())));
            for (CommonPlayer player : queue) {
                lines.add(Component.text(player.getDisplayName()).color(NamedTextColor.GRAY));
            }
        }
        return lines;
    }
}
