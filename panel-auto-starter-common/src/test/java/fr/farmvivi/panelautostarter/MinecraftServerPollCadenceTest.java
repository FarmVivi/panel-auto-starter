package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelServerState;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie que la cadence de surveillance ne dépend pas du temps de réponse du
 * serveur sondé.
 * <p>
 * Elle en dépendait : la replanification vivait dans le rappel du ping. Or un
 * serveur en cours de démarrage accepte la connexion — le port est publié avant
 * que le jeu n'écoute — sans y répondre, et le proxy patiente alors son propre
 * délai de lecture, une trentaine de secondes. Un intervalle de démarrage réglé
 * à trois secondes en valait trente-trois, et la mise en ligne se constatait
 * avec près d'une minute de retard.
 */
public class MinecraftServerPollCadenceTest {

    private static final long PING_TIMEOUT = 3;
    private static final long IDLE_INTERVAL = 60;

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private CommonPlugin mockCommonPlugin;

    @Mock
    private PanelServer mockPanelServer;

    private MockCommonServer server;
    private MinecraftServer minecraftServer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getPlugin()).thenReturn(mockCommonPlugin);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());
        when(mockPanelServer.retrieveState()).thenReturn(PanelServerState.RUNNING);

        when(mockConfiguration.getLong(eq("server-start.ping-timeout"), anyLong()))
                .thenReturn(PING_TIMEOUT);
        when(mockConfiguration.getLong(eq("server-start.check-interval-idle"), anyLong()))
                .thenReturn(IDLE_INTERVAL);
        when(mockConfiguration.getLong(eq("server-start.check-interval-normal"), anyLong()))
                .thenReturn(15L);
        when(mockConfiguration.getLong(eq("server-start.check-interval-startup"), anyLong()))
                .thenReturn(3L);
        when(mockConfiguration.getLong(eq("server-start.idle-threshold"), anyLong()))
                .thenReturn(300L);

        // Le serveur ne repond jamais de lui-meme : le mock retient le rappel
        // sans l'appeler, ce qui reproduit exactement un ping resté en attente.
        server = new MockCommonServer("survie");
        minecraftServer = new MinecraftServer(mockPlugin, server, mockPanelServer,
                mock(ServerMotd.class));
    }

    /**
     * Retourne les tâches planifiées pour un délai donné, sans les exécuter.
     * <p>
     * Volontairement non destructif : oublier les planifications déjà observées
     * effacerait celles qu'un appel ultérieur cherche, et le nombre de tâches
     * armées est ici l'objet même de la vérification.
     *
     * @param delaySeconds le délai recherché
     * @return les tâches correspondantes, dans l'ordre de planification
     */
    private List<Runnable> scheduledFor(long delaySeconds) {
        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> delays = ArgumentCaptor.forClass(Long.class);
        verify(mockProxy, atLeast(0)).schedule(any(), tasks.capture(), delays.capture(),
                eq(TimeUnit.SECONDS));

        List<Runnable> due = new ArrayList<>();
        for (int i = 0; i < tasks.getAllValues().size(); i++) {
            if (delays.getAllValues().get(i) == delaySeconds) {
                due.add(tasks.getAllValues().get(i));
            }
        }
        return due;
    }

    /**
     * Le cœur du défaut : tant que le ping ne répondait pas, plus rien n'était
     * planifié et la surveillance restait muette.
     */
    @Test
    public void testAnUnansweredPingDoesNotStallTheCadence() {
        assertEquals(1, server.getPingCallCount(), "Le constructeur lance un premier sondage");
        assertEquals(1, scheduledFor(IDLE_INTERVAL).size(), "Un tour est arme des la construction");

        // Le sondage precedent n'a toujours pas repondu.
        scheduledFor(IDLE_INTERVAL).get(0).run();

        assertEquals(2, scheduledFor(IDLE_INTERVAL).size(),
                "Le tour suivant doit etre arme sans attendre la reponse du sondage en cours");
    }

    /**
     * Mais il ne faut pas non plus empiler les sondages sur un serveur muet.
     */
    @Test
    public void testNoSecondPingIsIssuedWhileOneIsStillPending() {
        scheduledFor(IDLE_INTERVAL).get(0).run();

        assertEquals(1, server.getPingCallCount(),
                "Un sondage en cours interdit d'en ouvrir un second");
    }

    /**
     * Une fois le sondage abandonné, le créneau est rendu et la surveillance
     * repart.
     */
    @Test
    public void testANewPingIsIssuedOnceThePendingOneIsAbandoned() {
        scheduledFor(PING_TIMEOUT).get(0).run();
        scheduledFor(IDLE_INTERVAL).get(0).run();

        assertEquals(2, server.getPingCallCount(),
                "Le creneau rendu, le tour suivant doit sonder a nouveau");
    }

    /**
     * Un abandon ne prouve pas que le serveur a disparu : le traiter comme une
     * disparition viderait la file d'attente d'un serveur simplement lent.
     */
    @Test
    public void testAnAbandonedPollDoesNotTakeTheServerOffline() {
        server.simulatePingResponse(new MockCommonServerPing());
        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus());

        // Un nouveau tour part, puis tous les abandons en attente se declenchent.
        scheduledFor(IDLE_INTERVAL).get(0).run();
        scheduledFor(PING_TIMEOUT).forEach(Runnable::run);

        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus(),
                "Un sondage abandonne ne dit rien a la machine a etats");
    }

    /**
     * Le serveur étant hors ligne et regardé par personne, la cadence lente
     * s'applique dès la construction.
     */
    @Test
    public void testTheIntervalIsArmedFromTheStartOfTheCycle() {
        verify(mockProxy).schedule(any(), any(), eq(IDLE_INTERVAL), eq(TimeUnit.SECONDS));
    }
}
