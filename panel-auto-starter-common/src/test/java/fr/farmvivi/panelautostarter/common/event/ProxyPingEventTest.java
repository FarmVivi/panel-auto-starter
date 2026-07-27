package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests unitaires pour ProxyPingEvent.
 */
public class ProxyPingEventTest {

    private ProxyPingEvent event;
    private CommonServerPing serverPing;
    private String host;

    @Before
    public void setUp() {
        host = "127.0.0.1";
        serverPing = new MockCommonServerPing();
        event = new ProxyPingEvent(host, serverPing);
    }

    /**
     * Test : Vérifier que ProxyPingEvent contient l'host et le serverPing
     */
    @Test
    public void testProxyPingEventConstructor() {
        assertNotNull("L'événement ne doit pas être null", event);
        assertNotNull("L'host ne doit pas être null", event.getHost());
        assertNotNull("Le response ne doit pas être null", event.getResponse());
    }

    /**
     * Test : Vérifier que getHost() retourne le bon host
     */
    @Test
    public void testGetHost() {
        assertEquals("L'host doit être celui passé au constructeur", 
            host, event.getHost());
    }

    /**
     * Test : Vérifier que getResponse() retourne le bon response
     */
    @Test
    public void testGetResponse() {
        assertEquals("Le response doit être celui passé au constructeur", 
            serverPing, event.getResponse());
    }

    /**
     * Test : Vérifier que setResponse() fonctionne correctement
     */
    @Test
    public void testSetResponse() {
        CommonServerPing newPing = new MockCommonServerPing();
        event.setResponse(newPing);
        assertEquals("Le response doit être celui défini", 
            newPing, event.getResponse());
    }

    /**
     * Test : Vérifier que le ProxyPingEvent hérite de Event
     */
    @Test
    public void testProxyPingEventInheritance() {
        assertTrue("ProxyPingEvent doit être une instance de Event", 
            event instanceof Event);
    }

    /**
     * Test : Vérifier avec un host vide
     */
    @Test
    public void testProxyPingEventWithEmptyHost() {
        ProxyPingEvent eventEmpty = new ProxyPingEvent("", serverPing);
        assertEquals("L'host peut être vide", "", eventEmpty.getHost());
    }

    /**
     * Test : Vérifier avec null response
     */
    @Test
    public void testProxyPingEventWithNullResponse() {
        ProxyPingEvent eventWithNull = new ProxyPingEvent(host, null);
        assertNull("Le response peut être null", eventWithNull.getResponse());
    }
}
