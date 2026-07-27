package fr.farmvivi.panelautostarter.motd;

import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la lecture des réglages d'affichage du MOTD.
 */
public class MotdSettingsTest {

    private static MotdSettings parse(String yaml) {
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new StringReader(yaml));
        return MotdSettings.from(config);
    }

    // ===================== Défauts =====================

    @Test
    public void testDefaultsApplyWhenNothingIsConfigured() {
        MotdSettings settings = parse("queue:\n  server: lobby\n");

        MotdSettings.State offline = settings.getOffline();
        assertEquals(MotdSettings.Description.CACHED, offline.description());
        assertEquals(MotdSettings.Favicon.CACHED, offline.favicon());
        assertTrue(offline.faviconGrayscale(), "Un serveur eteint est desature par defaut");
        assertTrue(offline.faviconBadge());
        assertEquals(MotdSettings.Players.ZERO, offline.players());
        assertEquals(MotdSettings.MaxPlayers.ZERO, offline.maxPlayers());
        assertEquals(MotdSettings.Hover.PLUGIN, offline.hover());
        assertEquals(MotdSettings.VersionLabel.NONE, offline.versionLabel());
        assertTrue(settings.getWarnings().isEmpty());
    }

    /**
     * Un serveur en démarrage garde ses couleurs : il n'est pas éteint, il
     * revient.
     */
    @Test
    public void testStartingKeepsColoursByDefault() {
        MotdSettings settings = parse("queue:\n  server: lobby\n");

        assertFalse(settings.getStarting().faviconGrayscale());
        assertTrue(settings.getStarting().faviconBadge());
    }

    @Test
    public void testNullConfigYieldsDefaults() {
        MotdSettings settings = MotdSettings.from(null);

        assertEquals(MotdSettings.Description.CACHED, settings.getOffline().description());
        assertTrue(settings.getWarnings().isEmpty());
    }

    // ===================== Lecture =====================

    @Test
    public void testEveryFieldIsRead() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    description: plugin
                    favicon: bundled
                    favicon-grayscale: false
                    favicon-badge: false
                    players: queue
                    max-players: cached
                    hover: none
                    version-label: text
                """);

        MotdSettings.State offline = settings.getOffline();
        assertEquals(MotdSettings.Description.PLUGIN, offline.description());
        assertEquals(MotdSettings.Favicon.BUNDLED, offline.favicon());
        assertFalse(offline.faviconGrayscale());
        assertFalse(offline.faviconBadge());
        assertEquals(MotdSettings.Players.QUEUE, offline.players());
        assertEquals(MotdSettings.MaxPlayers.CACHED, offline.maxPlayers());
        assertEquals(MotdSettings.Hover.NONE, offline.hover());
        assertEquals(MotdSettings.VersionLabel.TEXT, offline.versionLabel());
    }

    /**
     * Les valeurs de configuration s'écrivent en kebab-case, les constantes Java
     * en SNAKE_CASE : la correspondance doit se faire dans les deux sens sans
     * que l'utilisateur ait à le savoir.
     */
    @Test
    public void testValuesAreCaseAndSeparatorInsensitive() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    description: "  PLUGIN  "
                    max-players: Cached
                """);

        assertEquals(MotdSettings.Description.PLUGIN, settings.getOffline().description());
        assertEquals(MotdSettings.MaxPlayers.CACHED, settings.getOffline().maxPlayers());
    }

    @Test
    public void testStatesAreConfiguredIndependently() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    favicon: bundled
                  starting:
                    favicon: proxy
                """);

        assertEquals(MotdSettings.Favicon.BUNDLED, settings.getOffline().favicon());
        assertEquals(MotdSettings.Favicon.PROXY, settings.getStarting().favicon());
    }

    @Test
    public void testPartialSectionKeepsDefaultsForOtherFields() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    hover: none
                """);

        assertEquals(MotdSettings.Hover.NONE, settings.getOffline().hover());
        assertEquals(MotdSettings.Description.CACHED, settings.getOffline().description(),
                "Les champs non renseignes gardent leur defaut");
    }

    // ===================== Valeurs invalides =====================

    /**
     * Un réglage cosmétique mal orthographié ne doit pas priver les joueurs de
     * leurs serveurs : le défaut s'applique et l'anomalie est signalée.
     */
    @Test
    public void testUnknownValueFallsBackToDefaultAndWarns() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    favicon: rainbow
                """);

        assertEquals(MotdSettings.Favicon.CACHED, settings.getOffline().favicon(),
                "Le defaut doit s'appliquer");
        assertEquals(1, settings.getWarnings().size());

        String warning = settings.getWarnings().get(0);
        assertTrue(warning.contains("rainbow"), "L'avertissement doit citer la valeur fautive");
        assertTrue(warning.contains("motd.offline.favicon"), "Il doit nommer la cle concernee");
        assertTrue(warning.contains("bundled"), "Il doit lister les valeurs acceptees");
    }

    @Test
    public void testEachInvalidValueIsReportedOnce() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    description: nope
                    hover: nope
                  starting:
                    players: nope
                """);

        assertEquals(3, settings.getWarnings().size(),
                "Chaque champ fautif doit etre signale separement");
    }

    @Test
    public void testBlankValueIsTreatedAsAbsent() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    favicon: ""
                """);

        assertEquals(MotdSettings.Favicon.CACHED, settings.getOffline().favicon());
        assertTrue(settings.getWarnings().isEmpty(), "Une valeur vide n'est pas une erreur");
    }

    // ===================== Résolution par état =====================

    @Test
    public void testForStatusMapsToTheRightSection() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    hover: none
                  starting:
                    hover: proxy
                """);

        assertEquals(MotdSettings.Hover.NONE,
                settings.forStatus(MinecraftServerStatus.OFFLINE).hover());
        assertEquals(MotdSettings.Hover.PROXY,
                settings.forStatus(MinecraftServerStatus.STARTING).hover());
    }

    /**
     * Un serveur en ligne affiche son propre ping : ces réglages ne le
     * concernent pas.
     */
    @Test
    public void testOnlineHasNoSettings() {
        assertNull(MotdSettings.defaults().forStatus(MinecraftServerStatus.ONLINE));
    }

    // ===================== Combinaisons documentées =====================

    /**
     * La combinaison annoncée dans config.yml pour retrouver le rendu de la 2.x
     * doit effectivement produire ces réglages.
     */
    @Test
    public void testDocumentedLegacyPresetParses() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    description: plugin
                    favicon: bundled
                    version-label: text
                  starting:
                    description: plugin
                    favicon: bundled
                    version-label: text
                """);

        for (MotdSettings.State state : new MotdSettings.State[]{settings.getOffline(), settings.getStarting()}) {
            assertEquals(MotdSettings.Description.PLUGIN, state.description());
            assertEquals(MotdSettings.Favicon.BUNDLED, state.favicon());
            assertEquals(MotdSettings.VersionLabel.TEXT, state.versionLabel());
        }
        assertTrue(settings.getWarnings().isEmpty());
    }

    /**
     * Le {@code config.yml} livré doit produire exactement les défauts codés en
     * dur. Sans ce test, les deux peuvent diverger silencieusement : le fichier
     * annoncerait un comportement que le code n'applique pas.
     */
    @Test
    public void testShippedConfigYieldsTheCodedDefaults() throws Exception {
        Configuration shipped;
        try (java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(in, "config.yml doit etre present dans les ressources");
            shipped = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        }

        MotdSettings fromFile = MotdSettings.from(shipped);
        MotdSettings coded = MotdSettings.defaults();

        assertEquals(coded.getOffline(), fromFile.getOffline(),
                "La section motd.offline livree doit refleter les defauts du code");
        assertEquals(coded.getStarting(), fromFile.getStarting(),
                "La section motd.starting livree doit refleter les defauts du code");
        assertTrue(fromFile.getWarnings().isEmpty(),
                "Le fichier livre ne doit contenir aucune valeur invalide : " + fromFile.getWarnings());
    }

    /**
     * La combinaison « ne rien changer » doit laisser chaque champ à PROXY.
     */
    @Test
    public void testDocumentedPassthroughPresetParses() {
        MotdSettings settings = parse("""
                motd:
                  offline:
                    description: proxy
                    favicon: proxy
                    players: proxy
                    max-players: proxy
                    hover: proxy
                    version-label: none
                """);

        MotdSettings.State offline = settings.getOffline();
        assertEquals(MotdSettings.Description.PROXY, offline.description());
        assertEquals(MotdSettings.Favicon.PROXY, offline.favicon());
        assertEquals(MotdSettings.Players.PROXY, offline.players());
        assertEquals(MotdSettings.MaxPlayers.PROXY, offline.maxPlayers());
        assertEquals(MotdSettings.Hover.PROXY, offline.hover());
        assertTrue(settings.getWarnings().isEmpty());
    }
}
