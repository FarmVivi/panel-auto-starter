package fr.farmvivi.panelautostarter.menu;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.access.WhitelistStore;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie les menus et surtout leur ligne de partage : le menu de sélection est
 * volontairement bête, le menu d'administration filtre.
 */
public class MenuTest {

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
    private MinecraftServer survieServer;
    private MinecraftServer creatifServer;
    private WhitelistStore whitelists;

    private static final class FakeSource implements CommonCommandSource {
        private final Set<String> permissions = new HashSet<>();

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
        }

        @Override
        public String getName() {
            return "Testeur";
        }

        @Override
        public CommonPlayer asPlayer() {
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());

        survie = new MockCommonServer("survie");
        survieServer = new MinecraftServer(mockPlugin, survie, surviePanel, mock(ServerMotd.class));
        creatifServer = new MinecraftServer(mockPlugin, new MockCommonServer("creatif"),
                creatifPanel, mock(ServerMotd.class));

        whitelists = new WhitelistStore(tempDir.toFile(), message -> {
        });
    }

    private List<MinecraftServer> servers() {
        return List.of(survieServer, creatifServer);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    // ===================== Menu de selection =====================

    /**
     * Le point que le menu ne doit surtout pas faire : filtrer. Le menu montre
     * tout, l'événement de connexion décide.
     */
    @Test
    public void testTheServerMenuShowsEveryServerRegardlessOfPermissions() {
        CommonPlayer sansDroit = new MockCommonPlayer("Quidam");

        Menu menu = ServerMenu.build(servers(), sansDroit, "server");

        assertEquals(2, menu.items().size(),
                "Le menu de selection ne filtre pas : c'est l'evenement qui refuse");
    }

    @Test
    public void testAServerEntryRunsTheProxyConnectCommand() {
        Menu menu = ServerMenu.build(servers(), new MockCommonPlayer("Vivi"), "server");

        assertEquals("server survie", menu.items().get(0).command());
        assertTrue(menu.items().get(0).isActionable());
    }

    @Test
    public void testTheConnectCommandIsConfigurable() {
        Menu menu = ServerMenu.build(servers(), new MockCommonPlayer("Vivi"), "hub goto");

        assertEquals("hub goto survie", menu.items().get(0).command());
    }

    /**
     * Proposer de rejoindre l'endroit où l'on est déjà n'a pas de sens.
     */
    @Test
    public void testTheServerYouAreOnIsNotClickable() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        player.setCurrentServer(survie);

        Menu menu = ServerMenu.build(servers(), player, "server");

        assertFalse(menu.items().get(0).isActionable(), "On ne rejoint pas la ou on est");
        assertTrue(plain(menu.items().get(0).label()).contains("survie"));
    }

    @Test
    public void testAnOnlineServerShowsItsPlayerCount() {
        survie.simulatePingResponse(new MockCommonServerPing());

        Menu menu = ServerMenu.build(servers(), new MockCommonPlayer("Vivi"), "server");

        assertTrue(menu.items().get(0).lore().stream()
                        .anyMatch(line -> plain(line).contains("connecté")),
                "Un serveur en ligne annonce sa frequentation");
    }

    @Test
    public void testAnEmptyNetworkStillProducesAMenu() {
        Menu menu = ServerMenu.build(List.of(), new MockCommonPlayer("Vivi"), "server");

        assertEquals(1, menu.items().size());
        assertFalse(menu.items().get(0).isActionable());
    }

    // ===================== Menu d'administration =====================

    /**
     * Celui-ci filtre, lui : il n'a rien à apprendre à son lecteur sur une
     * infrastructure qu'il ne peut pas toucher.
     */
    @Test
    public void testTheAdminMenuOnlyShowsAdministeredServers() {
        Menu menu = AdminMenu.build(servers(), whitelists,
                new FakeSource().grant("panelautostarter.admin.creatif"), "pas");

        String text = menu.items().stream().map(item -> plain(item.label()))
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(text.contains("creatif"), text);
        assertFalse(text.contains("survie"), text);
    }

    @Test
    public void testAGlobalAdminSeesEverything() {
        Menu menu = AdminMenu.build(servers(), whitelists,
                new FakeSource().grant("panelautostarter.admin"), "pas");

        String text = menu.items().stream().map(item -> plain(item.label()))
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(text.contains("creatif") && text.contains("survie"));
    }

    @Test
    public void testSomeoneWithoutRightsGetsAnEmptyAdminMenu() {
        Menu menu = AdminMenu.build(servers(), whitelists, new FakeSource(), "pas");

        assertEquals(1, menu.items().size());
        assertFalse(menu.items().get(0).isActionable());
    }

    /**
     * Proposer « démarrer » sur un serveur allumé n'aboutirait qu'à un refus.
     */
    /**
     * Le menu racine ne fait que lister : chaque entrée mène au menu du serveur.
     * Tout mettre à plat devenait illisible dès le deuxième serveur.
     */
    @Test
    public void testTheRootMenuListsServersAndLeadsToTheirOwnMenu() {
        Menu menu = AdminMenu.build(servers(), whitelists,
                new FakeSource().grant("panelautostarter.admin"), "pas");

        assertEquals(2, menu.items().size(), "Une entree par serveur, et rien de plus");
        assertTrue(commands(menu).contains("pas menu survie"));
        assertTrue(commands(menu).contains("pas menu creatif"));
    }

    @Test
    public void testOnlyTheRelevantActionIsOffered() {
        Menu offline = AdminMenu.buildForServer(survieServer, whitelists.get("survie"), "pas");
        assertTrue(commands(offline).contains("pas start survie"));
        assertFalse(commands(offline).contains("pas stop survie"));

        bringOnline(survie);
        Menu online = AdminMenu.buildForServer(survieServer, whitelists.get("survie"), "pas");
        assertTrue(commands(online).contains("pas stop survie"));
        assertFalse(commands(online).contains("pas start survie"));
    }

    @Test
    public void testTheWhitelistToggleReflectsItsCurrentState() {
        Menu off = AdminMenu.buildForServer(survieServer, whitelists.get("survie"), "pas");
        assertTrue(commands(off).contains("pas whitelist survie on"));

        whitelists.get("survie").setEnabled(true);
        Menu on = AdminMenu.buildForServer(survieServer, whitelists.get("survie"), "pas");
        assertTrue(commands(on).contains("pas whitelist survie off"));
    }

    /**
     * Sans retour, on ne peut plus atteindre les autres serveurs sans retaper
     * la commande.
     */
    @Test
    public void testTheServerMenuOffersAWayBack() {
        Menu menu = AdminMenu.buildForServer(survieServer, whitelists.get("survie"), "pas");

        assertTrue(commands(menu).contains("pas menu"),
                "Le menu d'un serveur doit ramener a la liste");
    }

    /**
     * Toute action de menu est une commande : elle emprunte donc le même chemin
     * que si elle avait été tapée, et subit les mêmes contrôles.
     */
    @Test
    public void testEveryAdminActionIsACommand() {
        Menu menu = AdminMenu.build(servers(), whitelists,
                new FakeSource().grant("panelautostarter.admin"), "pas");

        for (MenuItem item : menu.items()) {
            if (item.isActionable()) {
                assertTrue(item.command().startsWith("pas "),
                        "Une action doit etre une commande : " + item.command());
            }
        }
    }

    /**
     * Le panel doit confirmer la mise en ligne : un ping seul ne suffit plus.
     */
    private void bringOnline(MockCommonServer target) {
        for (MinecraftServer server : servers()) {
            if (server.getServer().equals(target)) {
                server.applyPanelState(fr.farmvivi.panelautostarter.panel.PanelServerState.RUNNING);
            }
        }
        target.simulatePingResponse(new MockCommonServerPing());
    }

    private static List<String> commands(Menu menu) {
        List<String> commands = new ArrayList<>();
        for (MenuItem item : menu.items()) {
            if (item.isActionable()) {
                commands.add(item.command());
            }
        }
        return commands;
    }

    // ===================== Rendu et choix du rendu =====================

    @Test
    public void testTheChatRendererProducesClickableLines() {
        MockCommonPlayer viewer = new MockCommonPlayer("Vivi");
        Menu menu = ServerMenu.build(servers(), viewer, "server");

        new ChatMenuRenderer().open(viewer, menu);

        Component page = viewer.getLastMessage();
        assertNotNull(page);
        assertTrue(carries(page, ClickEvent.runCommand("/server survie")),
                "Une entree doit porter un clic executant la commande");
    }

    /**
     * Compare l'événement entier plutôt que ses parties : Adventure 5 a remplacé
     * {@code value()} par {@code payload()} et transformé {@code Action} en
     * classe générique, si bien qu'inspecter le détail rendrait ce test
     * solidaire de la version de la bibliothèque.
     */
    private static boolean carries(Component component, ClickEvent<?> expected) {
        if (expected.equals(component.clickEvent())) {
            return true;
        }
        for (Component child : component.children()) {
            if (carries(child, expected)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testTheChatRendererIsAlwaysAvailable() {
        assertTrue(new ChatMenuRenderer().isAvailable(new MockCommonPlayer("Vivi")));
    }

    /**
     * Le repli est ce qui rend un rendu plus riche adoptable sans risque.
     */
    @Test
    public void testAnUnavailableRendererFallsBackToChat() {
        MenuService service = new MenuService(MenuSettings.defaults());
        service.register(unavailable("coffre"));
        service.register(new ChatMenuRenderer());

        assertEquals(ChatMenuRenderer.NAME,
                service.resolve(new MockCommonPlayer("Vivi")).getName());
    }

    @Test
    public void testTheRichestAvailableRendererWins() {
        MenuService service = new MenuService(MenuSettings.defaults());
        MenuRenderer coffre = available("coffre");
        service.register(coffre);
        service.register(new ChatMenuRenderer());

        assertSame(coffre, service.resolve(new MockCommonPlayer("Vivi")));
    }

    /**
     * Un rendu nommé explicitement est exigé, non suggéré.
     */
    @Test
    public void testAnExplicitRendererIsHonoured() {
        MenuService service = new MenuService(
                new MenuSettings(true, ChatMenuRenderer.NAME, "servers", "server",
                        MenuSettings.CompassSettings.defaults()));
        service.register(available("coffre"));
        service.register(new ChatMenuRenderer());

        assertEquals(ChatMenuRenderer.NAME,
                service.resolve(new MockCommonPlayer("Vivi")).getName());
    }

    @Test
    public void testAnExplicitButUnavailableRendererStillFallsBack() {
        MenuService service = new MenuService(
                new MenuSettings(true, "coffre", "servers", "server",
                        MenuSettings.CompassSettings.defaults()));
        service.register(unavailable("coffre"));
        service.register(new ChatMenuRenderer());

        assertEquals(ChatMenuRenderer.NAME,
                service.resolve(new MockCommonPlayer("Vivi")).getName(),
                "Mieux vaut un menu qui s'ouvre qu'un menu absent");
    }

    @Test
    public void testWithNoRendererNothingOpens() {
        MenuService service = new MenuService(MenuSettings.defaults());

        assertNull(service.resolve(new MockCommonPlayer("Vivi")));
        assertFalse(service.open(new MockCommonPlayer("Vivi"),
                new Menu(Component.empty(), List.of())));
    }

    private static MenuRenderer available(String name) {
        return renderer(name, true);
    }

    private static MenuRenderer unavailable(String name) {
        return renderer(name, false);
    }

    private static MenuRenderer renderer(String name, boolean availability) {
        return new MenuRenderer() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isAvailable(CommonPlayer viewer) {
                return availability;
            }

            @Override
            public void open(CommonPlayer viewer, Menu menu) {
            }
        };
    }

    // ===================== Reglages =====================

    @Test
    public void testAnEmptyCommandNameFallsBackToTheDefault() {
        Configuration config = new Configuration();
        config.set("menu.command", "  ");

        assertEquals("servers", MenuSettings.from(config).command(),
                "Un nom vide empecherait tout enregistrement");
    }

    @Test
    public void testSettingsAreReadFromTheConfiguration() {
        Configuration config = new Configuration();
        config.set("menu.renderer", "CHAT");
        config.set("menu.command", "serveurs");

        MenuSettings settings = MenuSettings.from(config);

        assertEquals("chat", settings.renderer(), "Le nom du rendu est insensible a la casse");
        assertEquals("serveurs", settings.command());
    }

    @Test
    public void testAbsentConfigurationFallsBackToDefaults() {
        assertEquals(MenuSettings.defaults(), MenuSettings.from(null));
    }
}
