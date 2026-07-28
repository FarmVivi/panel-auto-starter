package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.Messages;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelServerState;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests du compte à rebours et de la vidange de la file d'attente.
 * <p>
 * Le décompte et les téléportations sont menés par des tâches planifiées à la
 * seconde. Le test remplace le planificateur par une file qu'il déroule à la
 * main, ce qui permet de vérifier l'enchaînement sans attendre réellement.
 */
public class MinecraftServerCountdownTest {

    /** Une tâche planifiée, avec le délai demandé. */
    private record Scheduled(Runnable task, long delaySeconds) {
    }

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private PanelServer mockPanelServer;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private CommonPlugin mockCommonPlugin;

    private final Deque<Scheduled> scheduled = new ArrayDeque<>();
    private MockCommonServer testServer;
    private MinecraftServer minecraftServer;
    private Callback<CommonServerPing> schedulerCallback;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        // Sans ce stub, plugin.getPlugin() renvoie null et any(CommonPlugin.class)
        // ne matche pas les nulls : les taches planifiees ne seraient pas captees.
        when(mockPlugin.getPlugin()).thenReturn(mockCommonPlugin);
        when(mockPanelServer.retrieveState()).thenReturn(PanelServerState.RUNNING);

        // Aucun delai de courtoisie : la sequence doit demarrer des la mise en ligne.
        when(mockConfiguration.getLong(eq("server-start.wait-before-teleport"), anyLong())).thenReturn(0L);
        when(mockConfiguration.getLong(eq("server-start.teleport-delay"), anyLong())).thenReturn(1L);
        when(mockConfiguration.getLong(eq("server-start.check-interval-normal"), anyLong())).thenReturn(15L);
        when(mockConfiguration.getLong(eq("server-start.check-interval-startup"), anyLong())).thenReturn(3L);
        when(mockConfiguration.getLong(eq("server-start.check-interval-idle"), anyLong())).thenReturn(60L);
        when(mockConfiguration.getLong(eq("server-start.idle-threshold"), anyLong())).thenReturn(300L);
        when(mockConfiguration.getLong(eq("server-start.ping-cache-ttl"), anyLong())).thenReturn(3600L);

        givenCountdown(true, 3);

        doAnswer(invocation -> {
            scheduled.add(new Scheduled(invocation.getArgument(1), invocation.getArgument(2)));
            return null;
        }).when(mockProxy).schedule(any(CommonPlugin.class), any(Runnable.class), anyLong(), any(TimeUnit.class));

        testServer = new MockCommonServer("survie", "Survie");
        minecraftServer = new MinecraftServer(mockPlugin, testServer, mockPanelServer, mock(ServerMotd.class));
        schedulerCallback = testServer.getLastPingCallback();
    }

    private void givenCountdown(boolean enabled, int seconds) {
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.of(
                new MessageSettings.TitleSettings(true, Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO),
                new MessageSettings.CountdownSettings(enabled, seconds, "tick.sound", "go.sound")));
    }

    /** Amène le serveur en ligne, ce qui déclenche la séquence si la file n'est pas vide. */
    private void bringOnline() {
        schedulerCallback.done(new MockCommonServerPing());
        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus());
    }

    /** Déroule la prochaine tâche planifiée avec ce délai, en l'ôtant de la file. */
    private void runNext(long delaySeconds) {
        Scheduled next = scheduled.stream()
                .filter(s -> s.delaySeconds() == delaySeconds)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucune tache planifiee a " + delaySeconds + "s"));
        scheduled.remove(next);
        next.task().run();
    }

    private MockCommonPlayer queued(String name) {
        MockCommonPlayer player = new MockCommonPlayer(name);
        minecraftServer.getQueue().add(player);
        return player;
    }

    // ===================== Décompte =====================

    @Test
    public void testCountdownShowsEachSecondThenDeparture() {
        MockCommonPlayer player = queued("Vivi");

        bringOnline();
        assertEquals(Messages.countdownTitle(3), player.getLastTitle(), "Le decompte doit commencer a 3");

        runNext(1);
        assertEquals(Messages.countdownTitle(2), player.getLastTitle());

        runNext(1);
        assertEquals(Messages.countdownTitle(1), player.getLastTitle());

        runNext(1);
        assertEquals(Messages.readyTitle(), player.getLastTitle(),
                "Le dernier tic doit annoncer le depart");
    }

    @Test
    public void testCountdownPlaysTickThenGoSound() {
        MockCommonPlayer player = queued("Vivi");

        bringOnline();
        runNext(1);
        runNext(1);
        runNext(1);

        List<String> sounds = player.getPlayedSounds();
        assertEquals(List.of("tick.sound", "tick.sound", "tick.sound", "go.sound"), sounds,
                "Trois tics puis le son de depart");
    }

    @Test
    public void testNobodyIsTeleportedDuringTheCountdown() {
        MockCommonPlayer player = queued("Vivi");

        bringOnline();

        assertNull(player.getServer(), "La teleportation ne doit pas avoir lieu pendant le decompte");
        assertTrue(minecraftServer.getQueue().contains(player), "Le joueur reste en file");
    }

    @Test
    public void testPlayerIsTeleportedOnceTheCountdownEnds() {
        MockCommonPlayer player = queued("Vivi");

        bringOnline();
        runNext(1);
        runNext(1);
        runNext(1);

        assertTrue(minecraftServer.getQueue().isEmpty(), "La file doit etre videe");
    }

    @Test
    public void testDisabledCountdownTeleportsWithoutWaiting() {
        givenCountdown(false, 3);
        MockCommonPlayer player = queued("Vivi");

        bringOnline();

        assertTrue(minecraftServer.getQueue().isEmpty(),
                "Sans decompte, la teleportation doit avoir lieu immediatement");
        assertNull(player.getLastTitle(), "Aucun titre de decompte ne doit etre affiche");
    }

    @Test
    public void testZeroSecondsBehavesLikeDisabled() {
        givenCountdown(true, 0);
        queued("Vivi");

        bringOnline();

        assertTrue(minecraftServer.getQueue().isEmpty());
    }

    // ===================== Vidange de la file =====================

    /**
     * La file se vidait auparavant d'un joueur par sondage, soit un toutes les
     * 15 secondes, ce qui rendait {@code teleport-delay} sans effet. Elle est
     * desormais menee par sa propre tâche.
     */
    @Test
    public void testQueueDrainsOnePlayerPerDelay() {
        queued("Un");
        queued("Deux");
        queued("Trois");
        givenCountdown(false, 0);

        bringOnline();
        assertEquals(2, minecraftServer.getQueue().size(), "Un seul joueur part d'abord");

        runNext(1);
        assertEquals(1, minecraftServer.getQueue().size());

        runNext(1);
        assertEquals(0, minecraftServer.getQueue().size());
    }

    /**
     * La boucle de surveillance repasse regulierement : sans garde, chaque
     * sondage relancerait un decompte par-dessus le precedent.
     */
    @Test
    public void testSequenceIsNotRestartedWhileRunning() {
        MockCommonPlayer player = queued("Vivi");

        bringOnline();
        int soundsAfterFirstTick = player.getPlayedSounds().size();

        // Un nouveau sondage arrive pendant le decompte.
        testServer.getLastPingCallback().done(new MockCommonServerPing());

        assertEquals(soundsAfterFirstTick, player.getPlayedSounds().size(),
                "Un second sondage ne doit pas relancer un decompte concurrent");
    }

    /**
     * Le serveur retombe pendant le décompte : la file est vidée et chaque
     * joueur en est informé, plutôt que d'attendre une téléportation qui
     * n'arrivera pas.
     */
    @Test
    public void testQueueIsClearedAndPlayersToldIfServerDropsMidCountdown() {
        MockCommonPlayer player = queued("Vivi");
        bringOnline();

        schedulerCallback.done(null);

        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());
        assertTrue(minecraftServer.getQueue().isEmpty(), "La file doit etre videe");
        assertEquals(Messages.teleportFailed("Survie"), player.getLastMessage(),
                "Le joueur doit apprendre que le demarrage a echoue");
    }

    /**
     * Contrepartie du drapeau anti-relance : il doit être relâché quand le
     * serveur retombe, sans quoi plus aucun décompte n'aurait jamais lieu par
     * la suite — le plugin resterait muet à chaque démarrage ultérieur.
     */
    @Test
    public void testCountdownRunsAgainAfterAFailedStart() {
        queued("Vivi");
        bringOnline();
        schedulerCallback.done(null);
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());

        // Le serveur revient, un nouveau joueur patiente.
        MockCommonPlayer second = queued("Bob");
        runNext(60);
        testServer.getLastPingCallback().done(new MockCommonServerPing());

        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus());
        assertEquals(Messages.countdownTitle(3), second.getLastTitle(),
                "Un nouveau decompte doit demarrer apres un echec precedent");
    }
}
