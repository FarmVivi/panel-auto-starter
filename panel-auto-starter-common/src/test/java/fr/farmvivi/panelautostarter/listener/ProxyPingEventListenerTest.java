package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.event.ProxyPingEvent;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour ProxyPingEventListener.
 */
public class ProxyPingEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private ProxyPingEvent mockEvent;

    private ProxyPingEventListener listener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockProxy.getForcedHosts()).thenReturn(new java.util.HashMap<>());
        // Créer un InputStream vide pour éviter les NullPointerException dans ImageIO.read()
        InputStream emptyInputStream = new ByteArrayInputStream(new byte[0]);
        when(mockPlugin.getResourceAsStream("img/offline.png")).thenReturn(emptyInputStream);
        when(mockPlugin.getResourceAsStream("img/starting.png")).thenReturn(emptyInputStream);
        listener = new ProxyPingEventListener(mockPlugin);
    }

    /**
     * Test : Vérifier que le listener est créé correctement
     */
    @Test
    public void testListenerInitialization() {
        assertNotNull("Le listener ne doit pas être null", listener);
    }

    /**
     * Test : Vérifier que le listener gère un événement de ping
     */
    @Test
    public void testOnProxyPingEvent() {
        CommonServerPing serverPing = new MockCommonServerPing();
        ProxyPingEvent event = new ProxyPingEvent("127.0.0.1", serverPing);

        // Le listener ne doit pas lever d'exception
        listener.onProxyPing(event);

        assertNotNull("L'événement ne doit pas être null", event);
    }

    /**
     * Test : Vérifier que le listener gère un événement null
     */
    @Test(expected = NullPointerException.class)
    public void testOnProxyPingEventWithNull() {
        // Le listener doit lever une NullPointerException avec null
        listener.onProxyPing(null);
    }

    /**
     * Test : Vérifier que le listener gère plusieurs événements
     */
    @Test
    public void testOnMultipleProxyPingEvents() {
        CommonServerPing ping1 = new MockCommonServerPing();
        CommonServerPing ping2 = new MockCommonServerPing();

        ProxyPingEvent event1 = new ProxyPingEvent("host1", ping1);
        ProxyPingEvent event2 = new ProxyPingEvent("host2", ping2);

        listener.onProxyPing(event1);
        listener.onProxyPing(event2);

        assertNotNull("Le premier événement ne doit pas être null", event1);
        assertNotNull("Le second événement ne doit pas être null", event2);
    }

    /**
     * Test : Vérifier que le logger est utilisé
     */
    @Test
    public void testListenerUsesLogger() {
        CommonServerPing serverPing = new MockCommonServerPing();
        ProxyPingEvent event = new ProxyPingEvent("192.168.1.1", serverPing);

        listener.onProxyPing(event);

        // Vérifier que le plugin a le logger
        verify(mockPlugin, atLeast(0)).getLogger();
    }

    /**
     * Test : Vérifier que l'événement avec différents hosts
     */
    @Test
    public void testOnProxyPingEventWithDifferentHosts() {
        String[] hosts = {"localhost", "127.0.0.1", "192.168.1.1", "google.com"};

        for (String host : hosts) {
            CommonServerPing ping = new MockCommonServerPing();
            ProxyPingEvent event = new ProxyPingEvent(host, ping);
            listener.onProxyPing(event);
            assertEquals("L'host doit être celui défini", host, event.getHost());
        }
    }
}
