package fr.farmvivi.panelautostarter.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.listener.EventListener;
import fr.farmvivi.panelautostarter.velocity.listener.VelocityPlayerDisconnectEventListener;
import fr.farmvivi.panelautostarter.velocity.listener.VelocityProxyPingEventListener;
import fr.farmvivi.panelautostarter.velocity.listener.VelocityServerConnectEventListener;
import fr.farmvivi.panelautostarter.velocity.listener.VelocityServerConnectedEventListener;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

public class VelocityPlugin implements CommonPlugin {
    /**
     * Identifiant du plugin. Doit rester identique au champ {@code id} de
     * {@code velocity-plugin.json} : Velocity impose le motif
     * {@code [a-z][a-z0-9-_]{0,63}}, d'ou l'absence de majuscules, et cet
     * identifiant determine aussi le nom du dossier de donnees.
     */
    public static final String PLUGIN_ID = "panelautostarter";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private final Collection<EventListener> eventListeners = new LinkedList<>();

    @Inject
    public VelocityPlugin(ProxyServer server, @DataDirectory Path dataDirectory) {
        this.server = server;
        // Le logger n'est volontairement pas injecte. Velocity ne fournit pas de
        // binding pour java.util.logging.Logger : l'injection retombait sur celui
        // integre a Guice, qui nomme le logger d'apres la classe cible, d'ou le
        // "fr.farmvivi.panelautostarter.velocity.VelocityPlugin" a rallonge en
        // console.
        //
        // On le nomme donc d'apres l'identifiant du plugin, comme le fait Velocity
        // pour le logger SLF4J qu'il fournit : c'est ce qui donne [luckperms] ou
        // [viaversion] en console. Le module BungeeCord garde de son cote un nom
        // en CamelCase, conforme a l'usage de cette plateforme.
        this.logger = Logger.getLogger(PLUGIN_ID);
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        // Setting the plugin
        PanelAutoStarter.setPlugin(this, logger);

        // Building the VelocityProxy
        VelocityProxy proxy = new VelocityProxy(server);

        // Registering the proxy as an event listener
        eventListeners.add(proxy);

        // Enabling the PanelAutoStarter
        PanelAutoStarter.getInstance().enable(proxy);

        // Registering the event listeners
        this.logger.info("Registering Velocity event listeners...");
        this.server.getEventManager().register(this, new VelocityServerConnectEventListener(proxy, eventListeners));
        this.server.getEventManager().register(this, new VelocityProxyPingEventListener(eventListeners));
        this.server.getEventManager().register(this, new VelocityPlayerDisconnectEventListener(proxy, eventListeners));
        this.server.getEventManager().register(this, new VelocityServerConnectedEventListener(proxy, eventListeners));
        this.logger.info("Velocity Event listeners registered.");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        PanelAutoStarter.getInstance().disable();
    }

    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public void addEventListener(EventListener eventListener) {
        this.eventListeners.add(eventListener);
    }

    @Override
    public void removeEventListener(EventListener eventListener) {
        this.eventListeners.remove(eventListener);
    }
}
