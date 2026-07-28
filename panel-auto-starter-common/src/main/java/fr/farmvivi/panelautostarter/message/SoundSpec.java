package fr.farmvivi.panelautostarter.message;

import net.md_5.bungee.config.Configuration;

/**
 * Un son à jouer, avec son volume et sa hauteur.
 * <p>
 * Le volume compte autant que le choix du son : un son juste mais joué à plein
 * volume est agressif, surtout répété à chaque seconde d'un décompte.
 *
 * @param name   l'identifiant du son, vide pour n'en jouer aucun
 * @param volume le volume
 * @param pitch  la hauteur
 */
public record SoundSpec(String name, float volume, float pitch) {
    /**
     * Aucun son.
     */
    public static final SoundSpec SILENT = new SoundSpec("", 1f, 1f);

    /**
     * Indique si ce réglage ne produit aucun son.
     *
     * @return true si aucun son n'est à jouer
     */
    public boolean isSilent() {
        return name == null || name.isBlank();
    }

    /**
     * Lit un son depuis une section de configuration.
     *
     * @param config   la configuration
     * @param path     le chemin de la section
     * @param fallback les valeurs par défaut
     * @return le son configuré
     */
    static SoundSpec from(Configuration config, String path, SoundSpec fallback) {
        // Ecrire directement « sound: block.note_block.pling » est naturel ;
        // on l'accepte comme raccourci, le volume et la hauteur gardant alors
        // leurs valeurs par defaut. Sans cela, la forme courte provoquerait une
        // ClassCastException au demarrage.
        Object raw = config.get(path);
        if (raw instanceof String name) {
            return new SoundSpec(name.trim(), fallback.volume(), fallback.pitch());
        }

        return new SoundSpec(
                config.getString(path + ".name", fallback.name()).trim(),
                (float) Math.max(0, config.getDouble(path + ".volume", fallback.volume())),
                (float) Math.max(0, config.getDouble(path + ".pitch", fallback.pitch())));
    }
}
