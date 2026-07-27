package fr.farmvivi.panelautostarter.motd;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persistance du MOTD et du favicon des serveurs, dans le dossier du plugin.
 * <p>
 * Deux fichiers par serveur dans {@code cache/} : {@code <serveur>.json} pour la
 * description et {@code <serveur>.png} pour le favicon. Les supprimer force le
 * plugin à repartir de zéro pour ce serveur, sans toucher aux autres.
 * <p>
 * La description est stockée au format JSON d'Adventure, celui-là même qui
 * transite sur le réseau : le fichier est donc du JSON valide, sans conteneur
 * superflu, et le format legacy — qui perdrait les dégradés et les couleurs
 * hexadécimales — est évité.
 * <p>
 * Écrire ce cache sur disque plutôt qu'en mémoire a un intérêt précis : après un
 * redémarrage du proxy avec tous les serveurs éteints, les MOTD sont corrects
 * immédiatement au lieu d'être vides jusqu'à la première mise en ligne.
 */
public class MotdStore {
    private static final String CACHE_DIRECTORY = "cache";
    private static final String DESCRIPTION_SUFFIX = ".json";
    private static final String FAVICON_SUFFIX = ".png";
    private static final String DESCRIPTION_KEY = "description";
    private static final String MAX_PLAYERS_KEY = "max-players";

    private final File cacheDirectory;

    public MotdStore(File dataFolder) {
        this.cacheDirectory = new File(dataFolder, CACHE_DIRECTORY);
    }

    /**
     * Charge le MOTD conservé pour un serveur.
     *
     * @param serverName le nom du serveur
     * @return le MOTD, jamais null, mais éventuellement vide
     */
    public CachedMotd load(String serverName) {
        JsonObject root = readJson(serverName);
        return new CachedMotd(readDescription(root), readFavicon(serverName), readMaxPlayers(root));
    }

    /**
     * Enregistre le MOTD d'un serveur.
     *
     * @param serverName le nom du serveur
     * @param motd       le MOTD à conserver
     * @throws IOException si l'écriture échoue
     */
    public void save(String serverName, CachedMotd motd) throws IOException {
        Files.createDirectories(cacheDirectory.toPath());

        if (motd.description() != null || motd.maxPlayers() > 0) {
            JsonObject root = new JsonObject();
            if (motd.description() != null) {
                root.add(DESCRIPTION_KEY, GsonComponentSerializer.gson().serializeToTree(motd.description()));
            }
            root.addProperty(MAX_PLAYERS_KEY, motd.maxPlayers());
            Files.writeString(descriptionPath(serverName), root.toString(), StandardCharsets.UTF_8);
        }
        if (motd.faviconPng() != null) {
            Files.write(faviconPath(serverName), motd.faviconPng());
        }
    }

    private JsonObject readJson(String serverName) {
        Path path = descriptionPath(serverName);
        if (!Files.isReadable(path)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (IOException | RuntimeException ex) {
            // Un cache abime ne doit jamais empecher le plugin de demarrer :
            // on repart simplement sans MOTD conserve pour ce serveur.
            return null;
        }
    }

    private Component readDescription(JsonObject root) {
        if (root == null) {
            return null;
        }
        try {
            // Les premieres versions du cache ecrivaient le composant seul, sans
            // conteneur. On le reconnait a l'absence de la cle "description".
            JsonElement description = root.has(DESCRIPTION_KEY) ? root.get(DESCRIPTION_KEY) : root;
            return GsonComponentSerializer.gson().deserializeFromTree(description);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private int readMaxPlayers(JsonObject root) {
        if (root == null || !root.has(MAX_PLAYERS_KEY)) {
            return 0;
        }
        try {
            return Math.max(0, root.get(MAX_PLAYERS_KEY).getAsInt());
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private byte[] readFavicon(String serverName) {
        Path path = faviconPath(serverName);
        if (!Files.isReadable(path)) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            return null;
        }
    }

    private Path descriptionPath(String serverName) {
        return new File(cacheDirectory, sanitize(serverName) + DESCRIPTION_SUFFIX).toPath();
    }

    private Path faviconPath(String serverName) {
        return new File(cacheDirectory, sanitize(serverName) + FAVICON_SUFFIX).toPath();
    }

    /**
     * Neutralise les caractères qu'un nom de serveur pourrait contenir et qui
     * n'ont rien à faire dans un nom de fichier. Sans cela, un nom contenant
     * {@code ../} permettrait d'écrire hors du dossier de cache.
     *
     * @param serverName le nom du serveur
     * @return un nom de fichier sûr
     */
    static String sanitize(String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return "_";
        }
        return serverName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
