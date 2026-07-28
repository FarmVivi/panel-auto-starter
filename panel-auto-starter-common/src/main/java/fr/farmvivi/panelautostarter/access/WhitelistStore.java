package fr.farmvivi.panelautostarter.access;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Conserve les listes blanches dans {@code whitelist.yml}.
 * <p>
 * Volontairement séparé de {@code config.yml}, que le plugin ne réécrit jamais :
 * les listes blanches se modifient en jeu, et réécrire le fichier de
 * configuration à chaque ajout en effacerait les commentaires et la mise en
 * forme — tout ce qui le rend administrable à la main.
 */
public final class WhitelistStore {

    private static final String ENABLED = "enabled";
    private static final String PLAYERS = "players";

    private final File file;
    private final Consumer<String> warningSink;
    private final Map<String, Whitelist> whitelists = new ConcurrentHashMap<>();

    /**
     * @param dataFolder  le dossier de données du plugin
     * @param warningSink où signaler un fichier illisible
     */
    public WhitelistStore(File dataFolder, Consumer<String> warningSink) {
        this.file = new File(dataFolder, "whitelist.yml");
        this.warningSink = warningSink;
        load();
    }

    /**
     * Retourne la liste blanche d'un serveur, en la créant vide si elle
     * n'existe pas encore.
     *
     * @param serverName le nom du serveur
     * @return sa liste blanche
     */
    public Whitelist get(String serverName) {
        return whitelists.computeIfAbsent(serverName, Whitelist::new);
    }

    /**
     * Retourne toutes les listes blanches connues.
     *
     * @return les listes blanches
     */
    public Collection<Whitelist> getAll() {
        return whitelists.values();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        Configuration config;
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException ex) {
            // Un fichier illisible ne doit pas empecher le plugin de demarrer,
            // mais il ne doit pas non plus ouvrir en grand des serveurs qu'il
            // etait cense proteger : on le signale fort.
            warningSink.accept("whitelist.yml est illisible (" + ex.getMessage()
                    + ") : les listes blanches qu'il contenait ne sont PAS appliquees.");
            return;
        }
        if (config == null) {
            return;
        }

        for (String serverName : config.getKeys()) {
            Configuration section = config.getSection(serverName);
            if (section == null) {
                continue;
            }
            Whitelist whitelist = new Whitelist(serverName, section.getBoolean(ENABLED, false));

            Configuration players = section.getSection(PLAYERS);
            if (players != null) {
                for (String rawUuid : players.getKeys()) {
                    try {
                        whitelist.add(UUID.fromString(rawUuid), players.getString(rawUuid));
                    } catch (IllegalArgumentException ex) {
                        warningSink.accept("whitelist.yml : « " + rawUuid + " » n'est pas un "
                                + "identifiant de joueur valide, entree ignoree pour "
                                + serverName + ".");
                    }
                }
            }

            whitelists.put(serverName, whitelist);
        }
    }

    /**
     * Écrit les listes blanches sur disque.
     *
     * @return true si l'écriture a abouti
     */
    public synchronized boolean save() {
        Configuration config = new Configuration();

        for (Whitelist whitelist : whitelists.values()) {
            String base = whitelist.getServerName();
            config.set(base + "." + ENABLED, whitelist.isEnabled());
            for (Map.Entry<UUID, String> entry : whitelist.getPlayers().entrySet()) {
                config.set(base + "." + PLAYERS + "." + entry.getKey(), entry.getValue());
            }
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                warningSink.accept("Impossible de creer " + parent + " : whitelist.yml n'a pas ete ecrit.");
                return false;
            }
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
            return true;
        } catch (IOException ex) {
            warningSink.accept("Ecriture de whitelist.yml impossible : " + ex.getMessage());
            return false;
        }
    }

    /**
     * Retourne le fichier sous-jacent.
     *
     * @return le fichier whitelist.yml
     */
    public File getFile() {
        return file;
    }
}
