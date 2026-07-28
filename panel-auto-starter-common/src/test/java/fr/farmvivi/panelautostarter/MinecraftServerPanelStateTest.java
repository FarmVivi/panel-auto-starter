package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.message.MessageSettings;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie que l'état rapporté par le panel sert à constater la disparition d'un
 * serveur.
 * <p>
 * Le ping seul mettait trop longtemps : un serveur éteint dont le port reste
 * publié ne répond pas, il fait expirer la connexion, et plusieurs sondages
 * s'écoulaient avant que le plugin ne s'en aperçoive. Pendant ce temps les
 * joueurs n'étaient ni mis en file ni suivis d'un redémarrage — ils se
 * heurtaient à une connexion qui expire, puis étaient rattrapés sur le serveur
 * d'attente avec un « serveur arrêté » qui arrivait bien tard.
 */
public class MinecraftServerPanelStateTest {

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
        when(mockConfiguration.getBoolean(eq("server-start.use-panel-state"), anyBoolean()))
                .thenReturn(true);

        server = new MockCommonServer("survie");
        minecraftServer = new MinecraftServer(mockPlugin, server, mockPanelServer,
                mock(ServerMotd.class));
    }

    private void bringOnline() {
        server.simulatePingResponse(new MockCommonServerPing());
        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus());
    }

    @Test
    public void testAStoppedServerIsNoticedFromThePanel() {
        bringOnline();

        minecraftServer.applyPanelState(PanelServerState.OFFLINE);

        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(),
                "Le panel sait que le serveur est arrete : inutile d'attendre l'expiration d'un ping");
    }

    /**
     * Un serveur en cours d'arrêt n'acceptera plus personne : le traiter comme
     * arrêté évite d'y envoyer un joueur pour rien.
     */
    @Test
    public void testAStoppingServerCountsAsDown() {
        bringOnline();

        minecraftServer.applyPanelState(PanelServerState.STOPPING);

        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus());
    }

    @Test
    public void testTheQueueIsEmptiedAndPlayersTold() {
        bringOnline();
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        minecraftServer.enqueue(player);

        minecraftServer.applyPanelState(PanelServerState.OFFLINE);

        assertTrue(minecraftServer.getQueue().isEmpty(), "La file n'a plus d'objet");
        assertNotNull(player.getLastMessage(), "Le joueur doit etre prevenu");
    }

    /**
     * Le serveur redémarré doit pouvoir repasser en ligne : la transition se
     * détecte sur « aucun ping connu, puis un ping », mémoire qu'il faut donc
     * remettre à zéro.
     */
    @Test
    public void testTheServerCanComeBackOnlineAfterwards() {
        bringOnline();
        minecraftServer.applyPanelState(PanelServerState.OFFLINE);

        server.simulatePingResponse(new MockCommonServerPing());

        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus(),
                "Un serveur relance doit pouvoir etre reconnu en ligne");
    }

    /**
     * Le point délicat : pendant un démarrage, le panel rapporte brièvement le
     * serveur éteint avant que le conteneur ne parte. L'écouter annulerait le
     * démarrage que l'on vient de demander.
     */
    @Test
    public void testAStartingServerIsNotCancelledByAnOfflinePanelState() {
        minecraftServer.start();
        assertEquals(MinecraftServerStatus.STARTING, minecraftServer.getStatus());

        minecraftServer.applyPanelState(PanelServerState.OFFLINE);

        assertEquals(MinecraftServerStatus.STARTING, minecraftServer.getStatus(),
                "Un demarrage en cours ne doit pas etre annule par le panel");
    }

    /**
     * L'inverse n'est pas vrai : un panel qui annonce le serveur démarré ne
     * garantit pas que le jeu accepte les connexions.
     */
    @Test
    public void testARunningPanelStateDoesNotBringTheServerOnline() {
        minecraftServer.applyPanelState(PanelServerState.RUNNING);

        assertEquals(MinecraftServerStatus.OFFLINE, minecraftServer.getStatus(),
                "Le passage en ligne reste decide par le ping seul");
    }

    @Test
    public void testAnUnknownStateChangesNothing() {
        bringOnline();

        minecraftServer.applyPanelState(PanelServerState.UNKNOWN);
        minecraftServer.applyPanelState(null);

        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus());
    }

    /**
     * L'appel au panel est bloquant : il doit sortir du fil du proxy, faute de
     * quoi il immobilise la boucle d'événements réseau le temps d'un
     * aller-retour HTTP.
     */
    @Test
    public void testThePanelIsQueriedOffTheProxyThread() {
        verify(mockProxy, atLeastOnce()).runAsync(any(), any());
        verify(mockPanelServer, never()).retrieveState();
    }

    @Test
    public void testThePanelIsLeftAloneWhenDisabled() {
        clearInvocations(mockProxy);
        when(mockConfiguration.getBoolean(eq("server-start.use-panel-state"), anyBoolean()))
                .thenReturn(false);

        new MinecraftServer(mockPlugin, new MockCommonServer("autre"), mockPanelServer,
                mock(ServerMotd.class));

        verify(mockProxy, never()).runAsync(any(), any());
    }
}
