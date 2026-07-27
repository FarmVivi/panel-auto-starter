package fr.farmvivi.panelautostarter.common.ping;

/**
 * Icône de serveur affichée dans la liste des serveurs du client.
 */
public interface CommonFavicon {
    /**
     * Retourne le favicon sous sa forme encodée, telle qu'elle transite sur le
     * réseau : une data URI {@code data:image/png;base64,...}.
     *
     * @return la data URI du favicon
     */
    String getEncoded();
}
