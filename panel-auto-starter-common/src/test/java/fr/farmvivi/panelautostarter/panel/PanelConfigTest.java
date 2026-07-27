package fr.farmvivi.panelautostarter.panel;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la lecture des paramètres de connexion au panel.
 * <p>
 * Couvre les trois branches de {@link PanelConfig#from(Configuration)} :
 * format 3.x, format 2.x déprécié, et absence de configuration. C'est le seul
 * endroit du plugin où une régression serait silencieuse pour l'utilisateur,
 * d'où la couverture détaillée.
 */
public class PanelConfigTest {

    private static Configuration parse(String yaml) {
        return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new StringReader(yaml));
    }

    // ===================== Format 3.x =====================

    @Test
    public void testNewFormatPterodactyl() {
        PanelConfig config = PanelConfig.from(parse("""
                panel:
                  type: pterodactyl
                  url: https://panel.exemple.com
                  token: ptlc_abc
                """));

        assertEquals(PanelType.PTERODACTYL, config.getType());
        assertEquals("https://panel.exemple.com", config.getUrl());
        assertEquals("ptlc_abc", config.getToken());
        assertFalse(config.isLegacyFormat(), "Le format 3.x ne doit pas etre signale comme deprecie");
    }

    @Test
    public void testNewFormatPelican() {
        PanelConfig config = PanelConfig.from(parse("""
                panel:
                  type: pelican
                  url: https://pelican.exemple.com
                  token: plcn_abc
                """));

        assertEquals(PanelType.PELICAN, config.getType());
        assertEquals("https://pelican.exemple.com", config.getUrl());
        assertFalse(config.isLegacyFormat());
    }

    /**
     * Le type est facultatif : une configuration 3.x sans 'type' doit rester
     * valide et retomber sur Pterodactyl, le panel historique.
     */
    @Test
    public void testNewFormatDefaultsToPterodactylWhenTypeOmitted() {
        PanelConfig config = PanelConfig.from(parse("""
                panel:
                  url: https://panel.exemple.com
                  token: ptlc_abc
                """));

        assertEquals(PanelType.PTERODACTYL, config.getType());
        assertFalse(config.isLegacyFormat());
    }

    @Test
    public void testNewFormatRejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> PanelConfig.from(parse("""
                panel:
                  type: pufferpanel
                  url: https://panel.exemple.com
                  token: abc
                """)));
    }

    // ===================== Format 2.x deprecie =====================

    @Test
    public void testLegacyFormatIsStillRead() {
        PanelConfig config = PanelConfig.from(parse("""
                pterodactyl:
                  url: https://ancien.exemple.com
                  token: ptlc_ancien
                """));

        assertEquals(PanelType.PTERODACTYL, config.getType(),
                "Le format 2.x ne pouvait designer que Pterodactyl");
        assertEquals("https://ancien.exemple.com", config.getUrl());
        assertEquals("ptlc_ancien", config.getToken());
        assertTrue(config.isLegacyFormat(), "Le format 2.x doit etre signale comme deprecie");
    }

    /**
     * Une installation qui a migre partiellement (nouvelle section ajoutee,
     * ancienne pas encore retiree) doit utiliser la nouvelle sans avertissement.
     */
    @Test
    public void testNewFormatTakesPrecedenceOverLegacy() {
        PanelConfig config = PanelConfig.from(parse("""
                panel:
                  type: pelican
                  url: https://nouveau.exemple.com
                  token: nouveau
                pterodactyl:
                  url: https://ancien.exemple.com
                  token: ancien
                """));

        assertEquals(PanelType.PELICAN, config.getType());
        assertEquals("https://nouveau.exemple.com", config.getUrl());
        assertEquals("nouveau", config.getToken());
        assertFalse(config.isLegacyFormat());
    }

    @Test
    public void testLegacyWarningMentionsBothSections() {
        String warning = PanelConfig.legacyFormatWarning();

        assertTrue(warning.contains("pterodactyl"), "Le message doit nommer la section depreciee");
        assertTrue(warning.contains("panel"), "Le message doit nommer la section de remplacement");
        assertTrue(warning.contains("github.com/FarmVivi/panel-auto-starter"), "Le message doit pointer vers la doc en ligne");
    }

    // ===================== Absence de configuration =====================

    @Test
    public void testMissingPanelSectionFailsWithExplicitMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> PanelConfig.from(parse("""
                        queue:
                          server: lobby
                        """)));

        assertTrue(ex.getMessage().contains("panel"), "Le message doit nommer la section attendue");
        assertTrue(ex.getMessage().contains("github.com/FarmVivi/panel-auto-starter"), "Le message doit pointer vers la doc en ligne");
    }

    @Test
    public void testNullConfigFails() {
        assertThrows(IllegalStateException.class, () -> PanelConfig.from(null));
    }

    // ===================== Valeurs invalides =====================

    @Test
    public void testBlankTokenIsRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PanelConfig.from(parse("""
                        panel:
                          url: https://panel.exemple.com
                          token: "   "
                        """)));

        assertTrue(ex.getMessage().contains("token"), "Le message doit nommer la cle fautive");
    }

    @Test
    public void testValuesAreTrimmed() {
        PanelConfig config = PanelConfig.from(parse("""
                panel:
                  url: "  https://panel.exemple.com  "
                  token: "  ptlc_abc  "
                """));

        assertEquals("https://panel.exemple.com", config.getUrl());
        assertEquals("ptlc_abc", config.getToken());
    }
}
