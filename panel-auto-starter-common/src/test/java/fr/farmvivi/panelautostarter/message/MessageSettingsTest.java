package fr.farmvivi.panelautostarter.message;

import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des réglages des retours visuels et sonores.
 */
public class MessageSettingsTest {

    private static MessageSettings parse(String yaml) {
        Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                .load(new StringReader(yaml));
        return MessageSettings.from(config);
    }

    @Test
    public void testDefaultsApplyWhenNothingIsConfigured() {
        MessageSettings settings = parse("queue:\n  server: lobby\n");

        assertTrue(settings.getTitle().enabled());
        assertEquals(Duration.ofMillis(2500), settings.getTitle().stay());
        assertTrue(settings.getCountdown().enabled());
        assertEquals(3, settings.getCountdown().seconds());
    }

    @Test
    public void testEveryFieldIsRead() {
        MessageSettings settings = parse("""
                queue:
                  title:
                    enabled: false
                    fade-in: 100
                    stay: 4000
                    fade-out: 250
                  countdown:
                    enabled: false
                    seconds: 5
                    tick-sound:
                      name: entity.experience_orb.pickup
                      volume: 0.25
                      pitch: 0.8
                    go-sound:
                      name: entity.player.levelup
                """);

        assertFalse(settings.getTitle().enabled());
        assertEquals(Duration.ofMillis(100), settings.getTitle().fadeIn());
        assertEquals(Duration.ofMillis(4000), settings.getTitle().stay());
        assertEquals(Duration.ofMillis(250), settings.getTitle().fadeOut());

        assertFalse(settings.getCountdown().enabled());
        assertEquals(5, settings.getCountdown().seconds());
        assertEquals("entity.experience_orb.pickup", settings.getCountdown().tickSound().name());
        assertEquals(0.25f, settings.getCountdown().tickSound().volume());
        assertEquals(0.8f, settings.getCountdown().tickSound().pitch());
        assertEquals("entity.player.levelup", settings.getCountdown().goSound().name());
    }

    /**
     * Un décompte négatif n'a pas de sens et produirait une boucle qui ne
     * s'arrête jamais.
     */
    @Test
    public void testNegativeCountdownIsClampedToZero() {
        MessageSettings settings = parse("""
                queue:
                  countdown:
                    seconds: -5
                """);

        assertEquals(0, settings.getCountdown().seconds());
    }

    @Test
    public void testNegativeDurationsAreClampedToZero() {
        MessageSettings settings = parse("""
                queue:
                  title:
                    fade-in: -300
                """);

        assertEquals(Duration.ZERO, settings.getTitle().fadeIn());
    }

    @Test
    public void testBlankSoundsMeanSilence() {
        MessageSettings settings = parse("""
                queue:
                  countdown:
                    tick-sound:
                      name: ""
                    go-sound:
                      name: "  "
                """);

        assertFalse(settings.getCountdown().usesSound(),
                "Deux sons vides doivent compter comme aucun son");
    }

    @Test
    public void testOneSoundIsEnoughToCountAsUsingSound() {
        MessageSettings settings = parse("""
                queue:
                  countdown:
                    tick-sound:
                      name: ""
                    go-sound:
                      name: block.note_block.pling
                """);

        assertTrue(settings.getCountdown().usesSound());
    }

    @Test
    public void testNullConfigYieldsDefaults() {
        MessageSettings settings = MessageSettings.from(null);

        assertTrue(settings.getTitle().enabled());
        assertEquals(3, settings.getCountdown().seconds());
    }

    /**
     * Le {@code config.yml} livré doit produire exactement les défauts codés en
     * dur, faute de quoi le fichier annoncerait un comportement que le code
     * n'applique pas.
     */
    @Test
    public void testShippedConfigYieldsTheCodedDefaults() throws Exception {
        Configuration shipped;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(in, "config.yml doit etre present dans les ressources");
            shipped = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        MessageSettings fromFile = MessageSettings.from(shipped);
        MessageSettings coded = MessageSettings.defaults();

        assertEquals(coded.getTitle(), fromFile.getTitle(),
                "La section queue.title livree doit refleter les defauts du code");
        assertEquals(coded.getCountdown(), fromFile.getCountdown(),
                "La section queue.countdown livree doit refleter les defauts du code");
    }

    /**
     * La forme courte doit être acceptée : écrire directement le nom du son est
     * naturel, et une ClassCastException au démarrage serait un accueil brutal.
     */
    @Test
    public void testShorthandSoundIsAccepted() {
        MessageSettings settings = parse("""
                queue:
                  countdown:
                    go-sound: block.note_block.bell
                """);

        assertEquals("block.note_block.bell", settings.getCountdown().goSound().name());
        assertEquals(MessageSettings.defaults().getCountdown().goSound().volume(),
                settings.getCountdown().goSound().volume(),
                "Le volume garde sa valeur par defaut");
    }

    @Test
    public void testNegativeVolumeIsClampedToZero() {
        MessageSettings settings = parse("""
                queue:
                  countdown:
                    go-sound:
                      name: block.note_block.bell
                      volume: -2
                """);

        assertEquals(0f, settings.getCountdown().goSound().volume());
    }

    /**
     * Les défauts sont volontairement discrets : un son de décompte joué à
     * plein volume chaque seconde devient vite pénible.
     */
    @Test
    public void testDefaultSoundsAreQuiet() {
        MessageSettings.CountdownSettings countdown = MessageSettings.defaults().getCountdown();

        assertTrue(countdown.tickSound().volume() < 1f,
                "Le tic doit etre plus discret que le volume plein");
        assertTrue(countdown.goSound().volume() < 1f,
                "Le son de depart doit rester doux");
        assertFalse(MessageSettings.defaults().getTitle().sound().isSilent(),
                "Le titre d'attente doit etre accompagne d'un son par defaut");
    }
}
