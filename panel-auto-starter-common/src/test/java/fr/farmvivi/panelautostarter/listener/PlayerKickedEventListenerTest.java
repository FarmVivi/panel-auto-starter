package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests du rattrapage des joueurs éjectés d'un serveur géré.
 * <p>
 * Le plugin impose la destination plutôt que de laisser le proxy décider : selon
 * la façon dont un serveur coupe la connexion, celui-ci renvoie le joueur
 * ailleurs ou le déconnecte, d'où un comportement erratique à l'arrêt d'un
 * serveur.
 */
public class PlayerKickedEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private MinecraftServer mockManagedServer;

    private CommonServer queueServer;
    private CommonServer managedServer;
    private CommonServer unmanagedServer;
    private MockCommonPlayer player;
    private PlayerKickedEventListener listener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        queueServer = new MockCommonServer("limbo");
        managedServer = new MockCommonServer("survie", "Survie");
        unmanagedServer = new MockCommonServer("creatif", "Creatif");
        player = new MockCommonPlayer("Vivi");

        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockConfiguration.getBoolean(eq("queue.catch-kicks"), anyBoolean())).thenReturn(true);
        when(mockConfiguration.getString("queue.server")).thenReturn("limbo");
        when(mockProxy.getServer("limbo")).thenReturn(queueServer);

        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(managedServer, mockManagedServer);
        doReturn(servers).when(mockPlugin).getServers();

        listener = new PlayerKickedEventListener(mockPlugin);
    }

    private PlayerKickedEvent kickedFrom(CommonServer from) {
        PlayerKickedEvent event = new PlayerKickedEvent(player, from, Component.text("Server closed"));
        listener.onPlayerKicked(event);
        return event;
    }

    @Test
    public void testPlayerKickedFromManagedServerGoesToTheQueueServer() {
        PlayerKickedEvent event = kickedFrom(managedServer);

        assertSame(queueServer, event.getRedirectTo(),
                "Le joueur doit etre renvoye vers le serveur d'attente");
    }

    /**
     * Le plugin n'a pas à s'immiscer dans le sort des joueurs d'un serveur
     * qu'il ne gère pas.
     */
    @Test
    public void testPlayerKickedFromUnmanagedServerIsLeftToTheProxy() {
        PlayerKickedEvent event = kickedFrom(unmanagedServer);

        assertNull(event.getRedirectTo());
    }

    /**
     * Renvoyer vers le serveur dont le joueur vient d'être éjecté l'enfermerait
     * dans une boucle de reconnexions.
     */
    @Test
    public void testKickFromTheQueueServerIsNotRedirectedToItself() {
        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(queueServer, mockManagedServer);
        doReturn(servers).when(mockPlugin).getServers();

        PlayerKickedEvent event = kickedFrom(queueServer);

        assertNull(event.getRedirectTo(),
                "Aucune redirection ne doit boucler sur le serveur d'origine");
    }

    @Test
    public void testDisabledByConfiguration() {
        when(mockConfiguration.getBoolean(eq("queue.catch-kicks"), anyBoolean())).thenReturn(false);

        PlayerKickedEvent event = kickedFrom(managedServer);

        assertNull(event.getRedirectTo());
    }

    @Test
    public void testMissingQueueServerLeavesTheProxyInCharge() {
        when(mockProxy.getServer("limbo")).thenReturn(null);

        PlayerKickedEvent event = kickedFrom(managedServer);

        assertNull(event.getRedirectTo(),
                "Sans serveur d'attente joignable, mieux vaut ne rien imposer");
    }

    @Test
    public void testUnknownOriginIsIgnored() {
        PlayerKickedEvent event = kickedFrom(null);

        assertNull(event.getRedirectTo());
    }

    /**
     * La raison annoncée par le serveur est conservée sur l'événement : les
     * adaptateurs de plateforme la transmettent au joueur, pour qu'une exclusion
     * volontaire ne soit pas masquée par la redirection.
     */
    @Test
    public void testKickReasonIsPreserved() {
        PlayerKickedEvent event = kickedFrom(managedServer);

        assertEquals(Component.text("Server closed"), event.getReason());
    }
}
