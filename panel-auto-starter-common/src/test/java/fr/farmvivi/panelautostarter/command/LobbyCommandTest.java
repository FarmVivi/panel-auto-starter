package fr.farmvivi.panelautostarter.command;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.queue.QueueCoordinator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie la commande de retour au serveur d'attente.
 */
public class LobbyCommandTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private PanelServer mockPanelServer;

    private CommonServer limbo;
    private MockCommonServer survie;
    private MinecraftServer survieServer;
    private LobbyCommand command;

    /** Auteur de commande adossé à un joueur, ce que la commande exige. */
    private record PlayerSource(CommonPlayer player, List<Component> received)
            implements CommonCommandSource {

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }

        @Override
        public void sendMessage(Component message) {
            received.add(message);
        }

        @Override
        public String getName() {
            return player == null ? "Console" : player.getUsername();
        }

        @Override
        public CommonPlayer asPlayer() {
            return player;
        }
    }

    private PlayerSource sourceFor(CommonPlayer player) {
        return new PlayerSource(player, new ArrayList<>());
    }

    private static String plain(List<Component> received) {
        StringBuilder builder = new StringBuilder();
        for (Component component : received) {
            builder.append(PlainTextComponentSerializer.plainText().serialize(component))
                    .append('\n');
        }
        return builder.toString();
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());

        limbo = new MockCommonServer("lobby");
        survie = new MockCommonServer("survie");
        when(mockConfiguration.getString("queue.server")).thenReturn("lobby");
        when(mockProxy.getServer("lobby")).thenReturn(limbo);

        survieServer = new MinecraftServer(mockPlugin, survie, mockPanelServer,
                mock(ServerMotd.class));
        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(survie, survieServer);
        doReturn(servers).when(mockPlugin).getServers();
        when(mockPlugin.getQueueCoordinator()).thenReturn(new QueueCoordinator(mockPlugin));

        command = new LobbyCommand(mockPlugin, LobbyCommandSettings.defaults());
    }

    @Test
    public void testAPlayerElsewhereIsSentBack() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        player.setCurrentServer(survie);

        command.execute(sourceFor(player), new String[0]);

        assertEquals(limbo, player.getServer(), "Le joueur doit avoir ete renvoye");
        assertEquals(1, player.getConnectToServerCallCount());
    }

    /**
     * Le renvoyer là où il est provoquerait un aller-retour visible à l'écran
     * pour rien.
     */
    @Test
    public void testAPlayerAlreadyThereIsNotMoved() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        player.setCurrentServer(limbo);
        PlayerSource source = sourceFor(player);

        command.execute(source, new String[0]);

        assertEquals(0, player.getConnectToServerCallCount(), "Aucun aller-retour inutile");
        assertTrue(plain(source.received()).contains("déjà"), plain(source.received()));
    }

    /**
     * Le cœur de la décision : revenir au serveur d'attente, c'est renoncer.
     * Rester inscrit téléporterait le joueur alors qu'il vient de dire, par son
     * geste, qu'il n'attendait plus.
     */
    @Test
    public void testGoingBackLeavesTheQueue() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        player.setCurrentServer(survie);
        survieServer.enqueue(player);
        assertTrue(survieServer.isQueued(player));

        command.execute(sourceFor(player), new String[0]);

        assertFalse(survieServer.isQueued(player), "Revenir au lobby doit quitter la file");
    }

    @Test
    public void testTheConsoleIsToldItIsNotAPlayer() {
        PlayerSource source = new PlayerSource(null, new ArrayList<>());

        command.execute(source, new String[0]);

        assertTrue(plain(source.received()).contains("joueur"), plain(source.received()));
    }

    /**
     * Un serveur d'attente mal nommé dans la configuration ne doit pas produire
     * une commande qui ne fait rien sans rien dire.
     */
    @Test
    public void testAMissingLobbyIsReported() {
        when(mockProxy.getServer("lobby")).thenReturn(null);
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        player.setCurrentServer(survie);
        PlayerSource source = sourceFor(player);

        command.execute(source, new String[0]);

        assertTrue(plain(source.received()).contains("introuvable"), plain(source.received()));
        assertEquals(0, player.getConnectToServerCallCount());
    }

    // ===================== Reglages =====================

    @Test
    public void testTheNameAndAliasesAreConfigurable() {
        Configuration config = new Configuration();
        config.set("queue.back-command.name", "spawn");
        config.set("queue.back-command.aliases", List.of("retour"));

        LobbyCommandSettings settings = LobbyCommandSettings.from(config);

        assertEquals("spawn", settings.name());
        assertEquals(List.of("retour"), settings.aliases());
    }

    /**
     * Un alias identique au nom ferait refuser l'enregistrement entier sur
     * certains proxys, pour un doublon sans intérêt.
     */
    @Test
    public void testAnAliasEqualToTheNameIsDropped() {
        Configuration config = new Configuration();
        config.set("queue.back-command.name", "lobby");
        config.set("queue.back-command.aliases", List.of("LOBBY", "hub"));

        assertEquals(List.of("hub"), LobbyCommandSettings.from(config).aliases());
    }

    @Test
    public void testAnEmptyNameFallsBackToTheDefault() {
        Configuration config = new Configuration();
        config.set("queue.back-command.name", "   ");

        assertEquals("lobby", LobbyCommandSettings.from(config).name());
    }

    @Test
    public void testAbsentConfigurationFallsBackToDefaults() {
        assertEquals(LobbyCommandSettings.defaults(), LobbyCommandSettings.from(null));
    }

    @Test
    public void testTheCommandCanBeTurnedOff() {
        Configuration config = new Configuration();
        config.set("queue.back-command.enabled", false);

        assertFalse(LobbyCommandSettings.from(config).enabled());
    }
}
