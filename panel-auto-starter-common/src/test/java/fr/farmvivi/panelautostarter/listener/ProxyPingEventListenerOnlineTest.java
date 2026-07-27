package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests du MOTD servi pour un serveur géré <strong>en ligne</strong>.
 * <p>
 * Ce chemin rend inutile le {@code ping-passthrough} de Velocity, qui ne sait
 * pas distinguer l'adresse du proxy d'un forced host : réglé sur {@code "all"},
 * il fait remonter le MOTD du premier serveur de la liste {@code try} y compris
 * quand un joueur ping le proxy lui-même. En servant le backend ici, le proxy
 * peut repasser en {@code "disabled"} et conserver son propre MOTD.
 */
public class ProxyPingEventListenerOnlineTest {

    private static final String FORCED_HOST = "play.streetland.farmvivi.fr";

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private MinecraftServer mockManagedServer;

    private CommonServer backendServer;
    private ProxyPingEventListener listener;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        backendServer = new MockCommonServer("streetland_survie", "Streetland Survie");

        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getResourceAsStream(anyString()))
                .thenAnswer(invocation -> new ByteArrayInputStream(new byte[0]));

        Map<String, String> forcedHosts = new HashMap<>();
        forcedHosts.put(FORCED_HOST, "streetland_survie");
        when(mockProxy.getForcedHosts()).thenReturn(forcedHosts);
        when(mockProxy.getServer("streetland_survie")).thenReturn(backendServer);

        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(backendServer, mockManagedServer);
        when(mockPlugin.getServers()).thenReturn(servers);

        when(mockManagedServer.getStatus()).thenReturn(MinecraftServerStatus.ONLINE);

        listener = new ProxyPingEventListener(mockPlugin);
    }

    private CommonServerPing backendPingWith(String motd, int online, int max) {
        MockCommonServerPing backend = new MockCommonServerPing(online, max);
        backend.setDescriptionComponent(Component.text(motd));
        backend.setSamplePlayers(List.of(Component.text("Vivi")));
        return backend;
    }

    @Test
    public void testOnlineServerServesBackendMotd() {
        when(mockManagedServer.peekServerPing())
                .thenReturn(backendPingWith("MOTD du backend", 7, 40));

        CommonServerPing response = new MockCommonServerPing();
        response.setDescriptionComponent(Component.text("MOTD du proxy"));
        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, response));

        assertEquals(Component.text("MOTD du backend"), response.getDescriptionComponent(),
                "Le forced host doit afficher le MOTD de son backend");
        assertEquals(7, response.getOnlinePlayers());
        assertEquals(40, response.getMaxPlayers());
        assertEquals(List.of(Component.text("Vivi")), response.getSamplePlayers());
    }

    /**
     * La version de protocole appartient au proxy : c'est elle qui indique au
     * client s'il peut se connecter, et le proxy accepte une plage de versions
     * plus large que le backend. La recopier afficherait « version incompatible »
     * a des clients que le proxy sait pourtant servir.
     */
    @Test
    public void testProtocolVersionIsNotOverriddenByBackend() {
        MockCommonServerPing backend = (MockCommonServerPing) backendPingWith("MOTD", 1, 10);
        backend.setProtocolVersion(47);
        backend.setProtocolName(Component.text("1.8"));
        when(mockManagedServer.peekServerPing()).thenReturn(backend);

        CommonServerPing response = new MockCommonServerPing();
        response.setProtocolVersion(769);
        response.setProtocolName(Component.text("1.21"));
        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, response));

        assertEquals(769, response.getProtocolVersion(),
                "La version de protocole du proxy doit etre conservee");
        assertEquals(Component.text("1.21"), response.getProtocolName());
    }

    @Test
    public void testResponseUntouchedWhenNoBackendPingCachedYet() {
        when(mockManagedServer.peekServerPing()).thenReturn(null);

        CommonServerPing response = new MockCommonServerPing();
        response.setDescriptionComponent(Component.text("MOTD du proxy"));
        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, response));

        assertEquals(Component.text("MOTD du proxy"), response.getDescriptionComponent(),
                "Sans donnee en cache, la reponse du proxy doit etre laissee intacte");
    }

    /**
     * L'adresse principale du proxy n'est pas un forced host : le plugin ne doit
     * pas y toucher, sinon on reproduirait le probleme de ping-passthrough "all"
     * en affichant le MOTD d'un backend sur l'adresse du proxy.
     */
    @Test
    public void testProxyOwnAddressIsLeftAlone() {
        CommonServerPing response = new MockCommonServerPing();
        response.setDescriptionComponent(Component.text("MOTD du proxy"));

        listener.onProxyPing(new ProxyPingEvent("play.farmvivi.fr", response));

        assertEquals(Component.text("MOTD du proxy"), response.getDescriptionComponent(),
                "L'adresse du proxy doit conserver le MOTD du proxy");
        verify(mockManagedServer, never()).peekServerPing();
    }

    @Test
    public void testPingOnManagedServerMarksItAsWatched() {
        when(mockManagedServer.peekServerPing()).thenReturn(backendPingWith("MOTD", 0, 20));

        listener.onProxyPing(new ProxyPingEvent(FORCED_HOST, new MockCommonServerPing()));

        verify(mockManagedServer).notifyWatched();
    }
}
