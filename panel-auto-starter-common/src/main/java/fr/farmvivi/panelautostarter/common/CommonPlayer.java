package fr.farmvivi.panelautostarter.common;

import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Represents a player.
 */
public interface CommonPlayer {
    /**
     * Returns whether the player is online.
     *
     * @return whether the player is online
     */
    boolean isOnline();

    /**
     * Returns the username of the player.
     *
     * @return the username of the player
     */
    String getUsername();

    /**
     * Returns the display name of the player.
     *
     * @return the display name of the player
     */
    String getDisplayName();

    /**
     * Returns the UUID of the player.
     *
     * @return the UUID of the player
     */
    UUID getUniqueId();

    /**
     * Sends a message to the player.
     *
     * @param message the message to send
     */
    void sendMessage(String message);

    /**
     * Sends a message to the player.
     *
     * @param message the message to send
     */
    void sendMessage(Component message);

    /**
     * Connects the player to the server.
     *
     * @param server the server to connect to
     */
    void connectToServer(CommonServer server);

    /**
     * Retrieve the server the player is connected to.
     *
     * @return the server the player is connected to
     */
    CommonServer getServer();
}
