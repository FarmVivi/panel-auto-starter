package fr.farmvivi.panelautostarter.common.ping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Conversions entre un favicon encodé sur le réseau et une image manipulable.
 * <p>
 * Un favicon Minecraft est un PNG 64x64 transporté sous forme de data URI.
 */
public final class Favicons {
    /**
     * Préfixe de la data URI d'un favicon.
     */
    public static final String DATA_URI_PREFIX = "data:image/png;base64,";

    private Favicons() {
    }

    /**
     * Décode un favicon encodé vers son image.
     *
     * @param encoded la data URI, éventuellement null
     * @return l'image, ou null si la valeur est absente ou illisible
     */
    public static BufferedImage decode(String encoded) {
        byte[] png = decodeToPng(encoded);
        return png == null ? null : read(png);
    }

    /**
     * Décode un favicon encodé vers les octets du PNG.
     *
     * @param encoded la data URI, éventuellement null
     * @return les octets du PNG, ou null si la valeur est absente ou illisible
     */
    public static byte[] decodeToPng(String encoded) {
        if (encoded == null) {
            return null;
        }
        // Certains serveurs inserent des retours a la ligne dans la data URI.
        String cleaned = encoded.replaceAll("\\s", "");
        if (!cleaned.startsWith(DATA_URI_PREFIX)) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(cleaned.substring(DATA_URI_PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Lit une image depuis des octets PNG.
     *
     * @param png les octets, éventuellement null
     * @return l'image, ou null si les octets sont absents ou illisibles
     */
    public static BufferedImage read(byte[] png) {
        if (png == null || png.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Encode une image en octets PNG.
     *
     * @param image l'image, éventuellement null
     * @return les octets du PNG, ou null en cas d'échec
     */
    public static byte[] toPng(BufferedImage image) {
        if (image == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            return null;
        }
    }
}
