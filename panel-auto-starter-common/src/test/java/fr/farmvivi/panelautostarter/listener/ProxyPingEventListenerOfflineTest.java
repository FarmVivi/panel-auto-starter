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
import fr.farmvivi.panelautostarter.motd.BundledFavicons;
import fr.farmvivi.panelautostarter.motd.MotdSettings;
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
 * Tests de l'application des réglages MOTD quand un serveur géré n'est pas en
 * ligne.
 * <p>
 * Chaque champ de la réponse se règle indépendamment ; ces tests vérifient que
 * chaque valeur possible produit bien l'effet annoncé dans {@code config.yml}.
 */
public class ProxyPingEventListenerOfflineTest {

    private static final String FORCED_HOST = "play.streetland.farmvivi.fr";
    private static final Component PROXY_MOTD = Component.text("MOTD du proxy");
    private static final Component CACHED_MOTD = Component.text("MOTD du serveur");

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
    private BundledFavicons mockBundled;

    @Mock
    private CommonFavicon cachedFavicon;

    @Mock
    private CommonFavicon bundledFavicon;

    private CommonServer backendServer;
    private List<CommonPlayer> queue;

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

        when(mockMotd.isEmpty()).thenReturn(false);
        when(mockMotd.getDescription()).thenReturn(CACHED_MOTD);
        when(mockMotd.getMaxPlayers()).thenReturn(20);
        when(mockMotd.getFavicon(any())).thenReturn(cachedFavicon);
        when(mockBundled.get(any())).thenReturn(bundledFavicon);
    }

    /** Construit des réglages hors-ligne à partir des défauts, champ par champ. */
    private MotdSettings offlineSettings(MotdSettings.Description description, MotdSettings.Favicon favicon,
                                         MotdSettings.Players players, MotdSettings.MaxPlayers maxPlayers,
                                         MotdSettings.Hover hover, MotdSettings.VersionLabel versionLabel) {
        MotdSettings.State base = MotdSettings.defaultOfflineState();
        MotdSettings.State state = new MotdSettings.State(description, favicon,
                base.faviconGrayscale(), base.faviconBadge(), players, maxPlayers, hover, versionLabel);
        return MotdSettings.of(state, MotdSettings.defaultStartingState());
    }

    private MotdSettings defaults() {
        return MotdSettings.defaults();
    }

    /** Envoie un ping et retourne la réponse telle que le listener l'a laissée. */
    private CommonServerPing ping(MotdSettings settings, MinecraftServerStatus status) {
        when(mockManagedServer.getStatus()).thenReturn(status);
        ProxyPingEventListener listener = new ProxyPingEventListener(mockPlugin, settings, mockBundled);

        CommonServerPing response = new MockCommonServerPing();
        response.setDescriptionComponent(PROXY_MOTD);
        response.setOnlinePlayers(7);
        response.setMaxPlayers(100);
        response.setProtocolVersion(769);
        response.setProtocolName(Component.text("1.21"));
        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, response));
        return response;
    }

    // ===================== description =====================

    @Test
    public void testDescriptionCachedReplaysTheServerMotd() {
        CommonServerPing response = ping(defaults(), MinecraftServerStatus.OFFLINE);

        assertEquals(CACHED_MOTD, response.getDescriptionComponent());
    }

    @Test
    public void testDescriptionPluginAnnouncesTheState() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.PLUGIN, MotdSettings.Favicon.CACHED,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        String rendered = response.getDescriptionComponent().toString();
        assertTrue(rendered.contains("hors-ligne"), "Le texte du plugin doit annoncer l'etat");
        assertTrue(rendered.contains("Streetland Survie"), "Le texte du plugin doit nommer le serveur");
    }

    @Test
    public void testDescriptionProxyLeavesItAlone() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.PROXY, MotdSettings.Favicon.CACHED,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertEquals(PROXY_MOTD, response.getDescriptionComponent());
    }

    // ===================== favicon =====================

    @Test
    public void testFaviconCachedUsesTheDecoratedServerIcon() {
        CommonServerPing response = ping(defaults(), MinecraftServerStatus.OFFLINE);

        assertSame(cachedFavicon, response.getFavicon());
        verify(mockMotd).getFavicon(MinecraftServerStatus.OFFLINE);
        verify(mockBundled, never()).get(any());
    }

    @Test
    public void testFaviconBundledUsesTheShippedIcon() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.CACHED, MotdSettings.Favicon.BUNDLED,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertSame(bundledFavicon, response.getFavicon());
        verify(mockBundled).get(MinecraftServerStatus.OFFLINE);
    }

    @Test
    public void testFaviconProxyLeavesItAlone() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.CACHED, MotdSettings.Favicon.PROXY,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertNull(response.getFavicon());
        verify(mockBundled, never()).get(any());
    }

    // ===================== compteurs =====================

    @Test
    public void testPlayersZeroAndMaxZero() {
        CommonServerPing response = ping(defaults(), MinecraftServerStatus.OFFLINE);

        assertEquals(0, response.getOnlinePlayers());
        assertEquals(0, response.getMaxPlayers());
    }

    @Test
    public void testMaxPlayersCachedRestoresTheUsualCapacity() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.CACHED, MotdSettings.Favicon.CACHED,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.CACHED,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertEquals(0, response.getOnlinePlayers());
        assertEquals(20, response.getMaxPlayers(), "Le maximum memorise doit etre reutilise");
    }

    @Test
    public void testPlayersQueueCountsWaitingPlayers() {
        queue.add(new MockCommonPlayer("Vivi"));
        queue.add(new MockCommonPlayer("Bob"));
        MotdSettings.State starting = new MotdSettings.State(
                MotdSettings.Description.CACHED, MotdSettings.Favicon.CACHED, false, true,
                MotdSettings.Players.QUEUE, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(
                MotdSettings.of(MotdSettings.defaultOfflineState(), starting),
                MinecraftServerStatus.STARTING);

        assertEquals(2, response.getOnlinePlayers());
    }

    @Test
    public void testCountsProxyLeavesThemAlone() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.CACHED, MotdSettings.Favicon.CACHED,
                MotdSettings.Players.PROXY, MotdSettings.MaxPlayers.PROXY,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertEquals(7, response.getOnlinePlayers());
        assertEquals(100, response.getMaxPlayers());
    }

    // ===================== survol =====================

    @Test
    public void testHoverPluginExplainsHowToStartTheServer() {
        CommonServerPing response = ping(defaults(), MinecraftServerStatus.OFFLINE);

        String hover = response.getSamplePlayers().toString();
        assertTrue(hover.contains("hors-ligne"));
        assertTrue(hover.contains("Connectez-vous"));
    }

    @Test
    public void testHoverPluginListsTheQueueWhileStarting() {
        queue.add(new MockCommonPlayer("Vivi"));
        queue.add(new MockCommonPlayer("Bob"));

        CommonServerPing response = ping(defaults(), MinecraftServerStatus.STARTING);

        String hover = response.getSamplePlayers().toString();
        assertTrue(hover.contains("Vivi") && hover.contains("Bob"));
        assertTrue(hover.contains("(2)"));
    }

    @Test
    public void testHoverNoneClearsIt() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.CACHED, MotdSettings.Favicon.CACHED,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.NONE, MotdSettings.VersionLabel.NONE);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertTrue(response.getSamplePlayers().isEmpty());
    }

    // ===================== etiquette de version =====================

    /**
     * Par défaut l'étiquette est absente : c'est ce qui laisse le compteur de
     * joueurs visible, les deux occupant le même emplacement côté client.
     */
    @Test
    public void testVersionLabelNoneKeepsTheProxyProtocol() {
        CommonServerPing response = ping(defaults(), MinecraftServerStatus.OFFLINE);

        assertEquals(769, response.getProtocolVersion());
        assertEquals(Component.text("1.21"), response.getProtocolName());
    }

    @Test
    public void testVersionLabelTextRestoresThe2xBehaviour() {
        MotdSettings settings = offlineSettings(MotdSettings.Description.CACHED, MotdSettings.Favicon.CACHED,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.TEXT);

        CommonServerPing response = ping(settings, MinecraftServerStatus.OFFLINE);

        assertEquals(-1, response.getProtocolVersion(),
                "Un protocole invalide fait afficher l'etiquette a la place du compteur");
        assertTrue(response.getProtocolName().toString().contains("Hors-ligne"));
    }

    @Test
    public void testVersionLabelTextSaysStartingWhileBooting() {
        MotdSettings.State starting = new MotdSettings.State(
                MotdSettings.Description.CACHED, MotdSettings.Favicon.CACHED, false, true,
                MotdSettings.Players.ZERO, MotdSettings.MaxPlayers.ZERO,
                MotdSettings.Hover.PLUGIN, MotdSettings.VersionLabel.TEXT);

        CommonServerPing response = ping(
                MotdSettings.of(MotdSettings.defaultOfflineState(), starting),
                MinecraftServerStatus.STARTING);

        assertTrue(response.getProtocolName().toString().contains("marrage"),
                "L'etiquette doit annoncer le demarrage, obtenu: " + response.getProtocolName());
    }

    // ===================== cas limites =====================

    /**
     * Sans rien en cache, il n'y a pas de MOTD à rejouer — mais les autres
     * réglages continuent de s'appliquer.
     */
    @Test
    public void testWithoutCacheTheDescriptionFallsBackToTheProxy() {
        when(mockMotd.isEmpty()).thenReturn(true);
        when(mockMotd.getDescription()).thenReturn(null);
        when(mockMotd.getFavicon(any())).thenReturn(null);

        CommonServerPing response = ping(defaults(), MinecraftServerStatus.OFFLINE);

        assertEquals(PROXY_MOTD, response.getDescriptionComponent(),
                "Sans MOTD memorise, celui du proxy est conserve");
        assertNull(response.getFavicon());
    }

    @Test
    public void testProxyOwnAddressIsLeftAlone() {
        when(mockManagedServer.getStatus()).thenReturn(MinecraftServerStatus.OFFLINE);
        ProxyPingEventListener listener = new ProxyPingEventListener(mockPlugin, defaults(), mockBundled);

        CommonServerPing response = new MockCommonServerPing();
        response.setDescriptionComponent(PROXY_MOTD);
        listener.onProxyPing(new ProxyPingEvent("play.farmvivi.fr", response));

        assertEquals(PROXY_MOTD, response.getDescriptionComponent());
        verify(mockManagedServer, never()).getMotd();
    }

    @Test
    public void testPingOnManagedServerMarksItAsWatched() {
        ping(defaults(), MinecraftServerStatus.OFFLINE);

        verify(mockManagedServer).notifyWatched();
    }
}
