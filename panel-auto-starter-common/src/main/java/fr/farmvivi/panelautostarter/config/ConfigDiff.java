package fr.farmvivi.panelautostarter.config;

import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compare la configuration de l'utilisateur à celle livrée avec le plugin.
 * <p>
 * Le fichier n'est copié qu'à la première installation : une mise à jour qui
 * ajoute des réglages ne les fait donc jamais apparaître chez les utilisateurs
 * existants. Ils héritent silencieusement des valeurs par défaut sans savoir que
 * l'option existe — et cherchent le réglage dans un fichier qui ne le contient
 * pas.
 * <p>
 * Ce comparateur relève les clés manquantes pour pouvoir les signaler au
 * démarrage. Il ne touche jamais au fichier : réécrire la configuration d'un
 * administrateur reviendrait à écraser ses choix, commentaires compris.
 */
public final class ConfigDiff {
    /**
     * Sections décrivant les serveurs de l'utilisateur, et non des réglages.
     * L'exemple livré n'a rien à dire sur ce qu'il devrait y déclarer.
     */
    private static final String SERVERS_SECTION = "servers";

    private ConfigDiff() {
    }

    /**
     * Retourne les réglages présents dans la configuration livrée mais absents
     * de celle de l'utilisateur.
     *
     * @param user    la configuration de l'utilisateur
     * @param bundled la configuration livrée avec le plugin
     * @return les chemins manquants, dans l'ordre du fichier livré
     */
    public static List<String> missingKeys(Configuration user, Configuration bundled) {
        if (user == null || bundled == null) {
            return Collections.emptyList();
        }
        List<String> missing = new ArrayList<>();
        collect(user, bundled, "", missing);
        return missing;
    }

    private static void collect(Configuration user, Configuration section, String prefix, List<String> missing) {
        for (String key : section.getKeys()) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (path.equals(SERVERS_SECTION)) {
                continue;
            }

            Object value = section.get(key);
            if (value instanceof Configuration nested) {
                collect(user, nested, path, missing);
            } else if (!user.contains(path)) {
                missing.add(path);
            }
        }
    }
}
