package fr.farmvivi.panelautostarter.motd;

import fr.farmvivi.panelautostarter.MinecraftServerStatus;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.ping.CommonFavicon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests des icônes livrées avec le plugin, utilisées par {@code favicon: bundled}.
 */
public class BundledFaviconsTest {

    @Mock
    private CommonProxy proxy;

    @Mock
    private CommonFavicon builtFavicon;

    private List<String> warnings;
    private List<String> requested;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        warnings = new ArrayList<>();
        requested = new ArrayList<>();
        when(proxy.createFavicon(any())).thenReturn(builtFavicon);
    }

    /** Charge les vraies ressources du plugin depuis le classpath de test. */
    private Function<String, InputStream> realResources() {
        return name -> {
            requested.add(name);
            return getClass().getClassLoader().getResourceAsStream(name);
        };
    }

    private BundledFavicons favicons(Function<String, InputStream> loader) {
        return new BundledFavicons(loader, proxy, warnings::add);
    }

    /**
     * Les deux images doivent réellement être livrées dans le jar : sans elles,
     * {@code favicon: bundled} ne produirait rien.
     */
    @Test
    public void testShippedResourcesExistAndLoad() {
        BundledFavicons bundled = favicons(realResources());

        assertSame(builtFavicon, bundled.get(MinecraftServerStatus.OFFLINE));
        assertSame(builtFavicon, bundled.get(MinecraftServerStatus.STARTING));
        assertTrue(warnings.isEmpty(), "Aucun avertissement attendu : " + warnings);
        assertTrue(requested.contains(BundledFavicons.OFFLINE_RESOURCE));
        assertTrue(requested.contains(BundledFavicons.STARTING_RESOURCE));
    }

    /**
     * Ces images sont partagées par tous les serveurs : les décoder à chaque
     * ping serait absurde.
     */
    @Test
    public void testResourcesAreLoadedOnlyOnce() {
        BundledFavicons bundled = favicons(realResources());

        for (int i = 0; i < 10; i++) {
            bundled.get(MinecraftServerStatus.OFFLINE);
            bundled.get(MinecraftServerStatus.STARTING);
        }

        assertEquals(2, requested.size(), "Chaque ressource ne doit etre lue qu'une fois");
        verify(proxy, times(2)).createFavicon(any());
    }

    @Test
    public void testOnlineHasNoBundledIcon() {
        assertNull(favicons(realResources()).get(MinecraftServerStatus.ONLINE));
    }

    // ===================== Robustesse =====================

    @Test
    public void testMissingResourceIsReportedWithoutThrowing() {
        BundledFavicons bundled = favicons(name -> null);

        assertNull(assertDoesNotThrow(() -> bundled.get(MinecraftServerStatus.OFFLINE)));
        assertEquals(2, warnings.size(), "Les deux ressources manquantes doivent etre signalees");
        assertTrue(warnings.get(0).contains("img/"));
    }

    @Test
    public void testUnreadableResourceIsReportedWithoutThrowing() {
        BundledFavicons bundled = favicons(name -> new ByteArrayInputStream("pas une image".getBytes()));

        assertNull(assertDoesNotThrow(() -> bundled.get(MinecraftServerStatus.OFFLINE)));
        assertFalse(warnings.isEmpty(), "Une image illisible doit etre signalee");
    }
}
