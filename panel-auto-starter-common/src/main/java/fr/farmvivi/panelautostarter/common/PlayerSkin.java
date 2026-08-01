package fr.farmvivi.panelautostarter.common;

import java.util.UUID;

/**
 * Apparence d'un joueur, telle que le proxy la connaît déjà.
 * <p>
 * Aucun appel à Mojang n'est nécessaire : le proxy a authentifié le joueur, et
 * détient donc la propriété {@code textures} de son profil, signature comprise.
 * Aller la redemander coûterait un aller-retour réseau par entrée de menu, pour
 * une donnée déjà en mémoire.
 * <p>
 * La signature doit accompagner la valeur : sans elle, un client en ligne
 * refuse la texture et retombe sur une tête générique.
 *
 * @param username  le pseudo du joueur
 * @param uuid      son identifiant
 * @param value     la propriété {@code textures}, encodée en base64
 * @param signature la signature de cette propriété, éventuellement null
 */
public record PlayerSkin(String username, UUID uuid, String value, String signature) {
}
