package fr.farmvivi.panelautostarter.motd;

import net.kyori.adventure.text.Component;

import java.util.Arrays;

/**
 * MOTD d'un serveur tel qu'il était la dernière fois qu'il a été vu en ligne.
 *
 * @param description  la description affichée, ou null si jamais observée
 * @param faviconPng   les octets PNG du favicon, ou null si jamais observé
 */
public record CachedMotd(Component description, byte[] faviconPng) {
    /**
     * Un MOTD vide, pour un serveur jamais vu en ligne.
     */
    public static final CachedMotd EMPTY = new CachedMotd(null, null);

    /**
     * Indique si rien n'a encore été observé pour ce serveur.
     *
     * @return true si ni description ni favicon ne sont disponibles
     */
    public boolean isEmpty() {
        return description == null && faviconPng == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CachedMotd other)) {
            return false;
        }
        return java.util.Objects.equals(description, other.description)
                && Arrays.equals(faviconPng, other.faviconPng);
    }

    @Override
    public int hashCode() {
        return 31 * java.util.Objects.hashCode(description) + Arrays.hashCode(faviconPng);
    }

    @Override
    public String toString() {
        return "CachedMotd[description=" + description
                + ", faviconPng=" + (faviconPng == null ? "null" : faviconPng.length + " octets") + "]";
    }
}
