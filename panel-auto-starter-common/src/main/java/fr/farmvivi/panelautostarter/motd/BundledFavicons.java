package fr.farmvivi.panelautostarter.motd;

import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Icônes génériques livrées avec le plugin, utilisées quand la configuration
 * demande {@code favicon: bundled}.
 * <p>
 * Elles portent déjà leur propre indicateur d'état : les décorations
 * {@code favicon-grayscale} et {@code favicon-badge} ne leur sont pas
 * appliquées, sous peine d'empiler deux pastilles.
 * <p>
 * Le chargement est paresseux et le résultat mémorisé : ces images sont
 * partagées par tous les serveurs, il serait absurde de les décoder à chaque
 * ping.
 */
public class BundledFavicons {
    static final String OFFLINE_RESOURCE = "img/offline.png";
    static final String STARTING_RESOURCE = "img/starting.png";

    private final Function<String, InputStream> resourceLoader;
    private final CommonProxy proxy;
    private final Consumer<String> warningLogger;

    private boolean loaded;
    private CommonFavicon offline;
    private CommonFavicon starting;

    public BundledFavicons(Function<String, InputStream> resourceLoader, CommonProxy proxy,
                           Consumer<String> warningLogger) {
        this.resourceLoader = resourceLoader;
        this.proxy = proxy;
        this.warningLogger = warningLogger;
    }

    /**
     * Retourne l'icône livrée correspondant à un état.
     *
     * @param status l'état du serveur
     * @return l'icône, ou null si l'état n'en a pas ou si le chargement a échoué
     */
    public synchronized CommonFavicon get(MinecraftServerStatus status) {
        if (!loaded) {
            offline = load(OFFLINE_RESOURCE);
            starting = load(STARTING_RESOURCE);
            loaded = true;
        }
        return switch (status) {
            case OFFLINE -> offline;
            case STARTING -> starting;
            case ONLINE -> null;
        };
    }

    private CommonFavicon load(String resource) {
        try (InputStream in = resourceLoader.apply(resource)) {
            if (in == null) {
                warningLogger.accept("Icone livree introuvable : " + resource);
                return null;
            }
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                warningLogger.accept("Icone livree illisible : " + resource);
                return null;
            }
            return proxy.createFavicon(image);
        } catch (Exception ex) {
            warningLogger.accept("Impossible de charger l'icone livree " + resource + " : " + ex.getMessage());
            return null;
        }
    }
}
