package fr.farmvivi.panelautostarter;

import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals("Doit avoir 3 statuts", 3, statuses.length);
    }

    /**
     * Test : Vérifier que OFFLINE existe
     */
    @Test
    public void testOfflineStatusExists() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        assertNotNull("OFFLINE ne doit pas être null", offline);
        assertEquals("Le nom doit être OFFLINE", "OFFLINE", offline.name());
    }

    /**
     * Test : Vérifier que STARTING existe
     */
    @Test
    public void testStartingStatusExists() {
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        assertNotNull("STARTING ne doit pas être null", starting);
        assertEquals("Le nom doit être STARTING", "STARTING", starting.name());
    }

    /**
     * Test : Vérifier que ONLINE existe
     */
    @Test
    public void testOnlineStatusExists() {
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;
        assertNotNull("ONLINE ne doit pas être null", online);
        assertEquals("Le nom doit être ONLINE", "ONLINE", online.name());
    }

    /**
     * Test : Vérifier que les statuts sont distincts
     */
    @Test
    public void testStatusesAreDistinct() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;

        assertNotEquals("OFFLINE doit être différent de STARTING", offline, starting);
        assertNotEquals("OFFLINE doit être différent de ONLINE", offline, online);
        assertNotEquals("STARTING doit être différent de ONLINE", starting, online);
    }

    /**
     * Test : Vérifier valueOf()
     */
    @Test
    public void testValueOf() {
        MinecraftServerStatus offline = MinecraftServerStatus.valueOf("OFFLINE");
        assertEquals("valueOf doit retourner OFFLINE", MinecraftServerStatus.OFFLINE, offline);

        MinecraftServerStatus starting = MinecraftServerStatus.valueOf("STARTING");
        assertEquals("valueOf doit retourner STARTING", MinecraftServerStatus.STARTING, starting);

        MinecraftServerStatus online = MinecraftServerStatus.valueOf("ONLINE");
        assertEquals("valueOf doit retourner ONLINE", MinecraftServerStatus.ONLINE, online);
    }

    /**
     * Test : Vérifier que valueOf() lève exception pour statut invalide
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidStatus() {
        MinecraftServerStatus.valueOf("INVALID");
    }

    /**
     * Test : Vérifier ordinal()
     */
    @Test
    public void testOrdinal() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;

        assertEquals("OFFLINE doit être ordinal 0", 0, offline.ordinal());
        assertEquals("STARTING doit être ordinal 1", 1, starting.ordinal());
        assertEquals("ONLINE doit être ordinal 2", 2, online.ordinal());
    }

    /**
     * Test : Vérifier la comparaison
     */
    @Test
    public void testCompareTo() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;

        assertTrue("OFFLINE doit être inférieur à STARTING", offline.compareTo(starting) < 0);
        assertTrue("STARTING doit être supérieur à OFFLINE", starting.compareTo(offline) > 0);
        assertEquals("Même statut doit avoir compareTo == 0", 0, offline.compareTo(offline));
    }
}
