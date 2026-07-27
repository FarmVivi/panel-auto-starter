package fr.farmvivi.panelautostarter.motd;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Décore le favicon d'un serveur pour signaler son état.
 * <p>
 * Tout est dessiné à la volée : le plugin n'embarque aucune image, ce qui évite
 * qu'un favicon générique vienne écraser l'identité visuelle du serveur.
 * <p>
 * Les méthodes sont pures — elles ne modifient jamais l'image reçue — ce qui les
 * rend testables en inspectant directement les pixels du résultat.
 */
public final class FaviconRenderer {
    /**
     * Taille d'un favicon Minecraft.
     */
    public static final int SIZE = 64;

    private static final Color OFFLINE_BADGE = new Color(0xD1, 0x3A, 0x3A);
    private static final Color STARTING_BADGE = new Color(0xE8, 0x9C, 0x1F);
    private static final Color BADGE_SYMBOL = Color.WHITE;
    private static final Color BADGE_OUTLINE = new Color(0, 0, 0, 90);

    /**
     * Diamètre de la pastille, soit un peu plus d'un tiers de l'icône : assez
     * grand pour rester lisible dans la liste des serveurs, assez petit pour ne
     * pas masquer le favicon.
     */
    private static final int BADGE_DIAMETER = 26;
    private static final int BADGE_MARGIN = 3;

    /**
     * Pastille superposée au favicon.
     */
    public enum Badge {
        /** Rond rouge portant un carré blanc : serveur arrêté. */
        STOP,
        /** Rond ambre portant un chevron blanc : serveur en démarrage. */
        START
    }

    private FaviconRenderer() {
    }

    /**
     * Décore un favicon selon les options demandées.
     * <p>
     * Les deux décorations sont indépendantes : on peut désaturer sans pastille,
     * poser une pastille sans désaturer, les deux, ou aucune — auquel cas seule
     * la normalisation en 64x64 est appliquée.
     *
     * @param source    le favicon du serveur
     * @param grayscale désature l'image
     * @param badge     la pastille à superposer, ou null pour aucune
     * @return une nouvelle image, ou null si la source est absente
     */
    public static BufferedImage render(BufferedImage source, boolean grayscale, Badge badge) {
        if (source == null) {
            return null;
        }
        BufferedImage canvas = normalize(source);
        if (grayscale) {
            canvas = desaturate(canvas);
        }
        if (badge != null) {
            Graphics2D g = canvas.createGraphics();
            try {
                applyQuality(g);
                drawBadgeCircle(g, badge == Badge.STOP ? OFFLINE_BADGE : STARTING_BADGE);
                if (badge == Badge.STOP) {
                    drawStopSymbol(g);
                } else {
                    drawChevronSymbol(g);
                }
            } finally {
                g.dispose();
            }
        }
        return canvas;
    }

    /**
     * Rendu par défaut d'un serveur hors-ligne : désaturé, pastille d'arrêt.
     *
     * @param source le favicon du serveur
     * @return une nouvelle image, ou null si la source est absente
     */
    public static BufferedImage offline(BufferedImage source) {
        return render(source, true, Badge.STOP);
    }

    /**
     * Rendu par défaut d'un serveur en démarrage : couleurs conservées,
     * pastille de démarrage.
     *
     * @param source le favicon du serveur
     * @return une nouvelle image, ou null si la source est absente
     */
    public static BufferedImage starting(BufferedImage source) {
        return render(source, false, Badge.START);
    }

    /**
     * Copie la source dans une image 64x64 ARGB. Cela garantit un format
     * modifiable et la bonne taille, quel que soit ce qu'a renvoyé le serveur.
     */
    private static BufferedImage normalize(BufferedImage source) {
        BufferedImage canvas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        try {
            applyQuality(g);
            g.drawImage(source, 0, 0, SIZE, SIZE, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * Passe l'image en niveaux de gris en conservant la transparence.
     * <p>
     * La luminance est calculee avec les coefficients de perception usuels
     * (ITU-R BT.601) plutot qu'une moyenne des canaux, sinon les rouges
     * ressortent trop clairs et les bleus trop sombres.
     */
    private static BufferedImage desaturate(BufferedImage source) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int argb = source.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int lum = Math.min(255, (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b));
                out.setRGB(x, y, (a << 24) | (lum << 16) | (lum << 8) | lum);
            }
        }
        return out;
    }

    private static void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static int badgeX() {
        return SIZE - BADGE_DIAMETER - BADGE_MARGIN;
    }

    private static int badgeY() {
        return SIZE - BADGE_DIAMETER - BADGE_MARGIN;
    }

    private static void drawBadgeCircle(Graphics2D g, Color color) {
        Ellipse2D circle = new Ellipse2D.Float(badgeX(), badgeY(), BADGE_DIAMETER, BADGE_DIAMETER);
        g.setColor(color);
        g.fill(circle);
        // Un liseré sombre detache la pastille des favicons clairs.
        g.setColor(BADGE_OUTLINE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(circle);
    }

    private static void drawStopSymbol(Graphics2D g) {
        int side = BADGE_DIAMETER / 3;
        int x = badgeX() + (BADGE_DIAMETER - side) / 2;
        int y = badgeY() + (BADGE_DIAMETER - side) / 2;
        g.setColor(BADGE_SYMBOL);
        g.fillRect(x, y, side, side);
    }

    private static void drawChevronSymbol(Graphics2D g) {
        float cx = badgeX() + BADGE_DIAMETER / 2f;
        float cy = badgeY() + BADGE_DIAMETER / 2f;
        float w = BADGE_DIAMETER / 4.5f;
        float h = BADGE_DIAMETER / 3.2f;

        Path2D chevron = new Path2D.Float();
        chevron.moveTo(cx - w / 2, cy - h / 2);
        chevron.lineTo(cx + w / 2, cy);
        chevron.lineTo(cx - w / 2, cy + h / 2);

        g.setColor(BADGE_SYMBOL);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(chevron);
    }
}
