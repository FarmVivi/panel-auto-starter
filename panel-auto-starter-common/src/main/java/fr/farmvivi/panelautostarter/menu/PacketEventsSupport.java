package fr.farmvivi.panelautostarter.menu;

/**
 * Détecte la présence de PacketEvents sans en charger la moindre classe.
 * <p>
 * La détection ne peut pas vivre dans le rendu qui l'utilise : charger cette
 * classe-là chargerait aussi les types de PacketEvents auxquels ses champs font
 * référence, et lèverait un {@code NoClassDefFoundError} sur un proxy où la
 * bibliothèque n'est pas installée — c'est-à-dire précisément le cas qu'on
 * cherche à reconnaître. D'où le passage par la réflexion, qui ne lie rien à la
 * compilation.
 */
public final class PacketEventsSupport {

    private static final String API_CLASS = "com.github.retrooper.packetevents.PacketEvents";

    private PacketEventsSupport() {
    }

    /**
     * Indique si PacketEvents est installé et initialisé.
     *
     * @return true si son API répond
     */
    public static boolean isPresent() {
        try {
            return Class.forName(API_CLASS).getMethod("getAPI").invoke(null) != null;
        } catch (Throwable ignored) {
            // Absente, pas encore initialisee, ou en desaccord avec cette
            // version : dans tous les cas, on s'en passe.
            return false;
        }
    }
}
