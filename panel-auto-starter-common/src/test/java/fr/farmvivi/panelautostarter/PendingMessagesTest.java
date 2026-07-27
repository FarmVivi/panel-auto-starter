package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests du registre des messages différés.
 */
public class PendingMessagesTest {

    private PendingMessages pending;
    private MockCommonPlayer player;

    @BeforeEach
    public void setUp() {
        pending = new PendingMessages();
        player = new MockCommonPlayer("Vivi");
    }

    @Test
    public void testQueuedMessageIsDeliveredOnFlush() {
        pending.queue(player, Component.text("Le serveur demarre"));

        assertNull(player.getLastMessage(), "Rien ne doit partir avant le flush");
        assertEquals(1, pending.flush(player));
        assertEquals(Component.text("Le serveur demarre"), player.getLastMessage());
    }

    @Test
    public void testFlushEmptiesTheRegistry() {
        pending.queue(player, Component.text("un"));

        pending.flush(player);

        assertEquals(0, pending.size());
        assertEquals(0, pending.flush(player), "Un second flush ne doit rien redelivrer");
    }

    @Test
    public void testSeveralMessagesAreAllDelivered() {
        pending.queue(player, Component.text("un"));
        pending.queue(player, Component.text("deux"));

        assertEquals(2, pending.flush(player));
        assertEquals(Component.text("deux"), player.getLastMessage());
    }

    @Test
    public void testMessagesAreKeptPerPlayer() {
        MockCommonPlayer other = new MockCommonPlayer("Bob");
        pending.queue(player, Component.text("pour Vivi"));
        pending.queue(other, Component.text("pour Bob"));

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
        pending.queue(player, Component.text("un"));

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
        pending.queue(null, Component.text("un"));
        pending.queue(player, null);
        pending.discard(null);

        assertEquals(0, pending.size());
    }
}
