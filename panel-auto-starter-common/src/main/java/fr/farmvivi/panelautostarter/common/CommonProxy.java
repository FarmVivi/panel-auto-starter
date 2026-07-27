package fr.farmvivi.panelautostarter.common;

import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface CommonProxy {
    /**
     * Returns the proxy name.
     *
     * @return the proxy name
     */
    String getName();

    /**
     * Returns the proxy version.
     *
     * @return the proxy version
     */
    String getVersion();

    /**
     * Returns the server by its name.
     *
     * @param name the name
     * @return the server
     */
    CommonServer getServer(String name);

    /**
     * Returns all servers.
     *
     * @return all servers
     */
    CommonServer[] getServers();

    /**
     * Returns the player by its UUID.
     *
     * @param uuid the UUID
     * @return the player
     */
    CommonPlayer getPlayer(UUID uuid);

    /**
     * Returns the player by its name.
     *
     * @param name the name
     * @return the player
     */
    CommonPlayer getPlayer(String name);

    /**
     * Returns all players.
     *
     * @return all players
     */
    CommonPlayer[] getPlayers();

    /**
     * Run a task asynchronously.
     *
     * @param owner the plugin owner
     * @param task  the task
     */
    void runAsync(CommonPlugin owner, Runnable task);

    /**
     * Schedule a task asynchronously.
     *
     * @param owner the plugin owner
     * @param task  the task
     * @param delay the delay
     * @param unit  the unit
     */
    void schedule(CommonPlugin owner, Runnable task, long delay, TimeUnit unit);

    /**
     * Schedule a task asynchronously.
     *
     * @param owner  the plugin owner
     * @param task   the task
     * @param delay  the delay
     * @param period the period
     * @param unit   the unit
     */
    void schedule(CommonPlugin owner, Runnable task, long delay, long period, TimeUnit unit);

    /**
     * Cancel tasks of the plugin owner.
     *
     * @param owner the plugin owner
     */
    void cancel(CommonPlugin owner);

    /**
     * Returns the favicon from the image.
     * The image must be 64x64 pixels.
     *
     * @param image the image
     * @return the favicon
     */
    CommonFavicon createFavicon(BufferedImage image);

    /**
     * Returns the forced hosts.
     * The key is the forced host and the value is the server name.
     *
     * @return the forced hosts
     */
    Map<String, String> getForcedHosts();
}
