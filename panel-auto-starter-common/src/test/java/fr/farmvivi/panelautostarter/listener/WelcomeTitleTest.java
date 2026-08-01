package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectedEvent;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie le titre d'accueil sur le serveur d'attente.
 * <p>
 * Il ne doit apparaître que lorsque le joueur n'a rien de plus important à
 * lire : ni une file en cours, ni l'explication d'un serveur qui vient de
 * s'arrêter sous ses pieds.
 */
public class WelcomeTitleTest {

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
    private PendingNotifications pending;
    private ServerConnectedEventListener listener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        // L'accueil est eteint par defaut : ces tests portent sur ce qu'il
        // affiche une fois demande.
        when(mockPlugin.getMessageSettings()).thenReturn(welcomeEnabled(config -> {
        }));
        when(mockConfiguration.getString("queue.server")).thenReturn("lobby");

        limbo = new MockCommonServer("lobby");
        survie = new MockCommonServer("survie");
        survieServer = new MinecraftServer(mockPlugin, survie, mockPanelServer,
                mock(ServerMotd.class));

        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(survie, survieServer);
        doReturn(servers).when(mockPlugin).getServers();
        when(mockPlugin.getQueueCoordinator()).thenReturn(new QueueCoordinator(mockPlugin));

        pending = new PendingNotifications();
        listener = new ServerConnectedEventListener(pending, mockPlugin);
    }

    /**
     * Réglages avec l'accueil activé, plus ce que le test veut préciser.
     */
    private static MessageSettings welcomeEnabled(java.util.function.Consumer<Configuration> tweak) {
        Configuration config = new Configuration();
        config.set("queue.welcome-title.enabled", true);
        tweak.accept(config);
        return MessageSettings.from(config);
    }

    private void arriveOn(MockCommonPlayer player, CommonServer server) {
        listener.onServerConnected(new ServerConnectedEvent(player, server));
    }

    private static String plain(Component component) {
        return component == null ? ""
                : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    public void testAPlayerArrivingOnTheWaitingServerIsWelcomed() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");

        arriveOn(player, limbo);

        assertEquals("Bienvenue", plain(player.getLastTitle()));
    }

    /**
     * Un joueur déposé sur le serveur d'attente le temps d'un démarrage voit
     * déjà le titre de mise en file : l'accueillir par-dessus l'effacerait.
     */
    @Test
    public void testAQueuedPlayerIsNotWelcomed() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        survieServer.enqueue(player);

        arriveOn(player, limbo);

        assertNull(player.getLastTitle(), "Le titre de mise en file ne doit pas etre ecrase");
    }

    /**
     * Le joueur ramené d'un serveur qui vient de s'arrêter doit lire pourquoi,
     * pas un « bienvenue ».
     */
    @Test
    public void testAnExplanationTakesPrecedenceOverTheWelcome() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        pending.queueTitle(player, Component.text("Serveur arrêté"), Component.empty(),
                Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO);

        arriveOn(player, limbo);

        assertEquals("Serveur arrêté", plain(player.getLastTitle()),
                "L'explication doit rester la derniere chose affichee");
    }

    @Test
    public void testArrivingElsewhereBringsNoWelcome() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");

        arriveOn(player, survie);

        assertNull(player.getLastTitle());
    }

    @Test
    public void testTheWelcomeCanBeTurnedOff() {
        Configuration config = new Configuration();
        config.set("queue.welcome-title.enabled", false);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.from(config));
        MockCommonPlayer player = new MockCommonPlayer("Vivi");

        arriveOn(player, limbo);

        assertNull(player.getLastTitle());
    }

    /**
     * La délivrance des messages différés ne doit pas dépendre de l'accueil :
     * c'est elle qui porte les explications.
     */
    @Test
    public void testPendingNotificationsAreStillDelivered() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        pending.queueMessage(player, Component.text("Message differe"));

        arriveOn(player, limbo);

        assertEquals("Message differe", plain(player.getLastMessage()));
    }

    // ===================== Reglages =====================

    @Test
    public void testTheTextIsConfigurableWithColourCodes() {
        when(mockPlugin.getMessageSettings()).thenReturn(welcomeEnabled(config -> {
            config.set("queue.welcome-title.title", "&aSalut");
            config.set("queue.welcome-title.subtitle", "&7Amuse-toi bien");
        }));
        MockCommonPlayer player = new MockCommonPlayer("Vivi");

        arriveOn(player, limbo);

        assertEquals("Salut", plain(player.getLastTitle()));
        assertEquals("Amuse-toi bien", plain(player.getLastSubtitle()));
    }

    /**
     * Une chaîne vide est respectée : c'est la façon d'afficher un titre sans
     * sous-titre.
     */
    @Test
    public void testAnEmptySubtitleIsHonoured() {
        when(mockPlugin.getMessageSettings()).thenReturn(welcomeEnabled(config ->
                config.set("queue.welcome-title.subtitle", "")));
        MockCommonPlayer player = new MockCommonPlayer("Vivi");

        arriveOn(player, limbo);

        assertEquals("", plain(player.getLastSubtitle()));
        assertEquals("Bienvenue", plain(player.getLastTitle()), "Le titre garde son defaut");
    }

    /**
     * Le texte proposé est celui du plugin, pas celui de l'administrateur :
     * l'afficher d'emblée ferait surgir un « Bienvenue » chez tout le monde à la
     * première mise à jour.
     */
    @Test
    public void testTheWelcomeIsOffUntilAskedFor() {
        MessageSettings.WelcomeSettings welcome = MessageSettings.defaults().getWelcome();

        assertFalse(welcome.enabled(), "Rien ne doit apparaitre sans que ce soit demande");
        assertEquals("Bienvenue", plain(welcome.title()), "Le texte est pret pour qui l'active");
        assertEquals(Duration.ofMillis(3000), welcome.stay());
    }

    @Test
    public void testNothingIsShownWhileItIsOff() {
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());
        MockCommonPlayer player = new MockCommonPlayer("Vivi");

        arriveOn(player, limbo);

        assertNull(player.getLastTitle());
    }
}
