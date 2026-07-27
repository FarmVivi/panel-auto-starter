package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.panel.PanelClient;
import fr.farmvivi.panelautostarter.panel.PanelClientFactory;
import fr.farmvivi.panelautostarter.panel.PanelConfig;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.motd.MotdSettings;
import fr.farmvivi.panelautostarter.motd.MotdStore;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.listener.PlayerDisconnectEventListener;
import fr.farmvivi.panelautostarter.listener.ServerConnectedEventListener;
import fr.farmvivi.panelautostarter.listener.ProxyPingEventListener;
import fr.farmvivi.panelautostarter.listener.ServerConnectEventListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;

public final class PanelAutoStarter {
    public static final String NAME = PanelAutoStarter.class.getSimpleName();
    public static final String VERSION;
    public static final boolean PRODUCTION;
    private static final Logger LOGGER = Logger.getLogger(PanelAutoStarter.class.getName());
    private static PanelAutoStarter instance;

    static {
        Properties properties = new Properties();
        try {
            properties.load(PanelAutoStarter.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException e) {
            LOGGER.log(SEVERE, "ERROR: Cannot read properties file! Using default values", e);
            System.exit(1);
        }

        VERSION = properties.getProperty("version");
        PRODUCTION = !PanelAutoStarter.VERSION.contains("-SNAPSHOT");
    }

    private final CommonPlugin plugin;
    private final LoggerProxy delegateLogger;
    private final HashMap<CommonServer, MinecraftServer> servers = new HashMap<>();
    private CommonProxy proxy;
    private Configuration config;
    private PanelClient panel;
    private MotdStore motdStore;
    private MotdSettings motdSettings;
    private final PendingMessages pendingMessages = new PendingMessages();

    public PanelAutoStarter(CommonPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.delegateLogger = new LoggerProxy(logger);
    }

    public static PanelAutoStarter getInstance() {
        return instance;
    }

    public static void setPlugin(CommonPlugin plugin, Logger logger) {
        PanelAutoStarter.instance = new PanelAutoStarter(plugin, logger);
    }

    public void enable(CommonProxy proxy) {
        try {
            this.getLogger().info(Component.text(NAME).color(NamedTextColor.DARK_AQUA)
                    .append(Component.text(" v" + VERSION, NamedTextColor.YELLOW))
                    .append(Component.text(" running on "))
                    .append(Component.text(proxy.getName(), NamedTextColor.DARK_AQUA))
                    .append(Component.text(" v" + proxy.getVersion(), NamedTextColor.YELLOW))
                    .append(Component.text("..."))
            );

            this.proxy = proxy;

            // Load config
            this.loadConfig();

            // MOTD cache, kept on disk so it survives a proxy restart
            this.motdStore = new MotdStore(this.plugin.getDataFolder());

            // MOTD display settings. An unknown value falls back to its default
            // rather than preventing the plugin from starting: a cosmetic typo
            // must not cost players their servers.
            this.motdSettings = MotdSettings.from(config);
            for (String warning : this.motdSettings.getWarnings()) {
                this.getLogger().warning(warning);
            }

            // Connect to the panel
            PanelConfig panelConfig = PanelConfig.from(config);
            if (panelConfig.isLegacyFormat()) {
                this.getLogger().warning(PanelConfig.legacyFormatWarning());
            }
            this.panel = PanelClientFactory.create(panelConfig.getType(), panelConfig.getUrl(), panelConfig.getToken());
            this.getLogger().info(Component.text("Panel ")
                    .append(Component.text(panelConfig.getType().name(), NamedTextColor.DARK_AQUA))
                    .append(Component.text(" : "))
                    .append(Component.text(panelConfig.getUrl(), NamedTextColor.YELLOW)));

            // Initialize servers
            this.initServers();

            // Register listeners
            this.getLogger().info("Registering event listeners...");
            this.getPlugin().addEventListener(new ServerConnectEventListener(this));
            this.getPlugin().addEventListener(new ProxyPingEventListener(this));
            this.getPlugin().addEventListener(new PlayerDisconnectEventListener(this));
            this.getPlugin().addEventListener(new ServerConnectedEventListener(pendingMessages));
            this.getLogger().info("Event listeners registered.");

            this.getLogger().info("Plugin enabled.");
        } catch (Exception ex) {
            this.getLogger().log(SEVERE, "An error occurred while enabling the plugin.", ex);
        }
    }

    public void disable() {
        try {
            this.getLogger().info("Disabling plugin...");

            // Cancel tasks
            this.proxy.cancel(plugin);

            this.getLogger().info("Plugin disabled.");
        } catch (Exception ex) {
            this.getLogger().log(SEVERE, "An error occurred while disabling the plugin.", ex);
        }
    }

    private void loadConfig() throws IOException {
        this.getLogger().info("Loading config...");

        File dataFolder = this.plugin.getDataFolder();

        // Create plugin config folder if it doesn't exist
        if (!dataFolder.exists()) {
            this.getLogger().info("Creating config folder...");
            dataFolder.mkdirs();
            this.getLogger().info("Config folder created.");
        }

        File configFile = new File(dataFolder, "config.yml");

        // Copy default config if it doesn't exist
        if (!configFile.exists()) {
            FileOutputStream outputStream = new FileOutputStream(configFile); // Throws IOException
            InputStream in = this.getResourceAsStream("config.yml"); // This file must exist in the jar resources folder
            in.transferTo(outputStream); // Throws IOException
        }

        // Load config
        this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

        this.getLogger().info("Config loaded.");
    }

    private void initServers() {
        this.getLogger().info("Initializing servers...");

        for (CommonServer server : proxy.getServers()) {
            if (config.contains("servers." + server.getName())) {
                // Retrieve server panel identifier from config file
                String panelServerId = config.getString("servers." + server.getName() + ".id");

                // Registering the server
                this.getLogger().info("Registering " + server.getName() + " server with panel identifier " + panelServerId);

                // Retrieve panel server
                PanelServer panelServer = this.panel.retrieveServer(panelServerId);

                // Add server to the servers list
                ServerMotd motd = new ServerMotd(motdStore, server.getName(), proxy, motdSettings,
                        message -> this.getLogger().warning(message));
                servers.put(server, new MinecraftServer(this, server, panelServer, motd));

                // Server registered
                this.getLogger().info("Registered " + server.getName() + " server with panel identifier " + panelServerId);
            }
        }

        this.getLogger().info("Servers initialized.");
    }

    public InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    public CommonPlugin getPlugin() {
        return plugin;
    }

    public Map<CommonServer, MinecraftServer> getServers() {
        return servers;
    }

    public LoggerProxy getLogger() {
        return delegateLogger;
    }

    public CommonProxy getProxy() {
        return proxy;
    }

    public Configuration getConfig() {
        return config;
    }

    public PanelClient getPanel() {
        return panel;
    }

    /**
     * Retourne les réglages d'affichage du MOTD.
     *
     * @return les réglages, jamais null une fois le plugin activé
     */
    /**
     * Retourne les messages en attente de joueurs pas encore en mesure de les
     * recevoir.
     *
     * @return le registre des messages différés
     */
    public PendingMessages getPendingMessages() {
        return pendingMessages;
    }

    public MotdSettings getMotdSettings() {
        return motdSettings;
    }
}
