package fr.farmvivi.panelautostarter.i18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Charge les traductions et les branche sur le mécanisme d'Adventure.
 * <p>
 * Le choix de passer par {@link GlobalTranslator} plutôt que par un système
 * maison n'est pas cosmétique : il permet à un composant de rester
 * <em>non traduit</em> jusqu'au moment où il est remis à quelqu'un, et d'être
 * alors rendu dans <em>sa</em> langue. Deux joueurs d'un même serveur lisent
 * donc chacun la sienne, ce qu'un message traduit à la construction ne
 * permettrait pas.
 * <p>
 * Les fichiers embarqués sont recopiés dans {@code lang/} au premier
 * démarrage, et ce qui s'y trouve prime. La superposition est faite clé par
 * clé : un administrateur qui ne traduit que la moitié d'un fichier garde le
 * texte d'origine pour l'autre moitié, au lieu de se retrouver avec des clés
 * brutes à l'écran.
 */
public final class Translations {

    /**
     * Identité du magasin. Adventure s'en sert pour distinguer nos traductions
     * de celles des autres plugins.
     */
    private static final Key STORE_KEY = Key.key("panelautostarter", "messages");

    /** Dossier des fichiers de langue, dans le dossier de données du plugin. */
    public static final String LANG_FOLDER = "lang";

    private static final String BASE_NAME = "messages";
    private static final String EXTENSION = ".properties";

    /**
     * Langues fournies avec le plugin. La première est la langue de repli :
     * celle qu'un joueur reçoit quand la sienne n'est pas traduite.
     */
    private static final List<Locale> BUNDLED = List.of(Locale.ENGLISH, Locale.FRENCH);

    /**
     * Magasin actuellement enregistré, s'il y en a un.
     * <p>
     * {@link GlobalTranslator} est un registre global et cumulatif : une
     * seconde installation viendrait s'ajouter à la première, et c'est la plus
     * ancienne qui répondrait — une modification des fichiers suivie d'un
     * rechargement resterait donc sans effet. On retire l'ancien avant de poser
     * le nouveau.
     */
    private static TranslationStore<MessageFormat> installed;

    /**
     * Langue de repli, retenue pour ce qui s'adresse à personne en particulier.
     */
    private static Locale fallback = Locale.getDefault();

    private Translations() {
    }

    /**
     * Charge les traductions et les enregistre auprès d'Adventure.
     *
     * @param dataFolder    le dossier de données du plugin
     * @param defaultLocale la langue servie à qui n'en a pas de traduite
     * @param warningSink   où signaler un fichier illisible
     */
    public static synchronized void install(File dataFolder, Locale defaultLocale,
                                            Consumer<String> warningSink) {
        if (installed != null) {
            GlobalTranslator.translator().removeSource(installed);
            installed = null;
        }

        TranslationStore.StringBased<MessageFormat> store =
                TranslationStore.messageFormat(STORE_KEY);
        store.defaultLocale(defaultLocale);
        fallback = defaultLocale;

        File langFolder = new File(dataFolder, LANG_FOLDER);
        copyBundledFiles(langFolder, warningSink);

        for (Locale locale : localesToLoad(langFolder)) {
            Map<String, String> merged = new LinkedHashMap<>();
            // L'embarque d'abord, le fichier de l'administrateur par-dessus :
            // une traduction partielle complete l'originale au lieu de la
            // remplacer par des trous.
            merged.putAll(readBundled(locale, warningSink));
            merged.putAll(readFromDisk(langFolder, locale, warningSink));

            if (!merged.isEmpty()) {
                store.registerAll(locale, toMessageFormats(merged, locale));
            }
        }

        GlobalTranslator.translator().addSource(store);
        installed = store;
    }

    /**
     * Traduit un composant dans une langue donnée.
     * <p>
     * Nécessaire là où le proxy ne le fait pas lui-même — BungeeCord ignore
     * Adventure — et sans effet sur un composant déjà traduit.
     *
     * @param component le composant
     * @param locale    la langue, éventuellement null
     * @return le composant traduit
     */
    public static Component render(Component component, Locale locale) {
        if (component == null) {
            return null;
        }
        return GlobalTranslator.render(component, locale != null ? locale : Locale.getDefault());
    }

    /**
     * Traduit dans la langue de repli.
     * <p>
     * Pour ce qui s'adresse à personne en particulier : un MOTD est composé
     * pour une requête de liste de serveurs, qui ne dit rien de la langue de
     * celui qui l'a émise — le client ne l'annonce qu'une fois connecté. Il n'y
     * a donc pas de meilleure réponse que la langue configurée.
     *
     * @param component le composant
     * @return le composant traduit
     */
    public static Component renderDefault(Component component) {
        return render(component, fallback);
    }

    // ===================== Chargement =====================

    private static List<Locale> localesToLoad(File langFolder) {
        List<Locale> locales = new ArrayList<>(BUNDLED);

        // Un administrateur peut deposer une langue que le plugin ne fournit
        // pas : elle est prise sans qu'il ait rien d'autre a faire.
        File[] files = langFolder.listFiles((dir, name) ->
                name.startsWith(BASE_NAME) && name.endsWith(EXTENSION));
        if (files == null) {
            return locales;
        }
        for (File file : files) {
            Locale locale = localeOf(file.getName());
            if (locale != null && !locales.contains(locale)) {
                locales.add(locale);
            }
        }
        return locales;
    }

    /**
     * Déduit la langue du nom d'un fichier : {@code messages_fr.properties}
     * donne le français, {@code messages.properties} la langue de repli.
     */
    private static Locale localeOf(String fileName) {
        String core = fileName.substring(0, fileName.length() - EXTENSION.length());
        if (core.equals(BASE_NAME)) {
            return BUNDLED.get(0);
        }
        if (!core.startsWith(BASE_NAME + "_")) {
            return null;
        }
        String tag = core.substring(BASE_NAME.length() + 1).replace('_', '-');
        Locale locale = Locale.forLanguageTag(tag);
        return locale.getLanguage().isEmpty() ? null : locale;
    }

    private static String fileNameFor(Locale locale) {
        return locale.equals(BUNDLED.get(0))
                ? BASE_NAME + EXTENSION
                : BASE_NAME + "_" + locale.toLanguageTag().replace('-', '_') + EXTENSION;
    }

    private static void copyBundledFiles(File langFolder, Consumer<String> warningSink) {
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            warningSink.accept("Impossible de creer " + langFolder
                    + " : les traductions ne seront pas modifiables.");
            return;
        }

        for (Locale locale : BUNDLED) {
            File target = new File(langFolder, fileNameFor(locale));
            if (target.exists()) {
                // Ne jamais ecraser : le fichier appartient a l'administrateur
                // des lors qu'il existe.
                continue;
            }
            try (InputStream in = open(locale)) {
                if (in == null) {
                    continue;
                }
                Files.copy(in, target.toPath());
            } catch (IOException ex) {
                warningSink.accept("Copie de " + target.getName() + " impossible : "
                        + ex.getMessage());
            }
        }
    }

    private static InputStream open(Locale locale) {
        return Translations.class.getClassLoader()
                .getResourceAsStream(LANG_FOLDER + "/" + fileNameFor(locale));
    }

    private static Map<String, String> readBundled(Locale locale, Consumer<String> warningSink) {
        try (InputStream in = open(locale)) {
            if (in == null) {
                return Map.of();
            }
            return read(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            warningSink.accept("Lecture des traductions embarquees (" + locale
                    + ") impossible : " + ex.getMessage());
            return Map.of();
        }
    }

    private static Map<String, String> readFromDisk(File langFolder, Locale locale,
                                                    Consumer<String> warningSink) {
        Path path = new File(langFolder, fileNameFor(locale)).toPath();
        if (!Files.isReadable(path)) {
            return Map.of();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader);
        } catch (IOException ex) {
            // Un fichier illisible ne doit pas priver le joueur de tout texte :
            // l'embarque reste en place, et on le dit.
            warningSink.accept(path.getFileName() + " est illisible (" + ex.getMessage()
                    + ") : les traductions embarquees sont utilisees a la place.");
            return Map.of();
        }
    }

    private static Map<String, String> read(Reader reader) throws IOException {
        // Les fichiers sont lus en UTF-8 et non dans l'encodage historique des
        // proprietes : les accents et les caracteres non latins doivent
        // s'ecrire tels quels, pas en sequences d'echappement.
        Properties properties = new Properties();
        properties.load(reader);

        Map<String, String> values = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            values.put(key, properties.getProperty(key));
        }
        return values;
    }

    private static Map<String, MessageFormat> toMessageFormats(Map<String, String> values,
                                                               Locale locale) {
        Map<String, MessageFormat> formats = new HashMap<>(values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            formats.put(entry.getKey(), new MessageFormat(escapeQuotes(entry.getValue()), locale));
        }
        return formats;
    }

    /**
     * Double les apostrophes avant de confier le texte à {@link MessageFormat}.
     * <p>
     * Sans cela, il les interprète comme des délimiteurs et les avale : « file
     * d'attente » devient « file dattente », et « l'{0} » perd son paramètre.
     * Le piège est d'autant plus vicieux qu'il ne se voit qu'en français, où
     * les apostrophes abondent.
     */
    private static String escapeQuotes(String value) {
        return value.replace("'", "''");
    }
}
