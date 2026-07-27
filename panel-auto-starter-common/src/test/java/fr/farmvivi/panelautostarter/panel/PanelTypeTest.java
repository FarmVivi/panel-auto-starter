package fr.farmvivi.panelautostarter.panel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la résolution du type de panel depuis la configuration.
 */
public class PanelTypeTest {

    @Test
    public void testFromLowerCase() {
        assertEquals(PanelType.PTERODACTYL, PanelType.from("pterodactyl"));
        assertEquals(PanelType.PELICAN, PanelType.from("pelican"));
    }

    @Test
    public void testFromIsCaseInsensitiveAndTrimmed() {
        assertEquals(PanelType.PELICAN, PanelType.from("Pelican"));
        assertEquals(PanelType.PELICAN, PanelType.from("PELICAN"));
        assertEquals(PanelType.PTERODACTYL, PanelType.from("  pterodactyl  "));
    }

    @Test
    public void testFromUnknownValueMentionsAcceptedValues() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PanelType.from("pufferpanel"));

        assertTrue(ex.getMessage().contains("pufferpanel"),
                "Le message doit rappeler la valeur fautive");
        assertTrue(ex.getMessage().contains("pterodactyl") && ex.getMessage().contains("pelican"),
                "Le message doit lister les valeurs acceptées");
    }

    @Test
    public void testFromNull() {
        assertThrows(IllegalArgumentException.class, () -> PanelType.from(null));
    }
}
