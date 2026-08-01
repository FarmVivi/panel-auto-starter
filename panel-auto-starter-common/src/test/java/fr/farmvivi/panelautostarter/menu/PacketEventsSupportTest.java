package fr.farmvivi.panelautostarter.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que la détection de PacketEvents ne casse pas quand il n'est pas là.
 * <p>
 * C'est tout son intérêt : la bibliothèque est facultative, et le plugin doit
 * démarrer sur un proxy qui ne l'a pas installée. Les tests s'exécutent
 * justement dans cette situation — la dépendance est déclarée « provided », donc
 * présente à la compilation, mais son API n'est jamais initialisée.
 */
public class PacketEventsSupportTest {

    /**
     * Une API non initialisée doit se lire comme une absence, pas comme une
     * erreur : c'est ce qui distingue le repli du plantage.
     */
    @Test
    public void testAnUninitialisedApiCountsAsAbsent() {
        assertFalse(PacketEventsSupport.isPresent(),
                "PacketEvents n'est pas initialise dans les tests : la detection doit le dire");
    }

    /**
     * La détection est appelée au démarrage du plugin : elle ne doit jamais
     * lever, quelle que soit la raison de l'absence.
     */
    @Test
    public void testDetectionNeverThrows() {
        assertDoesNotThrow(() -> PacketEventsSupport.isPresent());
        // Repetee, elle doit rester stable : le resultat conditionne
        // l'enregistrement du rendu, qui n'a lieu qu'une fois.
        assertEquals(PacketEventsSupport.isPresent(), PacketEventsSupport.isPresent());
    }
}
