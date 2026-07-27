package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ProxyPingEvent.
 */
public class ProxyPingEventTest {

    private ProxyPingEvent event;
    private CommonServerPing serverPing;
    private String host;

    @BeforeEach
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
        assertNotNull(event, "L'événement ne doit pas être null");
        assertNotNull(event.getHost(), "L'host ne doit pas être null");
        assertNotNull(event.getResponse(), "Le response ne doit pas être null");
    }

    /**
     * Test : Vérifier que getHost() retourne le bon host
     */
    @Test
    public void testGetHost() {
        assertEquals(host, event.getHost(), "L'host doit être celui passé au constructeur");
    }

    /**
     * Test : Vérifier que getResponse() retourne le bon response
     */
    @Test
    public void testGetResponse() {
        assertEquals(serverPing, event.getResponse(), "Le response doit être celui passé au constructeur");
    }

    /**
     * Test : Vérifier que setResponse() fonctionne correctement
     */
    @Test
    public void testSetResponse() {
        CommonServerPing newPing = new MockCommonServerPing();
        event.setResponse(newPing);
        assertEquals(newPing, event.getResponse(), "Le response doit être celui défini");
    }

    /**
     * Test : Vérifier que le ProxyPingEvent hérite de Event
     */
    @Test
    public void testProxyPingEventInheritance() {
        assertTrue(event instanceof Event, "ProxyPingEvent doit être une instance de Event");
    }

    /**
     * Test : Vérifier avec un host vide
     */
    @Test
    public void testProxyPingEventWithEmptyHost() {
        ProxyPingEvent eventEmpty = new ProxyPingEvent("", serverPing);
        assertEquals("", eventEmpty.getHost(), "L'host peut être vide");
    }

    /**
     * Test : Vérifier avec null response
     */
    @Test
    public void testProxyPingEventWithNullResponse() {
        ProxyPingEvent eventWithNull = new ProxyPingEvent(host, null);
        assertNull(eventWithNull.getResponse(), "Le response peut être null");
    }
}
