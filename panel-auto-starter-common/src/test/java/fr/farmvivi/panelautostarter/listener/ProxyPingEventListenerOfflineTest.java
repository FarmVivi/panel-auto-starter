package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests du MOTD servi quand un serveur géré est hors-ligne ou en démarrage.
 * <p>
 * Le plugin n'embarque plus d'icône : il rejoue le MOTD conservé du serveur,
 * décoré d'une pastille d'état.
 */
public class ProxyPingEventListenerOfflineTest {

    private static final String FORCED_HOST = "play.streetland.farmvivi.fr";

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private MinecraftServer mockManagedServer;

    @Mock
    private ServerMotd mockMotd;

    @Mock
    private CommonFavicon decoratedFavicon;

    private CommonServer backendServer;
    private List<CommonPlayer> queue;
    private ProxyPingEventListener listener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        backendServer = new MockCommonServer("streetland_survie", "Streetland Survie");
        queue = new ArrayList<>();

        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);

        Map<String, String> forcedHosts = new HashMap<>();
        forcedHosts.put(FORCED_HOST, "streetland_survie");
        when(mockProxy.getForcedHosts()).thenReturn(forcedHosts);
        when(mockProxy.getServer("streetland_survie")).thenReturn(backendServer);

        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(backendServer, mockManagedServer);
        when(mockPlugin.getServers()).thenReturn(servers);

        when(mockManagedServer.getServer()).thenReturn(backendServer);
        when(mockManagedServer.getQueue()).thenReturn(queue);
        when(mockManagedServer.getMotd()).thenReturn(mockMotd);

        listener = new ProxyPingEventListener(mockPlugin);
    }

    private void givenCachedMotd(String description) {
        when(mockMotd.isEmpty()).thenReturn(false);
        when(mockMotd.getDescription()).thenReturn(Component.text(description));
        when(mockMotd.getFavicon(any())).thenReturn(decoratedFavicon);
    }

    private CommonServerPing ping(MinecraftServerStatus status) {
        when(mockManagedServer.getStatus()).thenReturn(status);
        CommonServerPing response = new MockCommonServerPing();
        response.setDescriptionComponent(Component.text("MOTD du proxy"));
        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, response));
        return response;
    }

    // ===================== Hors-ligne =====================

    @Test
    public void testOfflineReusesCachedDescriptionAndDecoratedFavicon() {
        givenCachedMotd("MOTD du serveur");

        CommonServerPing response = ping(MinecraftServerStatus.OFFLINE);

        assertEquals(Component.text("MOTD du serveur"), response.getDescriptionComponent(),
                "Le MOTD conserve doit etre rejoue");
        assertSame(decoratedFavicon, response.getFavicon());
        verify(mockMotd).getFavicon(MinecraftServerStatus.OFFLINE);
    }

    @Test
    public void testOfflineShowsZeroPlayers() {
        givenCachedMotd("MOTD du serveur");

        CommonServerPing response = ping(MinecraftServerStatus.OFFLINE);

        assertEquals(0, response.getOnlinePlayers());
        assertEquals(0, response.getMaxPlayers());
    }

    /**
     * Garde-fou contre la 2.x : elle forçait {@code protocolVersion} à -1 pour
     * afficher un texte rouge, lequel remplace le compteur de joueurs. Rétablir
     * ce détournement rendrait le « 0 joueur » invisible.
     */
    @Test
    public void testOfflineDoesNotHijackProtocolVersion() {
        givenCachedMotd("MOTD du serveur");
        when(mockManagedServer.getStatus()).thenReturn(MinecraftServerStatus.OFFLINE);

        CommonServerPing response = new MockCommonServerPing();
        response.setProtocolVersion(769);
        response.setProtocolName(Component.text("1.21"));
        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, response));

        assertEquals(769, response.getProtocolVersion(),
                "La version de protocole du proxy doit etre preservee");
        assertEquals(Component.text("1.21"), response.getProtocolName());
    }

    @Test
    public void testOfflineHoverExplainsHowToStartTheServer() {
        givenCachedMotd("MOTD du serveur");

        CommonServerPing response = ping(MinecraftServerStatus.OFFLINE);

        String hover = response.getSamplePlayers().toString();
        assertTrue(hover.contains("hors-ligne"), "Le survol doit indiquer que le serveur est eteint");
        assertTrue(hover.contains("Connectez-vous"), "Le survol doit expliquer comment le demarrer");
    }

    /**
     * Sans rien en cache, il n'y a rien de pertinent à afficher : la réponse du
     * proxy est laissée intacte.
     */
    @Test
    public void testOfflineWithoutCacheLeavesResponseUntouched() {
        when(mockMotd.isEmpty()).thenReturn(true);

        CommonServerPing response = ping(MinecraftServerStatus.OFFLINE);

        assertEquals(Component.text("MOTD du proxy"), response.getDescriptionComponent());
        assertNull(response.getFavicon());
        verify(mockMotd, never()).getFavicon(any());
    }

    // ===================== Démarrage =====================

    @Test
    public void testStartingUsesStartingFavicon() {
        givenCachedMotd("MOTD du serveur");

        CommonServerPing response = ping(MinecraftServerStatus.STARTING);

        assertEquals(Component.text("MOTD du serveur"), response.getDescriptionComponent());
        verify(mockMotd).getFavicon(MinecraftServerStatus.STARTING);
    }

    @Test
    public void testStartingListsTheQueueOnHover() {
        givenCachedMotd("MOTD du serveur");
        queue.add(new MockCommonPlayer("Vivi"));
        queue.add(new MockCommonPlayer("Bob"));

        CommonServerPing response = ping(MinecraftServerStatus.STARTING);

        String hover = response.getSamplePlayers().toString();
        assertTrue(hover.contains("Vivi") && hover.contains("Bob"),
                "Le survol doit lister les joueurs en attente");
        assertTrue(hover.contains("(2)"), "Le survol doit indiquer la taille de la file");
    }

    @Test
    public void testStartingWithEmptyQueueInvitesToJoin() {
        givenCachedMotd("MOTD du serveur");

        CommonServerPing response = ping(MinecraftServerStatus.STARTING);

        String hover = response.getSamplePlayers().toString();
        assertTrue(hover.contains("file d'attente"),
                "Sans personne en attente, le survol doit inviter a rejoindre la file");
    }

    @Test
    public void testStartingAlsoShowsZeroPlayers() {
        givenCachedMotd("MOTD du serveur");
        queue.add(new MockCommonPlayer("Vivi"));

        CommonServerPing response = ping(MinecraftServerStatus.STARTING);

        assertEquals(0, response.getOnlinePlayers(),
                "La file d'attente ne doit pas etre comptee comme des joueurs connectes");
        assertEquals(0, response.getMaxPlayers());
    }

    @Test
    public void testStartingWithoutCacheLeavesResponseUntouched() {
        when(mockMotd.isEmpty()).thenReturn(true);

        CommonServerPing response = ping(MinecraftServerStatus.STARTING);

        assertEquals(Component.text("MOTD du proxy"), response.getDescriptionComponent());
    }
}
