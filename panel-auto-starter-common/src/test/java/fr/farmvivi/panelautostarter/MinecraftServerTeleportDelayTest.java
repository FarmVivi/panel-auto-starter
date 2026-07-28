package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlugin;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Vérifie que le délai précédant la téléportation est celui qui est configuré,
 * et non celui de la boucle de surveillance.
 * <p>
 * Le déclenchement de la séquence vivait dans le rappel de surveillance : une
 * fois le serveur en ligne, celle-ci ne repasse qu'au bout de
 * {@code check-interval-normal}, quinze secondes par défaut. Un
 * {@code wait-before-teleport} de cinq secondes en devenait quinze, et les
 * joueurs regardaient un serveur annoncé prêt sans que rien ne se passe.
 */
public class MinecraftServerTeleportDelayTest {

    private static final long WAIT_BEFORE_TELEPORT_SECONDS = 5;

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

        // Un mock de Configuration rend 0 par defaut, et non la valeur de repli
        // passee en argument : sans ces amorces, le delai teste serait nul.
        when(mockConfiguration.getLong(eq("server-start.wait-before-teleport"), anyLong()))
                .thenReturn(WAIT_BEFORE_TELEPORT_SECONDS);
        when(mockConfiguration.getLong(eq("server-start.check-interval-normal"), anyLong()))
                .thenReturn(15L);

        server = new MockCommonServer("survie");
        minecraftServer = new MinecraftServer(mockPlugin, server, mockPanelServer,
                mock(ServerMotd.class));
    }

    /**
     * Amène le serveur en ligne en simulant un ping abouti.
     */
    private void bringOnline() {
        server.simulatePingResponse(mock(CommonServerPing.class));
    }

    @Test
    public void testTheCountdownIsScheduledOnItsOwnDeadline() {
        minecraftServer.enqueue(new MockCommonPlayer("Vivi"));

        bringOnline();

        assertEquals(MinecraftServerStatus.ONLINE, minecraftServer.getStatus(),
                "Le ping abouti doit faire passer le serveur en ligne");
        verify(mockProxy).schedule(any(), any(),
                eq(WAIT_BEFORE_TELEPORT_SECONDS * 1000), eq(TimeUnit.MILLISECONDS));
    }

    /**
     * Le cœur du défaut : sans échéance propre, la séquence n'était relancée
     * qu'au passage suivant de la surveillance.
     */
    @Test
    public void testTheCountdownDoesNotWaitForTheNextWatchdogPoll() {
        minecraftServer.enqueue(new MockCommonPlayer("Vivi"));

        bringOnline();

        // La surveillance se replanifie en secondes ; la sequence de
        // teleportation, elle, doit avoir sa propre echeance en millisecondes.
        verify(mockProxy, never()).schedule(any(), any(), eq(0L), eq(TimeUnit.MILLISECONDS));
        verify(mockProxy).schedule(any(), any(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    /**
     * Aucune échéance ne doit être posée quand personne n'attend.
     */
    @Test
    public void testNothingIsScheduledWithAnEmptyQueue() {
        bringOnline();

        verify(mockProxy, never()).schedule(any(), any(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    /**
     * La barre d'action doit rester affichée pendant ce délai : aucun titre ne
     * l'a encore remplacée, l'effacer laisserait le joueur devant un écran
     * vide.
     */
    @Test
    public void testTheActionBarSurvivesTheWait() {
        minecraftServer.enqueue(new MockCommonPlayer("Vivi"));

        bringOnline();

        assertFalse(minecraftServer.isShowingTeleportTitles(),
                "Aucun titre n'est encore affiche pendant le delai d'attente");
    }

    /**
     * La surveillance repasse pendant le délai : elle ne doit pas poser une
     * seconde échéance, sans quoi deux décomptes se chevaucheraient.
     */
    @Test
    public void testASecondPollDuringTheWaitDoesNotScheduleTwice() {
        minecraftServer.enqueue(new MockCommonPlayer("Vivi"));

        bringOnline();
        bringOnline();

        verify(mockProxy, times(1)).schedule(any(), any(), anyLong(), eq(TimeUnit.MILLISECONDS));
    }
}
