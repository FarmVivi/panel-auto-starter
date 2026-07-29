package fr.farmvivi.panelautostarter.common;

import fr.farmvivi.panelautostarter.common.command.CommonCommand;
import fr.farmvivi.panelautostarter.common.listener.EventListener;

import java.io.File;

/**
 * Represents the plugin.
 */
public interface CommonPlugin {
    /**
     * Returns the data folder.
     *
     * @return the data folder
     */
    File getDataFolder();

    /**
     * Adds an event listener.
     *
     * @param eventListener the event listener
     */
    void addEventListener(EventListener eventListener);

    /**
     * Removes an event listener.
     *
     * @param eventListener the event listener
     */
    void removeEventListener(EventListener eventListener);

    /**
     * Enregistre une commande auprès du proxy.
     *
     * @param command la commande
     */
    void registerCommand(CommonCommand command);
}
