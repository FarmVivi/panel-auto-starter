package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.access.AccessSettings;
import fr.farmvivi.panelautostarter.access.ServerAccess;
import fr.farmvivi.panelautostarter.access.Whitelist;
import fr.farmvivi.panelautostarter.access.WhitelistStore;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ServerConnectEvent;
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
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie que le contrôle d'accès s'applique à l'événement de connexion.
 * <p>
 * C'est l'endroit qui compte : le menu de sélection se contente de demander un
 * serveur, comme le ferait un {@code /server}. Filtrer au menu laisserait
 * passer tous les autres chemins — commande, hôte forcé, redirection d'un autre
 * plugin — et donnerait l'illusion d'une protection.
 */
public class ServerConnectAccessTest {

    @TempDir
    Path tempDir;

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private ServerConnectEvent mockEvent;

    @Mock
    private PanelServer mockPanelServer;

    private CommonServer limboServer;
    private CommonServer targetServer;
    private MinecraftServer managed;
    private WhitelistStore whitelists;
    private PendingNotifications pendingNotifications;
    private ServerConnectEventListener listener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockConfiguration.getString("queue.server")).thenReturn("limbo");

        limboServer = new MockCommonServer("limbo");
        targetServer = new MockCommonServer("survie");
        when(mockProxy.getServer("limbo")).thenReturn(limboServer);

        pendingNotifications = new PendingNotifications();
        when(mockPlugin.getPendingNotifications()).thenReturn(pendingNotifications);

        managed = new MinecraftServer(mockPlugin, targetServer, mockPanelServer,
                mock(ServerMotd.class));
        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(targetServer, managed);
        doReturn(servers).when(mockPlugin).getServers();

        when(mockPlugin.getQueueCoordinator()).thenReturn(new QueueCoordinator(mockPlugin));

        whitelists = new WhitelistStore(tempDir.toFile(), message -> {
        });
        // Permission exigee : c'est la configuration qui refuse quelque chose.
        when(mockPlugin.getServerAccess()).thenReturn(new ServerAccess(
                new AccessSettings(true, true, true, ServerAccess.DEFAULT_PERMISSION_FORMAT),
                whitelists));

        listener = new ServerConnectEventListener(mockPlugin);
    }

    private void fire(MockCommonPlayer player) {
        when(mockEvent.getTarget()).thenReturn(targetServer);
        when(mockEvent.getPlayer()).thenReturn(player);
        listener.onServerConnect(mockEvent);
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Le plus important : refuser avant de démarrer. Allumer une machine pour
     * quelqu'un qui n'y entrera pas serait absurde, et le laisser patienter
     * dans une file dont il sera refoulé à l'arrivée, malhonnête.
     */
    @Test
    public void testARefusedPlayerNeitherStartsTheServerNorJoinsTheQueue() {
        MockCommonPlayer player = new MockCommonPlayer("Intrus");
        player.setCurrentServer(limboServer);

        fire(player);

        assertEquals(fr.farmvivi.panelautostarter.MinecraftServerStatus.OFFLINE, managed.getStatus(),
                "Le serveur ne doit pas avoir ete demarre");
        verify(mockPanelServer, never()).start();
        assertTrue(managed.getQueue().isEmpty(), "Le joueur ne doit pas etre mis en file");
    }

    @Test
    public void testAnAlreadyConnectedPlayerIsHeldWhereHeIs() {
        MockCommonPlayer player = new MockCommonPlayer("Intrus");
        player.setCurrentServer(limboServer);

        fire(player);

        verify(mockEvent).setCancelled(true);
        assertNotNull(player.getLastMessage(), "Il doit savoir pourquoi");
        assertTrue(plain(player.getLastMessage()).contains("permission"),
                "Le refus doit nommer la permission : " + plain(player.getLastMessage()));
    }

    /**
     * Annuler la connexion de quelqu'un qui arrive sur le proxy le
     * déconnecterait — ce qui arriverait à quiconque se connecte par l'hôte
     * forcé d'un serveur qui lui est interdit.
     */
    @Test
    public void testAnArrivingPlayerIsSentToTheWaitingServerRatherThanDropped() {
        MockCommonPlayer player = new MockCommonPlayer("Intrus");

        fire(player);

        verify(mockEvent).setTarget(limboServer);
        verify(mockEvent, never()).setCancelled(true);
    }

    /**
     * Le message serait perdu s'il partait avant que la connexion n'atteigne
     * l'état de jeu.
     */
    @Test
    public void testTheRefusalIsDeferredUntilTheArrivingPlayerLands() {
        MockCommonPlayer player = new MockCommonPlayer("Intrus");

        fire(player);

        assertNull(player.getLastMessage(), "Rien ne doit partir tout de suite");
        assertEquals(1, pendingNotifications.flush(player), "Le refus doit etre rejoue a l'arrivee");
        assertNotNull(player.getLastMessage());
    }

    @Test
    public void testAnAuthorisedPlayerGoesThroughAndStartsTheServer() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi")
                .grant("panelautostarter.join.survie");
        player.setCurrentServer(limboServer);

        fire(player);

        assertEquals(fr.farmvivi.panelautostarter.MinecraftServerStatus.STARTING, managed.getStatus());
        assertTrue(managed.isQueued(player), "Il doit rejoindre la file");
    }

    /**
     * Les deux refus ne s'expliquent pas de la même façon : une liste blanche
     * se corrige en demandant à être ajouté, une permission relève des groupes.
     */
    @Test
    public void testAWhitelistRefusalPointsToTheRightDoor() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi")
                .grant("panelautostarter.join.survie");
        player.setCurrentServer(limboServer);
        Whitelist whitelist = whitelists.get("survie");
        whitelist.setEnabled(true);

        fire(player);

        String message = plain(player.getLastMessage());
        assertTrue(message.contains("liste blanche"),
                "Le refus doit nommer la liste blanche : " + message);
        assertFalse(message.contains("permission"),
                "Et ne pas envoyer le joueur frapper a la mauvaise porte : " + message);
    }

    /**
     * Un joueur peut avoir rejoint la file avant qu'on ne lui retire l'accès.
     */
    @Test
    public void testARefusedPlayerIsPulledOutOfHisQueue() {
        MockCommonPlayer player = new MockCommonPlayer("Intrus");
        player.setCurrentServer(limboServer);
        managed.enqueue(player);

        fire(player);

        assertFalse(managed.isQueued(player), "Le refus doit aussi le sortir de la file");
    }

    /**
     * Un serveur que le plugin ne gère pas ne le regarde pas.
     */
    @Test
    public void testAnUnmanagedServerIsLeftAlone() {
        MockCommonPlayer player = new MockCommonPlayer("Intrus");
        player.setCurrentServer(limboServer);
        when(mockEvent.getTarget()).thenReturn(new MockCommonServer("autre"));
        when(mockEvent.getPlayer()).thenReturn(player);

        listener.onServerConnect(mockEvent);

        verify(mockEvent, never()).setCancelled(true);
        assertNull(player.getLastMessage());
    }
}
