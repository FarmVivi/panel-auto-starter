package fr.farmvivi.panelautostarter.command;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.access.Whitelist;
import fr.farmvivi.panelautostarter.access.WhitelistStore;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie la commande d'administration, et surtout que ses droits se jouent
 * serveur par serveur : un réseau confie souvent un serveur à quelqu'un sans
 * lui donner la main sur les autres.
 */
public class AdminCommandTest {

    @TempDir
    Path tempDir;

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private PanelServer surviePanel;

    @Mock
    private PanelServer creatifPanel;

    private MockCommonServer survie;
    private MockCommonServer creatif;
    private MinecraftServer survieServer;
    private MinecraftServer creatifServer;
    private WhitelistStore whitelists;
    private AdminCommand command;

    /**
     * Auteur de commande simulé, dont on choisit les permissions et dont on
     * relit ce qu'il a reçu.
     */
    private static final class FakeSource implements CommonCommandSource {
        private final Set<String> permissions = new HashSet<>();
        private final List<Component> received = new ArrayList<>();

        FakeSource grant(String permission) {
            permissions.add(permission);
            return this;
        }

        @Override
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }

        @Override
        public void sendMessage(Component message) {
            received.add(message);
        }

        @Override
        public String getName() {
            return "Testeur";
        }

        @Override
        public CommonPlayer asPlayer() {
            return null;
        }

        String text() {
            StringBuilder builder = new StringBuilder();
            for (Component component : received) {
                builder.append(PlainTextComponentSerializer.plainText().serialize(component))
                        .append('\n');
            }
            return builder.toString();
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());
        when(mockProxy.getPlayers()).thenReturn(new CommonPlayer[0]);

        survie = new MockCommonServer("survie");
        creatif = new MockCommonServer("creatif");
        survieServer = new MinecraftServer(mockPlugin, survie, surviePanel, mock(ServerMotd.class));
        creatifServer = new MinecraftServer(mockPlugin, creatif, creatifPanel, mock(ServerMotd.class));

        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(survie, survieServer);
        servers.put(creatif, creatifServer);
        doReturn(servers).when(mockPlugin).getServers();

        whitelists = new WhitelistStore(tempDir.toFile(), message -> {
        });
        when(mockPlugin.getWhitelistStore()).thenReturn(whitelists);

        command = new AdminCommand(mockPlugin);
    }

    private FakeSource globalAdmin() {
        return new FakeSource().grant("panelautostarter.admin");
    }

    private FakeSource creatifAdmin() {
        return new FakeSource().grant("panelautostarter.admin.creatif");
    }

    // ===================== Droits =====================

    @Test
    public void testSomeoneWithoutAnyRightIsTurnedAway() {
        FakeSource source = new FakeSource();

        command.execute(source, new String[]{"status"});

        assertTrue(source.text().contains("administrez pas"), source.text());
    }

    /**
     * Le cœur de la décision : administrer un serveur ne donne pas la main sur
     * les autres.
     */
    @Test
    public void testAServerAdminCannotTouchAnotherServer() {
        FakeSource source = creatifAdmin();

        command.execute(source, new String[]{"start", "survie"});

        assertTrue(source.text().contains("administrez pas"), source.text());
        verify(surviePanel, never()).start();
    }

    @Test
    public void testAServerAdminActsOnHisOwnServer() {
        FakeSource source = creatifAdmin();

        command.execute(source, new String[]{"start", "creatif"});

        assertEquals(MinecraftServerStatus.STARTING, creatifServer.getStatus());
        verify(creatifPanel).start();
    }

    /**
     * Lister ce qu'on n'administre pas reviendrait à annoncer ce qui existe.
     */
    @Test
    public void testStatusOnlyListsWhatTheAuthorAdministers() {
        FakeSource source = creatifAdmin();

        command.execute(source, new String[]{"status"});

        assertTrue(source.text().contains("creatif"), source.text());
        assertFalse(source.text().contains("survie"), source.text());
    }

    @Test
    public void testGlobalAdminSeesEverything() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"status"});

        assertTrue(source.text().contains("creatif"));
        assertTrue(source.text().contains("survie"));
    }

    // ===================== Demarrage et arret =====================

    @Test
    public void testStartingAnAlreadyRunningServerDoesNothing() {
        survie.simulatePingResponse(new MockCommonServerPing());
        assertEquals(MinecraftServerStatus.ONLINE, survieServer.getStatus());
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"start", "survie"});

        verify(surviePanel, never()).start();
        assertTrue(source.text().contains("déjà"), source.text());
    }

    @Test
    public void testStoppingAnOfflineServerDoesNothing() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"stop", "survie"});

        verify(surviePanel, never()).stop();
        assertTrue(source.text().contains("déjà"), source.text());
    }

    @Test
    public void testStoppingARunningServer() {
        survie.simulatePingResponse(new MockCommonServerPing());
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"stop", "survie"});

        verify(surviePanel).stop();
    }

    @Test
    public void testAnUnknownServerIsReported() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"start", "inexistant"});

        assertTrue(source.text().contains("Serveur inconnu"), source.text());
    }

    // ===================== Liste blanche =====================

    @Test
    public void testTheWhitelistCanBeToggledAndIsPersisted() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "on"});

        assertTrue(whitelists.get("survie").isEnabled());
        assertTrue(new WhitelistStore(tempDir.toFile(), m -> {
        }).get("survie").isEnabled(), "L'activation doit avoir ete ecrite sur disque");
    }

    /**
     * Une liste vide que l'on vient d'activer ferme le serveur à tout le monde.
     */
    @Test
    public void testEnablingAnEmptyWhitelistWarnsLoudly() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "on"});

        assertTrue(source.text().contains("vide"), source.text());
    }

    @Test
    public void testAnOnlinePlayerCanBeAddedByName() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        when(mockProxy.getPlayer("Vivi")).thenReturn(player);
        when(mockProxy.getPlayer(player.getUniqueId())).thenReturn(player);
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "add", "Vivi"});

        assertTrue(whitelists.get("survie").contains(player.getUniqueId()));
    }

    @Test
    public void testAPlayerCanBeAddedByUuid() {
        UUID uuid = UUID.randomUUID();
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "add", uuid.toString()});

        assertTrue(whitelists.get("survie").contains(uuid));
    }

    /**
     * Le besoin réel : retirer quelqu'un qui n'est pas connecté. S'en tenir aux
     * joueurs présents serait exactement l'inverse de l'usage.
     */
    @Test
    public void testAnOfflinePlayerCanBeRemovedByName() {
        UUID uuid = UUID.randomUUID();
        Whitelist whitelist = whitelists.get("survie");
        whitelist.add(uuid, "Absent");
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "remove", "Absent"});

        assertFalse(whitelist.contains(uuid), "Un inscrit doit pouvoir etre retire en son absence");
    }

    @Test
    public void testAnUnknownPlayerIsReported() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "add", "Fantome"});

        assertTrue(source.text().contains("introuvable"), source.text());
        assertEquals(0, whitelists.get("survie").size());
    }

    @Test
    public void testTheWhitelistCanBeListed() {
        whitelists.get("survie").add(UUID.randomUUID(), "Inscrit");
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"whitelist", "survie"});

        assertTrue(source.text().contains("Inscrit"), source.text());
    }

    @Test
    public void testAServerAdminCannotEditAnotherWhitelist() {
        FakeSource source = creatifAdmin();

        command.execute(source, new String[]{"whitelist", "survie", "on"});

        assertFalse(whitelists.get("survie").isEnabled());
        assertTrue(source.text().contains("administrez pas"), source.text());
    }

    // ===================== Aide et completion =====================

    @Test
    public void testAnEmptyCommandShowsTheUsage() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[0]);

        assertTrue(source.text().contains("/pas status"), source.text());
    }

    @Test
    public void testAnUnknownSubcommandShowsTheUsage() {
        FakeSource source = globalAdmin();

        command.execute(source, new String[]{"nimportequoi"});

        assertTrue(source.text().contains("/pas status"), source.text());
    }

    @Test
    public void testCompletionOffersSubcommands() {
        assertTrue(command.suggest(globalAdmin(), new String[]{""}).contains("status"));
        assertEquals(List.of("start", "status"),
                command.suggest(globalAdmin(), new String[]{"sta"}).stream().sorted().toList());
    }

    /**
     * La complétion ne doit pas révéler les serveurs que l'auteur n'administre
     * pas : ce serait annoncer ce qu'on lui cache par ailleurs.
     */
    @Test
    public void testCompletionOnlyOffersAdministeredServers() {
        List<String> suggestions = command.suggest(creatifAdmin(), new String[]{"start", ""});

        assertEquals(List.of("creatif"), suggestions);
    }

    @Test
    public void testCompletionIsSilentForOutsiders() {
        assertTrue(command.suggest(new FakeSource(), new String[]{""}).isEmpty());
    }
}
