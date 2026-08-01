package fr.farmvivi.panelautostarter.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Garde les fichiers de langue alignés sur le code.
 * <p>
 * C'est ici que la régression serait silencieuse : une clé employée par le code
 * mais absente d'un fichier ne se voit qu'à l'écran d'un joueur, sous la forme
 * d'un identifiant brut, et seulement dans la langue oubliée. Un test le dit
 * avant la mise en production.
 */
public class TranslationsTest {

    /** Fichiers livrés avec le plugin. */
    private static final List<String> BUNDLES =
            List.of("lang/messages.properties", "lang/messages_fr.properties");

    /**
     * Les clés telles qu'elles apparaissent dans le code : le préfixe est
     * toujours écrit en clair, ce qui les rend repérables.
     */
    private static final Pattern KEY_IN_CODE =
            Pattern.compile("translatable\\(\\s*\"(panelautostarter\\.[a-z0-9._]+)\"");

    /** Concaténations de la forme {@code KEY + "suffixe"}. */
    private static final Pattern SUFFIX_IN_CODE = Pattern.compile(
            // Le prefixe lui-meme n'est pas un suffixe : sans cette exclusion,
            // « translatable("panelautostarter." + key » se recomposerait en une
            // cle doublee.
            "(?:text|translatable)\\(\\s*(?:KEY\\s*\\+\\s*)?\"(?!panelautostarter)"
                    + "([a-z0-9][a-z0-9._]*)\"");

    private static Properties load(String resource) throws IOException {
        try (InputStream in = TranslationsTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(in, "Fichier de langue absent du jar : " + resource);
            Properties properties = new Properties();
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return properties;
        }
    }

    /**
     * Toute langue livrée doit couvrir exactement les mêmes clés : une clé
     * présente d'un côté et pas de l'autre laisse un identifiant brut à
     * l'écran.
     */
    @Test
    public void testEveryBundleCoversTheSameKeys() throws IOException {
        Set<String> reference = new TreeSet<>(load(BUNDLES.get(0)).stringPropertyNames());

        for (String bundle : BUNDLES.subList(1, BUNDLES.size())) {
            Set<String> keys = new TreeSet<>(load(bundle).stringPropertyNames());

            Set<String> missing = new TreeSet<>(reference);
            missing.removeAll(keys);
            assertTrue(missing.isEmpty(), bundle + " : clés manquantes " + missing);

            Set<String> extra = new TreeSet<>(keys);
            extra.removeAll(reference);
            assertTrue(extra.isEmpty(), bundle + " : clés en trop " + extra);
        }
    }

    /**
     * Aucune traduction ne doit être vide : une valeur absente affiche une
     * ligne blanche, ce qui se remarque moins qu'un identifiant et se
     * diagnostique donc plus mal.
     */
    @Test
    public void testNoTranslationIsEmpty() throws IOException {
        for (String bundle : BUNDLES) {
            Properties properties = load(bundle);
            for (String key : properties.stringPropertyNames()) {
                assertFalse(properties.getProperty(key).isBlank(),
                        bundle + " : « " + key + " » est vide");
            }
        }
    }

    /**
     * Les paramètres doivent survivre à la traduction : en perdre un fait
     * disparaître une information — le nom du serveur, le plus souvent.
     */
    @Test
    public void testEveryBundleKeepsThePlaceholders() throws IOException {
        Properties reference = load(BUNDLES.get(0));

        for (String bundle : BUNDLES.subList(1, BUNDLES.size())) {
            Properties properties = load(bundle);
            for (String key : reference.stringPropertyNames()) {
                assertEquals(placeholders(reference.getProperty(key)),
                        placeholders(properties.getProperty(key)),
                        bundle + " : « " + key + " » n'a pas les mêmes paramètres");
            }
        }
    }

    private static Set<String> placeholders(String value) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\{(\\d+)[^}]*}").matcher(value);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    /**
     * Le vrai garde-fou : toute clé écrite dans le code doit exister dans les
     * fichiers. Sans lui, ajouter un message sans sa traduction ne se voit
     * qu'en jeu.
     */
    @Test
    public void testEveryKeyUsedInCodeExists() throws IOException {
        Set<String> declared = load(BUNDLES.get(0)).stringPropertyNames();
        Set<String> used = keysUsedInSources();

        assertFalse(used.isEmpty(), "Aucune clé trouvée : le test ne vérifierait rien");

        Set<String> missing = new TreeSet<>(used);
        missing.removeAll(declared);
        assertTrue(missing.isEmpty(), "Clés employées par le code mais absentes des "
                + "fichiers de langue : " + missing);
    }

    /**
     * Relit les sources pour y trouver les clés. Un peu fruste, mais c'est le
     * seul moyen de voir une clé qui n'est jamais atteinte par un test.
     */
    private static Set<String> keysUsedInSources() throws IOException {
        Path sources = Path.of("src", "main", "java");
        Set<String> keys = new TreeSet<>();

        try (var files = Files.walk(sources)) {
            List<Path> javaFiles = new ArrayList<>();
            files.filter(path -> path.toString().endsWith(".java")).forEach(javaFiles::add);

            for (Path file : javaFiles) {
                String content = Files.readString(file, StandardCharsets.UTF_8);

                Matcher full = KEY_IN_CODE.matcher(content);
                while (full.find()) {
                    keys.add(full.group(1));
                }

                // Les classes de messages ecrivent « KEY + "suffixe" » : le
                // prefixe est alors une constante, a recomposer.
                String prefix = prefixOf(content);
                if (prefix != null) {
                    Matcher suffix = SUFFIX_IN_CODE.matcher(content);
                    while (suffix.find()) {
                        keys.add(prefix + suffix.group(1));
                    }
                }
            }
        }
        return keys;
    }

    private static String prefixOf(String content) {
        // Deux formes coexistent : une constante KEY dans les classes de
        // messages, un préfixe écrit dans l'aide locale des menus.
        Matcher constant = Pattern.compile(
                "String KEY = \"(panelautostarter\\.[a-z.]*)\"").matcher(content);
        if (constant.find()) {
            return constant.group(1);
        }
        Matcher inlined = Pattern.compile(
                "translatable\\(\"(panelautostarter\\.)\" \\+").matcher(content);
        return inlined.find() ? inlined.group(1) : null;
    }

    // ===================== Chargement =====================

    @Test
    public void testTheBundledFilesAreCopiedForEditing(@TempDir Path dataFolder) {
        Translations.install(dataFolder.toFile(), Locale.ENGLISH, message -> {
        });

        Path lang = dataFolder.resolve(Translations.LANG_FOLDER);
        assertTrue(Files.isReadable(lang.resolve("messages.properties")));
        assertTrue(Files.isReadable(lang.resolve("messages_fr.properties")));
    }

    /**
     * Le fichier de l'administrateur prime, clé par clé : traduire la moitié
     * d'un fichier ne doit pas faire perdre l'autre moitié.
     */
    @Test
    public void testAPartialOverrideKeepsTheRest(@TempDir Path dataFolder) throws IOException {
        Path lang = Files.createDirectories(dataFolder.resolve(Translations.LANG_FOLDER));
        Files.writeString(lang.resolve("messages_fr.properties"),
                "panelautostarter.title.ready=Allez !\n", StandardCharsets.UTF_8);

        Translations.install(dataFolder.toFile(), Locale.ENGLISH, message -> {
        });

        assertEquals("Allez !", plain(Component.translatable("panelautostarter.title.ready")),
                "La valeur de l'administrateur doit primer");
        assertEquals("Démarrage", plain(Component.translatable("panelautostarter.title.starting")),
                "Le reste doit rester traduit");
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText()
                .serialize(Translations.render(component, Locale.FRENCH));
    }

    /**
     * Les apostrophes sont partout en français ; {@link java.text.MessageFormat}
     * les avale si elles ne sont pas doublées.
     */
    @Test
    public void testApostrophesSurvive(@TempDir Path dataFolder) {
        Translations.install(dataFolder.toFile(), Locale.ENGLISH, message -> {
        });

        String text = plain(Component.translatable("panelautostarter.queue.position",
                Component.text("2/5")));

        assertTrue(text.contains("d'attente"), "L'apostrophe doit survivre : " + text);
        assertTrue(text.contains("2/5"), "Et le parametre aussi : " + text);
    }
}
