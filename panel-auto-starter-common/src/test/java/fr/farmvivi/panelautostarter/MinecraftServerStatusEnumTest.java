package fr.farmvivi.panelautostarter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour MinecraftServerStatus enum.
 */
public class MinecraftServerStatusEnumTest {

    /**
     * Test : Vérifier que tous les statuts existent
     */
    @Test
    public void testAllStatusesExist() {
        MinecraftServerStatus[] statuses = MinecraftServerStatus.values();
        assertEquals(3, statuses.length, "Doit avoir 3 statuts");
    }

    /**
     * Test : Vérifier que OFFLINE existe
     */
    @Test
    public void testOfflineStatusExists() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        assertNotNull(offline, "OFFLINE ne doit pas être null");
        assertEquals("OFFLINE", offline.name(), "Le nom doit être OFFLINE");
    }

    /**
     * Test : Vérifier que STARTING existe
     */
    @Test
    public void testStartingStatusExists() {
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        assertNotNull(starting, "STARTING ne doit pas être null");
        assertEquals("STARTING", starting.name(), "Le nom doit être STARTING");
    }

    /**
     * Test : Vérifier que ONLINE existe
     */
    @Test
    public void testOnlineStatusExists() {
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;
        assertNotNull(online, "ONLINE ne doit pas être null");
        assertEquals("ONLINE", online.name(), "Le nom doit être ONLINE");
    }

    /**
     * Test : Vérifier que les statuts sont distincts
     */
    @Test
    public void testStatusesAreDistinct() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;

        assertNotEquals(offline, starting, "OFFLINE doit être différent de STARTING");
        assertNotEquals(offline, online, "OFFLINE doit être différent de ONLINE");
        assertNotEquals(starting, online, "STARTING doit être différent de ONLINE");
    }

    /**
     * Test : Vérifier valueOf()
     */
    @Test
    public void testValueOf() {
        MinecraftServerStatus offline = MinecraftServerStatus.valueOf("OFFLINE");
        assertEquals(MinecraftServerStatus.OFFLINE, offline, "valueOf doit retourner OFFLINE");

        MinecraftServerStatus starting = MinecraftServerStatus.valueOf("STARTING");
        assertEquals(MinecraftServerStatus.STARTING, starting, "valueOf doit retourner STARTING");

        MinecraftServerStatus online = MinecraftServerStatus.valueOf("ONLINE");
        assertEquals(MinecraftServerStatus.ONLINE, online, "valueOf doit retourner ONLINE");
    }

    /**
     * Test : Vérifier que valueOf() lève exception pour statut invalide
     */
    @Test
    public void testValueOfInvalidStatus() {
        assertThrows(IllegalArgumentException.class, () -> MinecraftServerStatus.valueOf("INVALID"));
    }

    /**
     * Test : Vérifier ordinal()
     */
    @Test
    public void testOrdinal() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;

        assertEquals(0, offline.ordinal(), "OFFLINE doit être ordinal 0");
        assertEquals(1, starting.ordinal(), "STARTING doit être ordinal 1");
        assertEquals(2, online.ordinal(), "ONLINE doit être ordinal 2");
    }

    /**
     * Test : Vérifier la comparaison
     */
    @Test
    public void testCompareTo() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;

        assertTrue(offline.compareTo(starting) < 0, "OFFLINE doit être inférieur à STARTING");
        assertTrue(starting.compareTo(offline) > 0, "STARTING doit être supérieur à OFFLINE");
        assertEquals(0, offline.compareTo(offline), "Même statut doit avoir compareTo == 0");
    }
}
