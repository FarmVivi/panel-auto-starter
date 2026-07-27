package fr.farmvivi.panelautostarter.motd;

import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.common.ping.Favicons;
import fr.farmvivi.panelautostarter.mocks.MockCommonServerPing;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests du MOTD conservé par serveur : persistance, mémoïsation du rendu et
 * tolérance aux pannes d'écriture.
 */
public class ServerMotdTest {
    private static final String SERVER = "streetland_survie";

    @Mock
    private MotdStore store;

    @Mock
    private CommonProxy proxy;

    @Mock
    private CommonFavicon renderedFavicon;

    private List<String> warnings;
    private ServerMotd motd;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        warnings = new ArrayList<>();
        when(store.load(SERVER)).thenReturn(CachedMotd.EMPTY);
        when(proxy.createFavicon(any())).thenReturn(renderedFavicon);
        motd = new ServerMotd(store, SERVER, proxy, warnings::add);
    }

    /** Construit un ping en ligne portant une description et un favicon valides. */
    private CommonServerPing onlinePing(String description, Color faviconColor) {
        MockCommonServerPing ping = new MockCommonServerPing();
        ping.setDescriptionComponent(Component.text(description));
        if (faviconColor != null) {
            ping.setFavicon(encoded(faviconColor));
        }
        return ping;
    }

    private CommonFavicon encoded(Color color) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        String uri = Favicons.DATA_URI_PREFIX
                + java.util.Base64.getEncoder().encodeToString(Favicons.toPng(image));
        // CommonFavicon n'expose qu'une methode : une lambda suffit.
        return () -> uri;
    }

    // ===================== Observation et persistance =====================

    @Test
    public void testStartsEmptyWhenNothingWasEverStored() {
        assertTrue(motd.isEmpty());
        assertNull(motd.getDescription());
        assertNull(motd.getFavicon(MinecraftServerStatus.OFFLINE));
    }

    @Test
    public void testObservingOnlinePingStoresDescriptionAndFavicon() throws IOException {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));

        assertEquals(Component.text("Bienvenue"), motd.getDescription());
        assertFalse(motd.isEmpty());
        verify(store).save(eq(SERVER), any(CachedMotd.class));
    }

    /**
     * Le serveur est interrogé toutes les 15 secondes : réécrire les fichiers à
     * chaque passage serait de l'usure disque pour rien.
     */
    @Test
    public void testUnchangedMotdIsNotWrittenAgain() throws IOException {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));

        verify(store, times(1)).save(eq(SERVER), any(CachedMotd.class));
    }

    @Test
    public void testChangedMotdIsWrittenAgain() throws IOException {
        motd.observeOnline(onlinePing("Avant", Color.RED));
        motd.observeOnline(onlinePing("Apres", Color.RED));

        verify(store, times(2)).save(eq(SERVER), any(CachedMotd.class));
        assertEquals(Component.text("Apres"), motd.getDescription());
    }

    @Test
    public void testNullPingIsIgnored() throws IOException {
        motd.observeOnline(null);

        assertTrue(motd.isEmpty());
        verify(store, never()).save(anyString(), any());
    }

    /**
     * Un serveur qui ne renvoie pas de favicon ne doit pas effacer celui qu'on
     * avait déjà conservé.
     */
    @Test
    public void testMissingFaviconDoesNotWipeTheStoredOne() {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));

        motd.observeOnline(onlinePing("Bienvenue", null));

        assertNotNull(motd.getFavicon(MinecraftServerStatus.OFFLINE),
                "Le favicon conserve doit survivre a un ping qui n'en fournit pas");
    }

    /**
     * Le cache disque est un confort. Si l'écriture échoue — disque plein,
     * droits insuffisants — le MOTD doit rester correct pour la session en
     * cours, avec un simple avertissement.
     */
    @Test
    public void testWriteFailureIsLoggedButDoesNotBreakTheMotd() throws IOException {
        doThrow(new IOException("disque plein")).when(store).save(anyString(), any());

        assertDoesNotThrow(() -> motd.observeOnline(onlinePing("Bienvenue", Color.RED)));

        assertEquals(Component.text("Bienvenue"), motd.getDescription(),
                "Le MOTD doit rester utilisable en memoire malgre l'echec d'ecriture");
        assertEquals(1, warnings.size(), "L'echec doit etre signale");
        assertTrue(warnings.get(0).contains(SERVER));
    }

    // ===================== Mémoïsation du rendu =====================

    /**
     * Garantie de performance : décorer le favicon en Java2D à chaque ping
     * client serait du gaspillage. Le rendu ne doit avoir lieu qu'une fois.
     */
    @Test
    public void testFaviconRenderingIsMemoized() {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));

        for (int i = 0; i < 20; i++) {
            motd.getFavicon(MinecraftServerStatus.OFFLINE);
        }

        verify(proxy, times(1)).createFavicon(any());
    }

    @Test
    public void testOfflineAndStartingAreRenderedSeparately() {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));

        motd.getFavicon(MinecraftServerStatus.OFFLINE);
        motd.getFavicon(MinecraftServerStatus.STARTING);
        motd.getFavicon(MinecraftServerStatus.OFFLINE);
        motd.getFavicon(MinecraftServerStatus.STARTING);

        verify(proxy, times(2)).createFavicon(any());
    }

    /**
     * Contrepartie de la mémoïsation : si le serveur revient en ligne avec un
     * favicon différent, le rendu conservé doit être invalidé, sinon on
     * afficherait indéfiniment l'ancienne icône.
     */
    @Test
    public void testRenderingIsInvalidatedWhenFaviconChanges() {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));
        motd.getFavicon(MinecraftServerStatus.OFFLINE);

        motd.observeOnline(onlinePing("Bienvenue", Color.BLUE));
        motd.getFavicon(MinecraftServerStatus.OFFLINE);

        verify(proxy, times(2)).createFavicon(any());
    }

    @Test
    public void testOnlineStatusHasNoDecoratedFavicon() {
        motd.observeOnline(onlinePing("Bienvenue", Color.RED));

        assertNull(motd.getFavicon(MinecraftServerStatus.ONLINE),
                "En ligne, c'est le favicon reel du serveur qui est servi, pas un rendu");
    }

    @Test
    public void testStoredMotdIsLoadedOnStartup() {
        when(store.load("autre")).thenReturn(new CachedMotd(Component.text("Depuis le disque"), null));

        ServerMotd reloaded = new ServerMotd(store, "autre", proxy, warnings::add);

        assertEquals(Component.text("Depuis le disque"), reloaded.getDescription(),
                "Le MOTD doit etre disponible des le demarrage, sans attendre une mise en ligne");
    }
}
