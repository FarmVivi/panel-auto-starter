package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.common.Callback;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelServerState;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests du cache de ping servi au MOTD et du rafraîchissement à la demande.
 * <p>
 * Deux garanties sont vérifiées ici, parce qu'elles ne sont pas évidentes à la
 * lecture et qu'une régression y serait silencieuse :
 * <ul>
 *   <li>{@code peekServerPing()} ne bloque jamais et ne déclenche qu'un seul
 *       rafraîchissement même sous rafales de pings ;</li>
 *   <li>le rafraîchissement à la demande n'interfère pas avec la machine à
 *       états, qui reste pilotée par le seul scheduler.</li>
 * </ul>
 */
public class MinecraftServerPingCacheTest {

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

    private MockCommonServer testServer;
    private MinecraftServer minecraftServer;

    /** Callback du ping declenche par le constructeur (celui du scheduler). */
    private Callback<CommonServerPing> schedulerCallback;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());

        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPanelServer.retrieveState()).thenReturn(PanelServerState.RUNNING);

        givenCacheTtlSeconds(0);
        when(mockConfiguration.getLong(eq("server-start.check-interval-normal"), anyLong())).thenReturn(15L);
        when(mockConfiguration.getLong(eq("server-start.check-interval-startup"), anyLong())).thenReturn(3L);
        when(mockConfiguration.getLong(eq("server-start.check-interval-idle"), anyLong())).thenReturn(60L);
        when(mockConfiguration.getLong(eq("server-start.idle-threshold"), anyLong())).thenReturn(300L);

        testServer = new MockCommonServer("test-server", "Test Server");
        minecraftServer = new MinecraftServer(mockPlugin, testServer, mockPanelServer, mock(ServerMotd.class));

        // Le constructeur lance immediatement une premiere verification.
        schedulerCallback = testServer.getLastPingCallback();
    }

    private void givenCacheTtlSeconds(long seconds) {
        when(mockConfiguration.getLong(eq("server-start.ping-cache-ttl"), anyLong())).thenReturn(seconds);
    }

    // ===================== Cache non bloquant =====================

    @Test
    public void testPeekReturnsNullBeforeAnyPingCompletes() {
        assertNull(minecraftServer.peekServerPing(),
                "Sans ping abouti, le cache doit rendre null plutot que d'attendre");
    }

    @Test
    public void testPeekReturnsImmediatelyWhileRefreshIsStillInFlight() {
        CommonServerPing known = new MockCommonServerPing();
        schedulerCallback.done(known);

        // Ce peek declenche un rafraichissement dont le callback n'aboutira jamais.
        CommonServerPing served = minecraftServer.peekServerPing();

        assertSame(known, served,
                "Le cache doit etre rendu immediatement, sans attendre le rafraichissement");
    }

    @Test
    public void testPeekServesCachedValueWithoutRefreshWhenFresh() {
        givenCacheTtlSeconds(3600);
        schedulerCallback.done(new MockCommonServerPing());
        int pingsBefore = testServer.getPingCallCount();

        minecraftServer.peekServerPing();
        minecraftServer.peekServerPing();

        assertEquals(pingsBefore, testServer.getPingCallCount(),
                "Un cache frais ne doit declencher aucune requete vers le serveur");
    }

    @Test
    public void testPeekTriggersRefreshWhenCacheIsStale() {
        schedulerCallback.done(new MockCommonServerPing());
        int pingsBefore = testServer.getPingCallCount();

        minecraftServer.peekServerPing();

        assertEquals(pingsBefore + 1, testServer.getPingCallCount(),
                "Un cache perime doit declencher un rafraichissement");
    }

    @Test
    public void testRefreshUpdatesCacheOnceCallbackCompletes() {
        schedulerCallback.done(new MockCommonServerPing());

        minecraftServer.peekServerPing();
        CommonServerPing fresher = new MockCommonServerPing();
        testServer.getLastPingCallback().done(fresher);

        assertSame(fresher, minecraftServer.getServerPing(),
                "Le cache doit refleter le resultat du rafraichissement");
    }

    // ===================== Garde anti-stampede =====================

    @Test
    public void testBurstOfPingsTriggersOnlyOneRefresh() {
        schedulerCallback.done(new MockCommonServerPing());
        int pingsBefore = testServer.getPingCallCount();

        // 50 clients pingent avant que le rafraichissement n'aboutisse.
        for (int i = 0; i < 50; i++) {
            minecraftServer.peekServerPing();
        }

        assertEquals(pingsBefore + 1, testServer.getPingCallCount(),
                "Une rafale de pings ne doit produire qu'une seule requete backend");
    }

    @Test
    public void testRefreshIsAllowedAgainAfterCallbackCompletes() {
        schedulerCallback.done(new MockCommonServerPing());

        minecraftServer.peekServerPing();
        int afterFirst = testServer.getPingCallCount();
        testServer.getLastPingCallback().done(new MockCommonServerPing());

        minecraftServer.peekServerPing();

        assertEquals(afterFirst + 1, testServer.getPingCallCount(),
                "Le drapeau doit etre relache apres le callback, sinon plus aucun "
                        + "rafraichissement n'aurait jamais lieu");
    }

    @Test
    public void testFlagIsReleasedWhenPingFailsSynchronously() {
        schedulerCallback.done(new MockCommonServerPing());

        MockCommonServer failing = new FailingOnceServer("test-server");
        MinecraftServer server = new MinecraftServer(mockPlugin, failing, mockPanelServer, mock(ServerMotd.class));
        ((FailingOnceServer) failing).armFailure();

        assertThrows(IllegalStateException.class, server::peekServerPing);

        // Sans le rattrapage, le drapeau resterait leve et bloquerait a jamais.
        int pingsBefore = failing.getPingCallCount();
        server.peekServerPing();
        assertEquals(pingsBefore + 1, failing.getPingCallCount(),
                "Un echec synchrone ne doit pas bloquer definitivement les rafraichissements");
    }

    // ===================== Etancheite avec la machine a etats =====================

    @Test
    public void testRefreshDoesNotChangeStatus() {
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());

        minecraftServer.peekServerPing();
        testServer.getLastPingCallback().done(new MockCommonServerPing());

        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(),
                "Le rafraichissement a la demande ne doit pas piloter le statut");
    }

    /**
     * Garantie centrale du design : le cache de présentation et la mémoire de la
     * machine à états sont deux champs distincts. S'ils étaient confondus, un
     * rafraîchissement à la demande arrivé avant le scheduler ferait croire à ce
     * dernier qu'il a déjà vu un ping, et la transition hors-ligne → en ligne
     * serait manquée — le serveur resterait éternellement marqué OFFLINE.
     */
    @Test
    public void testRefreshDoesNotMaskOfflineToOnlineTransition() {
        minecraftServer.peekServerPing();
        testServer.getLastPingCallback().done(new MockCommonServerPing());
        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());

        // Le scheduler voit son tout premier ping abouti : la transition doit avoir lieu.
        schedulerCallback.done(new MockCommonServerPing());

        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus(),
                "La transition doit etre detectee malgre le rafraichissement anterieur");
    }

    // ===================== Surveillance adaptative =====================

    @Test
    public void testUnwatchedOfflineServerIsPolledSlowly() {
        schedulerCallback.done(null);

        verify(mockProxy).schedule(any(), any(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testWatchedOfflineServerKeepsNormalInterval() {
        minecraftServer.notifyWatched();

        schedulerCallback.done(null);
        runNextSurveillanceCycle();

        verify(mockProxy).schedule(any(), any(), eq(15L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testOnlineServerKeepsNormalInterval() {
        schedulerCallback.done(new MockCommonServerPing());
        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus());

        runNextSurveillanceCycle();

        verify(mockProxy).schedule(any(), any(), eq(15L), eq(TimeUnit.SECONDS));
    }

    /**
     * Rejoue un tour de surveillance.
     * <p>
     * La cadence est armée au début du cycle et non à la réception de la
     * réponse : elle ne dépend plus du temps que met le serveur sondé à
     * répondre, un serveur qui démarre acceptant la connexion sans y répondre.
     * L'intervalle choisi reflète donc le statut courant, ce qui suppose de
     * déclencher un nouveau cycle pour l'observer.
     */
    private void runNextSurveillanceCycle() {
        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(mockProxy, atLeastOnce()).schedule(any(), tasks.capture(), anyLong(), any());
        clearInvocations(mockProxy);
        for (Runnable task : tasks.getAllValues()) {
            task.run();
        }
    }

    /**
     * Serveur qui echoue une fois de maniere synchrone, pour verifier que le
     * drapeau de rafraichissement est bien relache dans ce cas.
     */
    private static class FailingOnceServer extends MockCommonServer {
        private boolean armed = false;

        FailingOnceServer(String name) {
            super(name);
        }

        void armFailure() {
            this.armed = true;
        }

        @Override
        public void ping(Callback<CommonServerPing> callback) {
            if (armed) {
                armed = false;
                throw new IllegalStateException("echec reseau simule");
            }
            super.ping(callback);
        }
    }
}
