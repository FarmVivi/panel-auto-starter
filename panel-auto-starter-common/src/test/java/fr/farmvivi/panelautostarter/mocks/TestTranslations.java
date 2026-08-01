package fr.farmvivi.panelautostarter.mocks;

import fr.farmvivi.panelautostarter.i18n.Translations;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Installe les traductions une fois pour toutes les suites de tests.
 * <p>
 * Sans cela, les messages resteraient à l'état de clés et les tests
 * n'assureraient plus que la forme des composants. Les faire traduire a un
 * effet secondaire précieux : toute clé employée par le code sans exister dans
 * le fichier de langue se voit immédiatement, puisque le texte attendu ne sort
 * pas.
 */
public final class TestTranslations {

    /** Langue des assertions : celle dans laquelle les tests sont écrits. */
    public static final Locale LOCALE = Locale.FRENCH;

    private static boolean installed;

    private TestTranslations() {
    }

    /**
     * Rend un composant dans la langue des tests.
     * <p>
     * À appliquer à ce qu'un test <em>attend</em> quand il le compare à ce
     * qu'un joueur a reçu : celui-ci a déjà été traduit à la réception, le
     * comparer à une clé non résolue échouerait toujours.
     *
     * @param component le composant attendu
     * @return sa forme traduite
     */
    public static net.kyori.adventure.text.Component render(
            net.kyori.adventure.text.Component component) {
        ensureInstalled();
        return Translations.render(component, LOCALE);
    }

    /**
     * Installe les traductions si ce n'est pas déjà fait.
     */
    public static synchronized void ensureInstalled() {
        if (installed) {
            return;
        }
        installed = true;
        doInstall();
    }

    /**
     * Réinstalle les traductions complètes.
     * <p>
     * À appeler par toute suite qui installe un jeu partiel : l'enregistrement
     * étant global, elle priverait sinon les suites suivantes de leurs textes,
     * avec un échec dépendant de l'ordre d'exécution — le pire à diagnostiquer.
     */
    public static synchronized void reinstall() {
        installed = true;
        doInstall();
    }

    private static void doInstall() {
        try {
            // Un dossier jetable : l'installation y recopie les fichiers
            // embarques, ce qui n'a pas a polluer le depot.
            Translations.install(Files.createTempDirectory("pas-lang").toFile(), LOCALE,
                    message -> {
                    });
        } catch (IOException ex) {
            throw new IllegalStateException("Traductions non installables pour les tests", ex);
        }
    }
}
