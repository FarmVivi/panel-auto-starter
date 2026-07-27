package fr.farmvivi.panelautostarter.motd;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la persistance du MOTD sur disque.
 */
public class MotdStoreTest {

    @TempDir
    Path dataFolder;

    private MotdStore store() {
        return new MotdStore(dataFolder.toFile());
    }

    private Path cacheFile(String name) {
        return dataFolder.resolve("cache").resolve(name);
    }

    // ===================== Aller-retour =====================

    /**
     * Le point central : un MOTD réaliste, avec couleurs hexadécimales et
     * décorations, doit revenir <strong>identique</strong>. C'est ce qui a
     * motivé le format JSON d'Adventure plutôt que le format legacy, lequel
     * perdrait les couleurs hors palette.
     */
    @Test
    public void testDescriptionRoundTripsLosslessly() throws IOException {
        Component description = Component.text("Scipio", TextColor.color(0x4CC9F0))
                .decorate(TextDecoration.BOLD)
                .append(Component.text(" Survie", TextColor.color(0x7B2FF7)))
                .appendNewline()
                .append(Component.text("Entre potes", TextColor.color(0xFFD166)));

        store().save("lobby", new CachedMotd(description, null, 0));
        CachedMotd loaded = store().load("lobby");

        assertEquals(description, loaded.description(),
                "La description doit revenir a l'identique, couleurs comprises");
    }

    @Test
    public void testFaviconRoundTrips() throws IOException {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

        store().save("lobby", new CachedMotd(null, png, 0));

        assertArrayEquals(png, store().load("lobby").faviconPng());
    }

    @Test
    public void testWritesOneFilePairPerServer() throws IOException {
        store().save("lobby", new CachedMotd(Component.text("A"), new byte[]{1}, 0));
        store().save("survie", new CachedMotd(Component.text("B"), new byte[]{2}, 0));

        assertTrue(Files.exists(cacheFile("lobby.json")));
        assertTrue(Files.exists(cacheFile("lobby.png")));
        assertTrue(Files.exists(cacheFile("survie.json")));
        assertEquals(Component.text("A"), store().load("lobby").description());
        assertEquals(Component.text("B"), store().load("survie").description());
    }

    // ===================== Absence et corruption =====================

    @Test
    public void testUnknownServerYieldsEmptyMotd() {
        CachedMotd loaded = store().load("jamais-vu");

        assertTrue(loaded.isEmpty(), "Un serveur inconnu ne doit rien renvoyer");
        assertNull(loaded.description());
        assertNull(loaded.faviconPng());
    }

    /**
     * Un fichier de cache abîmé — édition manuelle, disque plein, coupure en
     * cours d'écriture — ne doit jamais empêcher le plugin de démarrer.
     */
    @Test
    public void testCorruptedDescriptionIsIgnoredRatherThanThrowing() throws IOException {
        Files.createDirectories(cacheFile("lobby.json").getParent());
        Files.writeString(cacheFile("lobby.json"), "{ ceci n'est pas du json", StandardCharsets.UTF_8);

        CachedMotd loaded = assertDoesNotThrow(() -> store().load("lobby"));

        assertNull(loaded.description(), "Un cache illisible doit etre traite comme absent");
    }

    @Test
    public void testSavingOnlyDescriptionKeepsFaviconAbsent() throws IOException {
        store().save("lobby", new CachedMotd(Component.text("Coucou"), null, 0));

        assertFalse(Files.exists(cacheFile("lobby.png")));
        assertNull(store().load("lobby").faviconPng());
    }

    @Test
    public void testCacheDirectoryIsCreatedOnDemand() throws IOException {
        assertFalse(Files.exists(dataFolder.resolve("cache")));

        store().save("lobby", new CachedMotd(Component.text("Coucou"), null, 0));

        assertTrue(Files.isDirectory(dataFolder.resolve("cache")));
    }

    // ===================== Noms de serveur hostiles =====================

    /**
     * Les noms de serveur viennent de la configuration du proxy. Un nom
     * contenant un séparateur de chemin ne doit pas permettre d'écrire hors du
     * dossier de cache.
     */
    @Test
    public void testServerNameCannotEscapeCacheDirectory() throws IOException {
        store().save("../../evil", new CachedMotd(Component.text("x"), null, 0));

        try (var files = Files.walk(dataFolder)) {
            assertTrue(files.filter(Files::isRegularFile)
                            .allMatch(p -> p.startsWith(dataFolder.resolve("cache"))),
                    "Aucun fichier ne doit etre ecrit hors du dossier cache");
        }
    }

    @Test
    public void testSanitizeReplacesPathSeparators() {
        assertFalse(MotdStore.sanitize("../evil").contains("/"));
        assertFalse(MotdStore.sanitize("a\\b").contains("\\"));
        assertEquals("streetland_survie", MotdStore.sanitize("streetland_survie"));
        assertEquals("_", MotdStore.sanitize(""));
        assertEquals("_", MotdStore.sanitize(null));
    }

    @Test
    public void testDistinctNamesDoNotCollideAfterSanitizing() throws IOException {
        // Deux noms differents restent distincts tant qu'ils ne different pas
        // uniquement par des caracteres remplaces.
        store().save("lobby-1", new CachedMotd(Component.text("un"), null, 0));
        store().save("lobby.2", new CachedMotd(Component.text("deux"), null, 0));

        assertEquals(Component.text("un"), store().load("lobby-1").description());
        assertEquals(Component.text("deux"), store().load("lobby.2").description());
    }

    @Test
    public void testStoreUsesCacheSubfolderOfDataFolder() throws IOException {
        store().save("lobby", new CachedMotd(Component.text("x"), null, 0));

        assertTrue(Files.exists(new File(dataFolder.toFile(), "cache/lobby.json").toPath()),
                "Le cache doit vivre dans <dossier du plugin>/cache/");
    }
}
