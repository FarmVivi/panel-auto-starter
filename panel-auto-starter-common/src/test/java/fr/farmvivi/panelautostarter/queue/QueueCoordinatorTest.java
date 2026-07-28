package fr.farmvivi.panelautostarter.queue;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie les deux règles que le coordinateur est seul à pouvoir tenir : un
 * joueur n'attend qu'un serveur à la fois, et la barre d'action de chaque
 * joueur en attente est entretenue.
 */
public class QueueCoordinatorTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    private final List<MinecraftServer> servers = new ArrayList<>();
    private MessageSettings settings;
    private QueueCoordinator coordinator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());

        settings = MessageSettings.defaults();
        coordinator = new QueueCoordinator(() -> servers, () -> settings);
    }

    /**
     * Crée un serveur réel : les files ne sont pas simulées, ce sont celles que
     * le plugin utilise en production.
     */
    private MinecraftServer newServer(String name) {
        MinecraftServer server = new MinecraftServer(mockPlugin, new MockCommonServer(name),
                mock(PanelServer.class), mock(ServerMotd.class));
        servers.add(server);
        return server;
    }

    // ===================== Une seule file à la fois =====================

    @Test
    public void testJoiningANewQueueLeavesThePreviousOne() {
        MinecraftServer survie = newServer("survie");
        MinecraftServer creatif = newServer("creatif");
        CommonPlayer player = new MockCommonPlayer("Vivi");

        coordinator.join(survie, player);
        assertTrue(survie.isQueued(player), "Le joueur attend d'abord survie");

        coordinator.join(creatif, player);

        assertFalse(survie.isQueued(player),
                "Demander un nouveau serveur doit retirer le joueur de la file precedente");
        assertTrue(creatif.isQueued(player), "Le joueur attend desormais creatif");
    }

    /**
     * Le retrait doit porter sur l'identifiant du joueur, non sur l'instance :
     * rien ne garantit que le proxy rende toujours le même objet pour une même
     * connexion.
     */
    @Test
    public void testTheOldQueueIsLeftEvenWithADifferentInstance() {
        MinecraftServer survie = newServer("survie");
        MinecraftServer creatif = newServer("creatif");
        UUID uuid = UUID.randomUUID();

        coordinator.join(survie, new MockCommonPlayer("Vivi", uuid));
        coordinator.join(creatif, new MockCommonPlayer("Vivi", uuid));

        assertFalse(survie.isQueued(new MockCommonPlayer("Vivi", uuid)),
                "Une autre instance du meme joueur doit tout de meme le retirer");
        assertEquals(0, survie.getQueue().size());
        assertEquals(1, creatif.getQueue().size());
    }

    @Test
    public void testOtherPlayersKeepTheirPlace() {
        MinecraftServer survie = newServer("survie");
        MinecraftServer creatif = newServer("creatif");
        CommonPlayer resté = new MockCommonPlayer("Resté");
        CommonPlayer parti = new MockCommonPlayer("Parti");

        coordinator.join(survie, resté);
        coordinator.join(survie, parti);
        coordinator.join(creatif, parti);

        assertTrue(survie.isQueued(resté), "Le depart d'un autre ne doit deloger personne");
        assertEquals(1, survie.getQueue().size());
    }

    @Test
    public void testRejoiningTheSameQueueDoesNotDuplicate() {
        MinecraftServer survie = newServer("survie");
        CommonPlayer player = new MockCommonPlayer("Vivi");

        assertTrue(coordinator.join(survie, player), "La premiere demande inscrit le joueur");
        assertFalse(coordinator.join(survie, player), "La seconde ne doit rien ajouter");
        assertEquals(1, survie.getQueue().size());
    }

    @Test
    public void testLeaveAllEmptiesEveryQueue() {
        MinecraftServer survie = newServer("survie");
        MinecraftServer creatif = newServer("creatif");
        CommonPlayer player = new MockCommonPlayer("Vivi");

        // Une file incoherente ne peut pas etre construite via le coordinateur :
        // on force les deux inscriptions pour verifier que leaveAll nettoie
        // vraiment partout et non seulement la derniere.
        survie.enqueue(player);
        creatif.enqueue(player);

        coordinator.leaveAll(player);

        assertFalse(survie.isQueued(player));
        assertFalse(creatif.isQueued(player));
    }

    @Test
    public void testQueuedServerReportsWhatThePlayerIsWaitingFor() {
        MinecraftServer survie = newServer("survie");
        newServer("creatif");
        CommonPlayer player = new MockCommonPlayer("Vivi");

        assertNull(coordinator.queuedServer(player), "Un joueur qui n'attend rien n'a pas de serveur");

        coordinator.join(survie, player);

        assertSame(survie, coordinator.queuedServer(player));
    }

    /**
     * Deux demandes simultanées ne doivent pas laisser le joueur dans deux
     * files : chacune pourrait nettoyer la file de l'autre avant d'inscrire la
     * sienne, et les deux inscriptions survivraient.
     */
    @Test
    @Timeout(20)
    public void testConcurrentJoinsLeaveThePlayerInASingleQueue() throws Exception {
        MinecraftServer survie = newServer("survie");
        MinecraftServer creatif = newServer("creatif");
        UUID uuid = UUID.randomUUID();

        int rounds = 2000;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (MinecraftServer target : List.of(survie, creatif)) {
            pool.submit(() -> {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    coordinator.join(target, new MockCommonPlayer("Vivi", uuid));
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));

        int total = survie.getQueue().size() + creatif.getQueue().size();
        assertEquals(1, total,
                "Le joueur doit figurer dans exactement une file, jamais dans les deux");
    }

    // ===================== Barre d'action =====================

    /**
     * Serveur simulé pour les cas où l'état importe plus que la file elle-même.
     */
    private MinecraftServer stubServer(String name, MinecraftServerStatus status,
                                       boolean teleporting, List<CommonPlayer> queue) {
        MinecraftServer server = mock(MinecraftServer.class);
        when(server.getServer()).thenReturn(new MockCommonServer(name));
        when(server.getStatus()).thenReturn(status);
        when(server.isTeleportSequenceRunning()).thenReturn(teleporting);
        when(server.getQueue()).thenReturn(queue);
        servers.add(server);
        return server;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    public void testEachWaitingPlayerGetsHisOwnPosition() {
        MockCommonPlayer premier = new MockCommonPlayer("Premier");
        MockCommonPlayer second = new MockCommonPlayer("Second");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(premier, second));

        coordinator.tickActionBar();

        assertEquals("1/2", plain(premier.getLastActionBar()).replaceAll(".*·\\s*", ""),
                "Le premier de la file doit lire sa propre place");
        assertEquals("2/2", plain(second.getLastActionBar()).replaceAll(".*·\\s*", ""),
                "Le second aussi, et ce n'est pas la meme");
    }

    @Test
    public void testTheAwaitedServerAndItsStateAreShown() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        stubServer("Survie", MinecraftServerStatus.STARTING, false, List.of(player));

        coordinator.tickActionBar();

        String line = plain(player.getLastActionBar());
        assertTrue(line.contains("Survie"), "La barre doit nommer le serveur attendu : " + line);
        assertTrue(line.contains("démarrage"), "Et dire ou il en est : " + line);
    }

    @Test
    public void testAReadyServerIsAnnouncedAsSuch() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        stubServer("survie", MinecraftServerStatus.ONLINE, false, List.of(player));

        coordinator.tickActionBar();

        assertTrue(plain(player.getLastActionBar()).contains("prêt"),
                "Un serveur en ligne ne doit plus etre annonce comme demarrant");
    }

    /**
     * Le titre du décompte occupe déjà l'écran et dit la même chose en plus
     * grand.
     */
    @Test
    public void testNothingIsSentDuringTheCountdown() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        stubServer("survie", MinecraftServerStatus.ONLINE, true, List.of(player));

        coordinator.tickActionBar();

        assertTrue(player.getActionBars().isEmpty(),
                "La barre d'action doit s'effacer pendant le decompte");
    }

    @Test
    public void testDisablingTheActionBarSendsNothing() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(player));
        settings = MessageSettings.of(MessageSettings.defaults().getTitle(),
                MessageSettings.defaults().getCountdown(),
                MessageSettings.defaults().getKickFeedback(),
                new MessageSettings.ActionBarSettings(false, Duration.ofSeconds(1), true));

        coordinator.tickActionBar();

        assertTrue(player.getActionBars().isEmpty(),
                "Desactivee, la barre d'action ne doit rien envoyer");
    }

    @Test
    public void testPositionCanBeHidden() {
        MockCommonPlayer premier = new MockCommonPlayer("Premier");
        MockCommonPlayer second = new MockCommonPlayer("Second");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(premier, second));
        settings = MessageSettings.of(MessageSettings.defaults().getTitle(),
                MessageSettings.defaults().getCountdown(),
                MessageSettings.defaults().getKickFeedback(),
                new MessageSettings.ActionBarSettings(true, Duration.ofSeconds(1), false));

        coordinator.tickActionBar();

        assertFalse(plain(premier.getLastActionBar()).contains("1/2"),
                "La place ne doit pas apparaitre quand elle est masquee");
    }

    /**
     * Annoncer « 1/1 » à quelqu'un qui attend seul n'apprend rien.
     */
    @Test
    public void testALonePlayerIsNotToldHeIsFirstOfOne() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(player));

        coordinator.tickActionBar();

        assertFalse(plain(player.getLastActionBar()).contains("1/1"),
                "Un joueur seul n'a pas de place a apprendre");
    }

    @Test
    public void testAnEmptyQueueCostsNothing() {
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of());

        assertDoesNotThrow(() -> coordinator.tickActionBar());
    }

    /**
     * Un joueur déjà parti ne doit pas être servi : sur BungeeCord, écrire à une
     * connexion fermée lève une exception qui interromprait le balayage, et
     * priverait de barre d'action tous les joueurs suivants.
     */
    @Test
    public void testAnOfflinePlayerIsSkipped() {
        MockCommonPlayer parti = new MockCommonPlayer("Parti");
        parti.setOnline(false);
        MockCommonPlayer présent = new MockCommonPlayer("Présent");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(parti, présent));

        coordinator.tickActionBar();

        assertTrue(parti.getActionBars().isEmpty(), "Rien ne doit etre envoye a un joueur parti");
        assertFalse(présent.getActionBars().isEmpty(), "Les suivants doivent tout de meme etre servis");
    }

    /**
     * L'indicateur d'activité doit réellement changer d'une émission à l'autre,
     * sans quoi il ne signale plus rien.
     */
    @Test
    public void testTheActivityIndicatorAdvancesBetweenTicks() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(player));

        coordinator.tickActionBar();
        coordinator.tickActionBar();

        assertEquals(2, player.getActionBars().size(), "Chaque passage reemet la barre");
        assertNotEquals(plain(player.getActionBars().get(0)), plain(player.getActionBars().get(1)),
                "L'indicateur doit avoir tourne entre les deux");
    }

    /**
     * Les files de tous les serveurs sont parcourues, pas seulement la première.
     */
    @Test
    public void testEveryServerQueueIsServed() {
        MockCommonPlayer surSurvie = new MockCommonPlayer("A");
        MockCommonPlayer surCreatif = new MockCommonPlayer("B");
        stubServer("survie", MinecraftServerStatus.STARTING, false, List.of(surSurvie));
        stubServer("creatif", MinecraftServerStatus.STARTING, false, List.of(surCreatif));

        coordinator.tickActionBar();

        assertTrue(plain(surSurvie.getLastActionBar()).contains("survie"));
        assertTrue(plain(surCreatif.getLastActionBar()).contains("creatif"));
    }

    @Test
    public void testNullArgumentsAreIgnored() {
        MinecraftServer survie = newServer("survie");

        assertFalse(coordinator.join(survie, null));
        assertFalse(coordinator.join(null, new MockCommonPlayer("Vivi")));
        assertDoesNotThrow(() -> coordinator.leaveAll(null));
        assertNull(coordinator.queuedServer(null));
    }

    /**
     * Le coordinateur lit la liste des serveurs à chaque appel : le plugin la
     * remplit après l'avoir construit.
     */
    @Test
    public void testServersRegisteredLaterAreTakenIntoAccount() {
        Collection<MinecraftServer> vide = servers;
        assertTrue(vide.isEmpty());

        MinecraftServer tardif = newServer("tardif");
        CommonPlayer player = new MockCommonPlayer("Vivi");
        coordinator.join(tardif, player);

        assertSame(tardif, coordinator.queuedServer(player));
    }
}
