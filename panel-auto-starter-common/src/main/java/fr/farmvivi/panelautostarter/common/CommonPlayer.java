package fr.farmvivi.panelautostarter.common;

import net.kyori.adventure.text.Component;

import java.time.Duration;
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
     * Displays a title and subtitle on the player's screen.
     *
     * @param title    the title, may be empty
     * @param subtitle the subtitle, may be empty
     * @param fadeIn   fade-in duration
     * @param stay     duration the title remains fully visible
     * @param fadeOut  fade-out duration
     */
    void showTitle(Component title, Component subtitle, Duration fadeIn, Duration stay, Duration fadeOut);

    /**
     * Joue un son au joueur, si la plateforme le permet.
     * <p>
     * Sans effet sur BungeeCord, qui n'expose aucune API de son ; voir
     * {@link CommonProxy#supportsSound()}.
     *
     * @param soundKey l'identifiant du son, par exemple {@code block.note_block.pling}
     * @param volume   le volume
     * @param pitch    la hauteur
     */
    void playSound(String soundKey, float volume, float pitch);

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
