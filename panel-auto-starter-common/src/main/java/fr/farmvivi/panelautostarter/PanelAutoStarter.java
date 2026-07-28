package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.config.ConfigDiff;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.panel.PanelClient;
import fr.farmvivi.panelautostarter.panel.PanelClientFactory;
import fr.farmvivi.panelautostarter.panel.PanelConfig;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.motd.MotdSettings;
import fr.farmvivi.panelautostarter.motd.MotdStore;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.listener.PlayerDisconnectEventListener;
import fr.farmvivi.panelautostarter.listener.PlayerKickedEventListener;
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
    private MessageSettings messageSettings;
    private final PendingNotifications pendingNotifications = new PendingNotifications();

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

            // Player feedback: titles and countdown sounds
            this.messageSettings = MessageSettings.from(config);
            this.warnIfSoundIsUnsupported();

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
            this.getPlugin().addEventListener(new ServerConnectedEventListener(pendingNotifications));
            this.getPlugin().addEventListener(new PlayerKickedEventListener(this));
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

        this.reportMissingKeys();

        this.getLogger().info("Config loaded.");
    }

    /**
     * Signale les réglages apparus dans une mise à jour et absents du fichier
     * de l'utilisateur.
     * <p>
     * Le config.yml n'est copié qu'à la première installation : une nouvelle
     * option n'apparaît jamais chez les installations existantes, qui héritent
     * silencieusement de sa valeur par défaut. L'administrateur cherche alors un
     * réglage dans un fichier qui ne le contient pas.
     * <p>
     * Le fichier n'est pas réécrit pour autant : cela écraserait ses choix et
     * ses commentaires.
     */
    private void reportMissingKeys() {
        try (InputStream bundled = this.getResourceAsStream("config.yml")) {
            if (bundled == null) {
                return;
            }
            Configuration reference = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new java.io.InputStreamReader(bundled, java.nio.charset.StandardCharsets.UTF_8));

            java.util.List<String> missing = ConfigDiff.missingKeys(config, reference);
            if (missing.isEmpty()) {
                return;
            }

            this.getLogger().warning(missing.size() + " reglage(s) absent(s) de votre config.yml utilisent "
                    + "leur valeur par defaut. Ajoutez-les pour les personnaliser :");
            for (String key : missing) {
                this.getLogger().warning("  - " + key);
            }
        } catch (IOException | RuntimeException ex) {
            // Purement informatif : un echec ne doit pas empecher le demarrage.
            this.getLogger().warning("Impossible de comparer votre config.yml a celui livre : " + ex.getMessage());
        }
    }

    /**
     * Signale un son configuré sur une plateforme qui ne sait pas en jouer.
     * <p>
     * BungeeCord n'expose aucune API de son. Sans cet avertissement, un
     * administrateur croirait à un réglage cassé plutôt qu'à une limite du
     * proxy.
     */
    private void warnIfSoundIsUnsupported() {
        if (!messageSettings.getCountdown().usesSound() || proxy.supportsSound()) {
            return;
        }
        this.getLogger().warning("Un son de compte a rebours est configure, mais "
                + proxy.getName() + " n'expose aucune API de son : les cles "
                + "'queue.countdown.tick-sound' et 'queue.countdown.go-sound' resteront sans effet. "
                + "Le decompte s'affiche normalement a l'ecran.");
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
    public PendingNotifications getPendingNotifications() {
        return pendingNotifications;
    }

    /**
     * Retourne les réglages des retours visuels et sonores aux joueurs.
     *
     * @return les réglages, jamais null une fois le plugin activé
     */
    public MessageSettings getMessageSettings() {
        return messageSettings;
    }

    public MotdSettings getMotdSettings() {
        return motdSettings;
    }
}
