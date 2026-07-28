package fr.farmvivi.panelautostarter.access;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie les trois portes du contrôle d'accès, et surtout qu'elles sont bien
 * indépendantes : une permission accordée ne dispense pas de la liste blanche,
 * et l'inverse non plus.
 */
public class ServerAccessTest {

    @TempDir
    Path tempDir;

    private WhitelistStore store;
    private final CommonServer survie = new MockCommonServer("survie");

    @BeforeEach
    public void setUp() {
        store = new WhitelistStore(tempDir.toFile(), message -> {
        });
    }

    private ServerAccess access(boolean requirePermission) {
        return new ServerAccess(new AccessSettings(true, requirePermission, true,
                ServerAccess.DEFAULT_PERMISSION_FORMAT), store);
    }

    // ===================== Permission =====================

    @Test
    public void testAccessIsOpenWhenNoPermissionIsRequired() {
        assertTrue(access(false).check(new MockCommonPlayer("Vivi"), survie).isAllowed(),
                "Sans exigence de permission, l'acces reste libre");
    }

    @Test
    public void testThePermissionIsRequiredWhenAsked() {
        ServerAccess access = access(true);

        assertEquals(ServerAccess.Result.NO_PERMISSION,
                access.check(new MockCommonPlayer("Vivi"), survie));
        assertTrue(access.check(new MockCommonPlayer("Vivi")
                .grant("panelautostarter.join.survie"), survie).isAllowed());
    }

    /**
     * Les nœuds de permission sont conventionnellement en minuscules, et
     * LuckPerms les compare ainsi : un serveur nommé « Survie » donnerait sinon
     * une permission que personne n'arriverait à accorder.
     */
    @Test
    public void testThePermissionNodeIsLowercased() {
        assertEquals("panelautostarter.join.streetland_survie",
                access(true).permissionFor("Streetland_Survie"));
    }

    @Test
    public void testThePermissionNodeFollowsTheConfiguredFormat() {
        ServerAccess access = new ServerAccess(
                new AccessSettings(true, true, true, "monreseau.serveur.%s"), store);

        assertEquals("monreseau.serveur.survie", access.permissionFor("survie"));
        assertTrue(access.check(new MockCommonPlayer("Vivi")
                .grant("monreseau.serveur.survie"), survie).isAllowed());
    }

    // ===================== Liste blanche =====================

    @Test
    public void testAnInactiveWhitelistLetsEveryoneThrough() {
        store.get("survie").add(java.util.UUID.randomUUID(), "QuelquUnDautre");

        assertTrue(access(false).check(new MockCommonPlayer("Vivi"), survie).isAllowed(),
                "Une liste blanche desactivee ne filtre rien, meme non vide");
    }

    @Test
    public void testAnActiveWhitelistKeepsOutsidersOut() {
        store.get("survie").setEnabled(true);

        assertEquals(ServerAccess.Result.NOT_WHITELISTED,
                access(false).check(new MockCommonPlayer("Vivi"), survie));
    }

    @Test
    public void testAListedPlayerGetsIn() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        Whitelist whitelist = store.get("survie");
        whitelist.setEnabled(true);
        whitelist.add(player.getUniqueId(), "Vivi");

        assertTrue(access(false).check(player, survie).isAllowed());
    }

    /**
     * Le point de la décision « les deux » : les deux mécanismes répondent à
     * des questions différentes et ne se remplacent pas.
     */
    @Test
    public void testThePermissionDoesNotExemptFromTheWhitelist() {
        store.get("survie").setEnabled(true);
        MockCommonPlayer autorise = new MockCommonPlayer("Vivi")
                .grant("panelautostarter.join.survie");

        assertEquals(ServerAccess.Result.NOT_WHITELISTED, access(true).check(autorise, survie),
                "Avoir la permission ne dispense pas de figurer sur la liste");
    }

    @Test
    public void testTheWhitelistDoesNotExemptFromThePermission() {
        MockCommonPlayer inscrit = new MockCommonPlayer("Vivi");
        Whitelist whitelist = store.get("survie");
        whitelist.setEnabled(true);
        whitelist.add(inscrit.getUniqueId(), "Vivi");

        assertEquals(ServerAccess.Result.NO_PERMISSION, access(true).check(inscrit, survie),
                "Figurer sur la liste ne dispense pas de la permission");
    }

    /**
     * Chaque serveur a sa propre liste : une inscription sur l'un ne vaut pas
     * sur l'autre.
     */
    @Test
    public void testWhitelistsAreScopedToTheirServer() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        store.get("creatif").setEnabled(true);
        store.get("creatif").add(player.getUniqueId(), "Vivi");
        store.get("survie").setEnabled(true);

        assertEquals(ServerAccess.Result.NOT_WHITELISTED, access(false).check(player, survie));
        assertTrue(access(false).check(player, new MockCommonServer("creatif")).isAllowed());
    }

    // ===================== Restriction du proxy =====================

    /**
     * BungeeCord possède la notion en propre ; la réutiliser évite de demander
     * à l'administrateur de déclarer deux fois la même restriction.
     */
    @Test
    public void testAProxyRestrictionIsHonoured() {
        CommonServer restreint = new MockCommonServer("survie") {
            @Override
            public boolean isAllowedByProxy(CommonPlayer player) {
                return false;
            }
        };

        assertEquals(ServerAccess.Result.NO_PERMISSION,
                access(false).check(new MockCommonPlayer("Vivi"), restreint));
    }

    @Test
    public void testAProxyRestrictionCanBeIgnored() {
        CommonServer restreint = new MockCommonServer("survie") {
            @Override
            public boolean isAllowedByProxy(CommonPlayer player) {
                return false;
            }
        };
        ServerAccess access = new ServerAccess(new AccessSettings(true, false, false,
                ServerAccess.DEFAULT_PERMISSION_FORMAT), store);

        assertTrue(access.check(new MockCommonPlayer("Vivi"), restreint).isAllowed());
    }

    // ===================== Interrupteur general =====================

    @Test
    public void testDisablingAccessControlOpensEverything() {
        store.get("survie").setEnabled(true);
        ServerAccess access = new ServerAccess(new AccessSettings(false, true, true,
                ServerAccess.DEFAULT_PERMISSION_FORMAT), store);

        assertTrue(access.check(new MockCommonPlayer("Vivi"), survie).isAllowed());
    }

    @Test
    public void testNullArgumentsAreAllowedThrough() {
        ServerAccess access = access(true);

        assertTrue(access.check(null, survie).isAllowed());
        assertTrue(access.check(new MockCommonPlayer("Vivi"), null).isAllowed());
    }

    // ===================== Reglages =====================

    /**
     * Un motif sans {@code %s} donnerait la même permission à tous les
     * serveurs, ce qui viderait la restriction par serveur de son sens.
     */
    @Test
    public void testAPermissionFormatWithoutPlaceholderIsRejected() {
        net.md_5.bungee.config.Configuration config = new net.md_5.bungee.config.Configuration();
        config.set("access.permission-format", "panelautostarter.join");

        assertEquals(ServerAccess.DEFAULT_PERMISSION_FORMAT,
                AccessSettings.from(config).permissionFormat());
    }

    @Test
    public void testDefaultsDoNotLockAnybodyOut() {
        AccessSettings defaults = AccessSettings.defaults();

        assertTrue(defaults.enabled(), "Le controle est en place...");
        assertFalse(defaults.requirePermission(),
                "...mais ne refuse rien tant que l'administrateur n'a rien demande");
    }

    @Test
    public void testSettingsAreReadFromTheConfiguration() {
        net.md_5.bungee.config.Configuration config = new net.md_5.bungee.config.Configuration();
        config.set("access.require-permission", true);
        config.set("access.respect-proxy-restrictions", false);

        AccessSettings settings = AccessSettings.from(config);

        assertTrue(settings.requirePermission());
        assertFalse(settings.respectProxyRestrictions());
    }

    @Test
    public void testAbsentConfigurationFallsBackToDefaults() {
        assertEquals(AccessSettings.defaults(), AccessSettings.from(null));
    }

    // ===================== Persistance =====================

    @Test
    public void testWhitelistsSurviveARestart() {
        MockCommonPlayer player = new MockCommonPlayer("Vivi");
        Whitelist whitelist = store.get("survie");
        whitelist.setEnabled(true);
        whitelist.add(player.getUniqueId(), "Vivi");

        assertTrue(store.save(), "L'ecriture doit aboutir");

        WhitelistStore reloaded = new WhitelistStore(tempDir.toFile(), message -> {
        });

        assertTrue(reloaded.get("survie").isEnabled(), "L'activation doit etre conservee");
        assertTrue(reloaded.get("survie").contains(player.getUniqueId()),
                "Le joueur inscrit doit etre retrouve");
        assertEquals("Vivi", reloaded.get("survie").getPlayers().get(player.getUniqueId()),
                "Le pseudo est conserve pour que le fichier reste lisible");
    }

    @Test
    public void testAnAbsentFileYieldsEmptyWhitelists() {
        WhitelistStore empty = new WhitelistStore(new File(tempDir.toFile(), "inexistant"),
                message -> {
                });

        assertFalse(empty.get("survie").isEnabled());
        assertEquals(0, empty.get("survie").size());
    }

    /**
     * Un identifiant illisible ne doit pas faire perdre le reste de la liste,
     * ni passer inaperçu.
     */
    @Test
    public void testACorruptEntryIsReportedAndSkipped() throws Exception {
        java.nio.file.Files.writeString(tempDir.resolve("whitelist.yml"),
                "survie:\n  enabled: true\n  players:\n    pas-un-uuid: Cassé\n");
        java.util.List<String> warnings = new java.util.ArrayList<>();

        WhitelistStore loaded = new WhitelistStore(tempDir.toFile(), warnings::add);

        assertTrue(loaded.get("survie").isEnabled(), "Le reste de la section reste lu");
        assertEquals(0, loaded.get("survie").size(), "L'entree illisible est ignoree");
        assertEquals(1, warnings.size(), "Et elle est signalee");
    }
}
