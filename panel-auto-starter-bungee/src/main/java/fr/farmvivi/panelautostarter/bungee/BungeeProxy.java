package fr.farmvivi.panelautostarter.bungee;

import fr.farmvivi.panelautostarter.bungee.ping.BungeeFavicon;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerDisconnectEvent;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeProxy implements CommonProxy, EventListener {
    private final ProxyServer proxyServer;

    private final Map<String, BungeeServer> serversCache = new HashMap<>();
    private final Map<UUID, BungeePlayer> playersCache = new HashMap<>();

    public BungeeProxy(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return proxyServer.getName();
    }

    @Override
    public String getVersion() {
        return proxyServer.getVersion();
    }

    @Override
    public CommonServer getServer(String name) {
        // Retrieve or create the server using computeIfAbsent
        return serversCache.computeIfAbsent(name, k -> {
            // Try to retrieve the server data from BungeeCord
            ServerInfo server = proxyServer.getServerInfo(k);
            // If the server exists, create and cache it
            return server != null ? new BungeeServer(server) : null;
        });
    }

    @Override
    public CommonServer[] getServers() {
        // Retrieve all the servers data from BungeeCord.
        // getServers() renvoie la map vivante du proxy : on en prend un instantane
        // avant d'iterer, sinon un rechargement de configuration concurrent peut
        // lever une ConcurrentModificationException. C'est la garantie qu'offrait
        // getServersCopy(), specifique a Waterfall et donc abandonne avec lui.
        Map<String, ServerInfo> servers = new HashMap<>(proxyServer.getServers());
        // For each server
        for (ServerInfo server : servers.values()) {
            // Retrieve the server name
            String name = server.getName();
            // Cache the server if it is not already cached
            serversCache.computeIfAbsent(name, k -> new BungeeServer(server));
        }

        // Return the servers
        return serversCache.values().toArray(new BungeeServer[0]);
    }

    @Override
    public CommonPlayer getPlayer(UUID uuid) {
        // Retrieve or create the player using computeIfAbsent
        return playersCache.computeIfAbsent(uuid, k -> {
            // Try to retrieve the player data from BungeeCord
            ProxiedPlayer player = proxyServer.getPlayer(k);
            // If the player exists, create and cache it
            return player != null ? new BungeePlayer(this, player) : null;
        });
    }

    @Override
    public CommonPlayer getPlayer(String name) {
        // Try to retrieve the player data from BungeeCord
        ProxiedPlayer player = proxyServer.getPlayer(name);
        // If the player exists
        if (player != null) {
            // Retrieve the player UUID
            UUID uuid = player.getUniqueId();

            // Return the player by its UUID
            return getPlayer(uuid);
        }

        // Return null if the player does not exist
        return null;
    }

    public CommonPlayer getPlayer(ProxiedPlayer player) {
        // Retrieve the player UUID
        UUID uuid = player.getUniqueId();
        // Cache the player if it is not already cached
        playersCache.computeIfAbsent(uuid, k -> new BungeePlayer(this, player));

        // Return the player
        return playersCache.get(uuid);
    }

    @Override
    public CommonPlayer[] getPlayers() {
        // Retrieve all the players data from BungeeCord
        for (ProxiedPlayer player : proxyServer.getPlayers()) {
            // Retrieve the player UUID
            UUID uuid = player.getUniqueId();
            // Cache the player if it is not already cached
            playersCache.computeIfAbsent(uuid, k -> new BungeePlayer(this, player));
        }

        // Return the players
        return playersCache.values().toArray(new BungeePlayer[0]);
    }

    @Override
    public void dispatchCommand(CommonPlayer player, String command) {
        if (player.getPlatformHandle() instanceof ProxiedPlayer proxied) {
            proxyServer.getPluginManager().dispatchCommand(proxied, command);
        }
    }

    @Override
    public void runAsync(CommonPlugin owner, Runnable task) {
        if (owner instanceof BungeePlugin bungeeOwner) {
            // Run the task asynchronously
            proxyServer.getScheduler().runAsync(bungeeOwner, task);
        }
    }

    @Override
    public void schedule(CommonPlugin owner, Runnable task, long delay, TimeUnit unit) {
        if (owner instanceof BungeePlugin bungeeOwner) {
            // Schedule the task
            proxyServer.getScheduler().schedule(bungeeOwner, task, delay, unit);
        }
    }

    @Override
    public void schedule(CommonPlugin owner, Runnable task, long delay, long period, TimeUnit unit) {
        if (owner instanceof BungeePlugin bungeeOwner) {
            // Schedule the task
            proxyServer.getScheduler().schedule(bungeeOwner, task, delay, period, unit);
        }
    }

    @Override
    public void cancel(CommonPlugin owner) {
        if (owner instanceof BungeePlugin bungeeOwner) {
            // Cancel the tasks of the plugin owner
            proxyServer.getScheduler().cancel(bungeeOwner);
        }
    }

    @Override
    public CommonFavicon createFavicon(BufferedImage image) {
        return new BungeeFavicon(Favicon.create(image));
    }

    @Override
    public Map<String, String> getForcedHosts() {
        return proxyServer.getConfig().getListeners().iterator().next().getForcedHosts();
    }

    @Override
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Retrieve the player
        CommonPlayer player = event.getPlayer();

        // If player is not null
        if (player != null) {
            // Remove the player from the cache
            playersCache.remove(player.getUniqueId());
        }
    }

    @Override
    public void onProxyPing(ProxyPingEvent event) {
        // Do nothing
    }

    @Override
    public void onServerConnect(ServerConnectEvent event) {
        // Do nothing
    }

    @Override
    public void onServerConnected(ServerConnectedEvent event) {
        // Do nothing : le proxy ne se sert de cet evenement pour aucun cache.
    }

    @Override
    public boolean supportsSound() {
        // BungeeCord n'a aucune API de son.
        return false;
    }

    @Override
    public void onPlayerKicked(PlayerKickedEvent event) {
        // Do nothing : le proxy ne se sert de cet evenement pour aucun cache.
    }
}
