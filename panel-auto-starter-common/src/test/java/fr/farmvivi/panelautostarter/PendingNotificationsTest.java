package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du registre des retours différés.
 */
public class PendingNotificationsTest {

    private PendingNotifications pending;
    private MockCommonPlayer player;

    @BeforeEach
    public void setUp() {
        pending = new PendingNotifications();
        player = new MockCommonPlayer("Vivi");
    }

    @Test
    public void testQueuedMessageIsDeliveredOnFlush() {
        pending.queueMessage(player, Component.text("Le serveur demarre"));

        assertNull(player.getLastMessage(), "Rien ne doit partir avant le flush");
        assertEquals(1, pending.flush(player));
        assertEquals(Component.text("Le serveur demarre"), player.getLastMessage());
    }

    @Test
    public void testFlushEmptiesTheRegistry() {
        pending.queueMessage(player, Component.text("un"));

        pending.flush(player);

        assertEquals(0, pending.size());
        assertEquals(0, pending.flush(player), "Un second flush ne doit rien redelivrer");
    }

    @Test
    public void testSeveralMessagesAreAllDelivered() {
        pending.queueMessage(player, Component.text("un"));
        pending.queueMessage(player, Component.text("deux"));

        assertEquals(2, pending.flush(player));
        assertEquals(Component.text("deux"), player.getLastMessage());
    }

    @Test
    public void testMessagesAreKeptPerPlayer() {
        MockCommonPlayer other = new MockCommonPlayer("Bob");
        pending.queueMessage(player, Component.text("pour Vivi"));
        pending.queueMessage(other, Component.text("pour Bob"));

        pending.flush(player);

        assertEquals(Component.text("pour Vivi"), player.getLastMessage());
        assertNull(other.getLastMessage(), "Le message de l'autre joueur ne doit pas partir");
        assertEquals(1, pending.size());
    }

    /**
     * Sans purge à la déconnexion, un joueur parti avant d'arriver sur le limbo
     * laisserait son message en mémoire indéfiniment.
     */
    @Test
    public void testDiscardPreventsTheLeak() {
        pending.queueMessage(player, Component.text("un"));

        pending.discard(player);

        assertEquals(0, pending.size());
        assertEquals(0, pending.flush(player));
        assertNull(player.getLastMessage(), "Un message abandonne ne doit jamais partir");
    }

    @Test
    public void testFlushOnUnknownPlayerIsHarmless() {
        assertEquals(0, pending.flush(player));
        assertEquals(0, pending.flush(null));
    }

    @Test
    public void testNullArgumentsAreIgnored() {
        pending.queueMessage(null, Component.text("un"));
        pending.queueMessage(player, null);
        pending.discard(null);

        assertEquals(0, pending.size());
    }

    // ===================== Titres =====================

    @Test
    public void testQueuedTitleIsShownOnFlush() {
        pending.queueTitle(player, Component.text("Demarrage"), Component.text("Survie"),
                Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);

        assertNull(player.getLastTitle(), "Rien ne doit s'afficher avant le flush");
        assertEquals(1, pending.flush(player));
        assertEquals(Component.text("Demarrage"), player.getLastTitle());
        assertEquals(Component.text("Survie"), player.getLastSubtitle());
    }

    /**
     * Titre et message sont rejoués dans leur ordre d'origine, pour que le
     * joueur les reçoive comme s'ils n'avaient jamais été différés.
     */
    @Test
    public void testTitleAndMessageKeepTheirOrder() {
        pending.queueTitle(player, Component.text("Demarrage"), Component.text("Survie"),
                Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);
        pending.queueMessage(player, Component.text("File d'attente"));

        assertEquals(2, pending.flush(player), "Les deux retours doivent partir");
        assertEquals(Component.text("Demarrage"), player.getLastTitle());
        assertEquals(Component.text("File d'attente"), player.getLastMessage());
    }

    @Test
    public void testDiscardDropsTitlesToo() {
        pending.queueTitle(player, Component.text("Demarrage"), Component.text("Survie"),
                Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO);

        pending.discard(player);

        assertEquals(0, pending.flush(player));
        assertNull(player.getLastTitle());
    }
}
