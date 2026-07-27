package fr.farmvivi.panelautostarter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour MinecraftServerStatus et les transitions d'état.
 */
public class MinecraftServerStatusTest {

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test 1.1.1 : Vérifier que MinecraftServerStatus énumère les états correctement
     */
    @Test
    public void testMinecraftServerStatusEnumValues() {
        // Vérifier que tous les états existent
        assertNotNull(MinecraftServerStatus.OFFLINE);
        assertNotNull(MinecraftServerStatus.STARTING);
        assertNotNull(MinecraftServerStatus.ONLINE);
    }

    /**
     * Test 1.1.2 : Vérifier que les états peuvent être comparés
     */
    @Test
    public void testStatusComparison() {
        MinecraftServerStatus offline = MinecraftServerStatus.OFFLINE;
        MinecraftServerStatus starting = MinecraftServerStatus.STARTING;
        MinecraftServerStatus online = MinecraftServerStatus.ONLINE;

        // Chaque état doit être unique
        assertNotEquals(offline, starting);
        assertNotEquals(starting, online);
        assertNotEquals(offline, online);

        // Les états doivent être égaux à eux-mêmes
        assertEquals(MinecraftServerStatus.OFFLINE, offline);
        assertEquals(MinecraftServerStatus.STARTING, starting);
        assertEquals(MinecraftServerStatus.ONLINE, online);
    }

    /**
     * Test 1.1.3 : Vérifier que equals() fonctionne correctement
     */
    @Test
    public void testStatusEquals() {
        assertEquals(MinecraftServerStatus.OFFLINE, MinecraftServerStatus.OFFLINE);
        assertNotEquals(MinecraftServerStatus.OFFLINE, MinecraftServerStatus.ONLINE);
    }

    /**
     * Test 1.1.4 : Vérifier que toString() retourne le nom de l'état
     */
    @Test
    public void testStatusToString() {
        assertEquals("OFFLINE", MinecraftServerStatus.OFFLINE.toString());
        assertEquals("STARTING", MinecraftServerStatus.STARTING.toString());
        assertEquals("ONLINE", MinecraftServerStatus.ONLINE.toString());
    }

    /**
     * Test 1.1.5 : Vérifier que les valeurs peuvent être obtenues par valueOf()
     */
    @Test
    public void testStatusValueOf() {
        assertEquals(MinecraftServerStatus.OFFLINE, MinecraftServerStatus.valueOf("OFFLINE"));
        assertEquals(MinecraftServerStatus.STARTING, MinecraftServerStatus.valueOf("STARTING"));
        assertEquals(MinecraftServerStatus.ONLINE, MinecraftServerStatus.valueOf("ONLINE"));
    }
}
