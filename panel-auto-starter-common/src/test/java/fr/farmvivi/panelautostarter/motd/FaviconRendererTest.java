package fr.farmvivi.panelautostarter.motd;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du rendu des favicons décorés.
 * <p>
 * Le rendu est vérifié en inspectant directement les pixels : c'est le seul
 * moyen d'attester qu'une pastille est bien dessinée et que la désaturation
 * s'applique, sans quoi ces tests ne feraient que constater l'absence
 * d'exception.
 */
public class FaviconRendererTest {

    /** Centre de la pastille, tel que la calcule le renderer. */
    private static final int BADGE_CENTER = 48;
    /** Point situé dans la pastille mais hors du symbole central. */
    private static final int BADGE_EDGE_X = 40;
    /** Point du favicon éloigné de la pastille. */
    private static final int ARTWORK_X = 10;

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(FaviconRenderer.SIZE, FaviconRenderer.SIZE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, FaviconRenderer.SIZE, FaviconRenderer.SIZE);
        g.dispose();
        return image;
    }

    private static int red(int argb) {
        return (argb >> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    // ===================== Hors-ligne =====================

    @Test
    public void testOfflineDesaturatesTheArtwork() {
        BufferedImage rendered = FaviconRenderer.offline(solid(new Color(0xE0, 0x20, 0x20)));

        int pixel = rendered.getRGB(ARTWORK_X, ARTWORK_X);
        assertEquals(red(pixel), green(pixel), "Un pixel desature doit avoir R = V");
        assertEquals(green(pixel), blue(pixel), "Un pixel desature doit avoir V = B");
    }

    /**
     * La luminance perceptuelle doit être utilisée, pas une moyenne des canaux :
     * un rouge saturé et un vert saturé doivent donner des gris différents,
     * sinon toutes les couleurs vives se confondent en un même gris.
     */
    @Test
    public void testOfflineUsesPerceptualLuminance() {
        int fromRed = FaviconRenderer.offline(solid(Color.RED)).getRGB(ARTWORK_X, ARTWORK_X);
        int fromGreen = FaviconRenderer.offline(solid(Color.GREEN)).getRGB(ARTWORK_X, ARTWORK_X);

        assertNotEquals(red(fromRed), red(fromGreen),
                "Rouge et vert purs ne doivent pas produire le meme gris");
        assertTrue(red(fromGreen) > red(fromRed),
                "Le vert est percu plus lumineux que le rouge");
    }

    @Test
    public void testOfflineDrawsRedBadgeWithWhiteSquare() {
        BufferedImage rendered = FaviconRenderer.offline(solid(Color.BLACK));

        int badge = rendered.getRGB(BADGE_EDGE_X, BADGE_CENTER);
        assertTrue(red(badge) > 150 && green(badge) < 110 && blue(badge) < 110,
                "La pastille hors-ligne doit etre rouge, obtenu: " + Integer.toHexString(badge));

        int symbol = rendered.getRGB(BADGE_CENTER, BADGE_CENTER);
        assertTrue(red(symbol) > 200 && green(symbol) > 200 && blue(symbol) > 200,
                "Le symbole d'arret doit etre blanc, obtenu: " + Integer.toHexString(symbol));
    }

    // ===================== Démarrage =====================

    @Test
    public void testStartingKeepsColorsAndDrawsAmberBadge() {
        BufferedImage rendered = FaviconRenderer.starting(solid(new Color(0x20, 0x40, 0xE0)));

        int artwork = rendered.getRGB(ARTWORK_X, ARTWORK_X);
        assertTrue(blue(artwork) > red(artwork),
                "Le favicon ne doit pas etre desature en demarrage");

        int badge = rendered.getRGB(BADGE_EDGE_X, BADGE_CENTER);
        assertTrue(red(badge) > 180 && green(badge) > 110 && blue(badge) < 110,
                "La pastille de demarrage doit etre ambre, obtenu: " + Integer.toHexString(badge));
    }

    @Test
    public void testOfflineAndStartingAreDistinguishable() {
        BufferedImage source = solid(Color.BLUE);

        int offlineBadge = FaviconRenderer.offline(source).getRGB(BADGE_EDGE_X, BADGE_CENTER);
        int startingBadge = FaviconRenderer.starting(source).getRGB(BADGE_EDGE_X, BADGE_CENTER);

        assertNotEquals(offlineBadge, startingBadge,
                "Les deux etats doivent se distinguer au premier coup d'oeil");
    }

    // ===================== Robustesse =====================

    /**
     * Le rendu est mémoïsé et le favicon source est conservé en cache : le
     * modifier au passage corromprait le cache pour les rendus suivants.
     */
    @Test
    public void testSourceImageIsNeverModified() {
        BufferedImage source = solid(Color.RED);
        int before = source.getRGB(BADGE_CENTER, BADGE_CENTER);

        FaviconRenderer.offline(source);
        FaviconRenderer.starting(source);

        assertEquals(before, source.getRGB(BADGE_CENTER, BADGE_CENTER),
                "L'image source doit rester intacte");
    }

    @Test
    public void testOutputIsAlways64x64() {
        BufferedImage small = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);

        BufferedImage rendered = FaviconRenderer.offline(small);

        assertEquals(FaviconRenderer.SIZE, rendered.getWidth());
        assertEquals(FaviconRenderer.SIZE, rendered.getHeight());
    }

    @Test
    public void testNullSourceYieldsNull() {
        assertNull(FaviconRenderer.offline(null));
        assertNull(FaviconRenderer.starting(null));
    }
}
