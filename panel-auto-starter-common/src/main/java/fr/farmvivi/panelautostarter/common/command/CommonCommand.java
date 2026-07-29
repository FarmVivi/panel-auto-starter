package fr.farmvivi.panelautostarter.common.command;

import java.util.List;

/**
 * Commande indépendante de la plateforme.
 * <p>
 * BungeeCord et Velocity exposent deux API de commandes sans rapport ; chaque
 * module de plateforme se contente d'adapter celle-ci, comme il le fait déjà
 * pour les événements.
 */
public interface CommonCommand {

    /**
     * Retourne le nom de la commande, sans barre oblique.
     *
     * @return le nom
     */
    String getName();

    /**
     * Retourne les autres noms sous lesquels la commande répond.
     *
     * @return les alias, éventuellement vides
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * Permission exigée pour seulement voir la commande exister.
     * <p>
     * Le détail des droits se joue à l'exécution, serveur par serveur : cette
     * permission-ci ne fait que masquer la commande à qui n'a rien à y faire.
     *
     * @return le nœud de permission, ou null si la commande est ouverte à tous
     */
    default String getPermission() {
        return null;
    }

    /**
     * Exécute la commande.
     *
     * @param source l'auteur
     * @param args   les arguments, sans le nom de la commande
     */
    void execute(CommonCommandSource source, String[] args);

    /**
     * Propose des complétions.
     *
     * @param source l'auteur
     * @param args   les arguments déjà saisis, le dernier étant partiel
     * @return les propositions
     */
    default List<String> suggest(CommonCommandSource source, String[] args) {
        return List.of();
    }
}
