package fr.farmvivi.panelautostarter.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.util.Favicon;
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
import fr.farmvivi.panelautostarter.velocity.ping.VelocityFavicon;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class VelocityProxy implements CommonProxy, EventListener {
    private final ProxyServer proxyServer;

    private final Map<String, VelocityServer> serversCache = new HashMap<>();
    private final Map<UUID, VelocityPlayer> playersCache = new HashMap<>();

    public VelocityProxy(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public String getName() {
        return proxyServer.getVersion().getName();
    }

    @Override
    public String getVersion() {
        return proxyServer.getVersion().getVersion();
    }

    @Override
    public CommonServer getServer(String name) {
        // Try to get the server from the cache
        VelocityServer velocityServer = serversCache.get(name);
        // If the server is not in the cache
        if (velocityServer == null) {
            // Try to retrieve the server data from Velocity
            Optional<RegisteredServer> server = proxyServer.getServer(name);
            // If the server exists
            if (server.isPresent()) {
                // Create the server
                velocityServer = new VelocityServer(server.get());
                // Cache the server
                serversCache.put(name, velocityServer);
            }
        }

        // Return the server
        return velocityServer;
    }

    @Override
    public CommonServer[] getServers() {
        // Retrieve all the servers data from Velocity
        Collection<RegisteredServer> servers = proxyServer.getAllServers();
        // For each server
        for (RegisteredServer server : servers) {
            // Retrieve the server name
            String name = server.getServerInfo().getName();
            // Cache the server if it is not already cached
            serversCache.computeIfAbsent(name, k -> new VelocityServer(server));
        }

        // Return the servers
        return serversCache.values().toArray(new VelocityServer[0]);
    }

    @Override
    public CommonPlayer getPlayer(UUID uuid) {
        // Try to get the player from the cache
        VelocityPlayer velocityPlayer = playersCache.get(uuid);
        // If the player is not in the cache
        if (velocityPlayer == null) {
            // Try to retrieve the player data from Velocity
            Optional<Player> player = proxyServer.getPlayer(uuid);
            // If the player exists
            if (player.isPresent()) {
                // Create the player
                velocityPlayer = new VelocityPlayer(this, player.get());
                // Cache the player
                playersCache.put(uuid, velocityPlayer);
            }
        }

        // Return the player
        return velocityPlayer;
    }

    @Override
    public CommonPlayer getPlayer(String name) {
        // Try to retrieve the player data from Velocity
        Optional<Player> player = proxyServer.getPlayer(name);
        // If the player exists
        if (player.isPresent()) {
            // Retrieve the player UUID
            UUID uuid = player.get().getUniqueId();

            // Return the player by its UUID
            return getPlayer(uuid);
        }

        // Return null if the player does not exist
        return null;
    }

    public CommonPlayer getPlayer(Player player) {
        // Retrieve the player UUID
        UUID uuid = player.getUniqueId();
        // Cache the player if it is not already cached
        playersCache.computeIfAbsent(uuid, k -> new VelocityPlayer(this, player));

        // Return the player
        return playersCache.get(uuid);
    }

    @Override
    public CommonPlayer[] getPlayers() {
        // Retrieve all the players data from Velocity
        Collection<Player> players = proxyServer.getAllPlayers();
        // For each player
        for (Player player : players) {
            // Retrieve the player UUID
            UUID uuid = player.getUniqueId();
            // Cache the player if it is not already cached
            playersCache.computeIfAbsent(uuid, k -> new VelocityPlayer(this, player));
        }

        // Return the players
        return playersCache.values().toArray(new VelocityPlayer[0]);
    }

    @Override
    public void runAsync(CommonPlugin owner, Runnable task) {
        if (owner instanceof VelocityPlugin velocityOwner) {
            // Run the task asynchronously
            proxyServer.getScheduler().buildTask(velocityOwner, task).schedule();
        }
    }

    @Override
    public void schedule(CommonPlugin owner, Runnable task, long delay, TimeUnit unit) {
        if (owner instanceof VelocityPlugin velocityOwner) {
            // Schedule the task
            proxyServer.getScheduler().buildTask(velocityOwner, task).delay(delay, unit).schedule();
        }
    }

    @Override
    public void schedule(CommonPlugin owner, Runnable task, long delay, long period, TimeUnit unit) {
        if (owner instanceof VelocityPlugin velocityOwner) {
            // Schedule the task
            proxyServer.getScheduler().buildTask(velocityOwner, task).delay(delay, unit).repeat(period, unit).schedule();
        }
    }

    @Override
    public void cancel(CommonPlugin owner) {
        if (owner instanceof VelocityPlugin velocityOwner) {
            // Cancel all the tasks of the plugin
            proxyServer.getScheduler().tasksByPlugin(velocityOwner).forEach(ScheduledTask::cancel);
        }
    }

    @Override
    public CommonFavicon createFavicon(BufferedImage image) {
        return new VelocityFavicon(Favicon.create(image));
    }

    @Override
    public Map<String, String> getForcedHosts() {
        Map<String, String> forcedHosts = new HashMap<>();

        // Retrieve all the forced hosts from Velocity
        for (Map.Entry<String, List<String>> entry : proxyServer.getConfiguration().getForcedHosts().entrySet()) {
            forcedHosts.put(entry.getKey(), entry.getValue().get(0));
        }

        return forcedHosts;
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
        // Velocity expose Adventure : les joueurs sont des Audience.
        return true;
    }

    @Override
    public void onPlayerKicked(PlayerKickedEvent event) {
        // Do nothing : le proxy ne se sert de cet evenement pour aucun cache.
    }
}
