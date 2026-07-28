package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Vérifie que la file supporte d'être lue et modifiée depuis plusieurs threads.
 * <p>
 * Elle l'est réellement : les événements du proxy y ajoutent et en retirent des
 * joueurs, le planificateur la parcourt pendant le décompte et la vide pendant
 * les téléportations, et le rappel de surveillance l'efface quand le serveur
 * disparaît. Une liste ordinaire lèverait une {@code ConcurrentModificationException}
 * dès qu'un parcours croise une écriture — un incident rare, dépendant du
 * hasard, et donc pénible à reproduire en production.
 */
public class MinecraftServerQueueConcurrencyTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private LoggerProxy mockLogger;

    @Mock
    private PanelServer mockPanelServer;

    @Mock
    private CommonProxy mockProxy;

    private MinecraftServer minecraftServer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());

        minecraftServer = new MinecraftServer(mockPlugin, new MockCommonServer("survie"),
                mockPanelServer, mock(ServerMotd.class));
    }

    /**
     * Un thread ajoute et retire pendant qu'un autre parcourt : aucun des deux
     * ne doit échouer.
     */
    @Test
    @Timeout(30)
    public void testQueueSurvivesConcurrentWritesAndReads() throws Exception {
        // Une file deja fournie : le parcours dure alors assez longtemps pour
        // croiser une ecriture. Avec une poignee de joueurs, la fenetre est trop
        // etroite pour que le probleme se manifeste de facon fiable.
        for (int i = 0; i < 500; i++) {
            minecraftServer.enqueue(new MockCommonPlayer("Present" + i));
        }

        int rounds = 3000;
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(4);

        Runnable writer = () -> {
            try {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    CommonPlayer player = new MockCommonPlayer("Passager" + i);
                    minecraftServer.enqueue(player);
                    minecraftServer.dequeue(player);
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        };

        Runnable reader = () -> {
            try {
                start.await();
                for (int i = 0; i < rounds; i++) {
                    // Ce que fait le decompte a chaque seconde.
                    for (CommonPlayer player : List.copyOf(minecraftServer.getQueue())) {
                        player.getUniqueId();
                    }
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        };

        pool.submit(writer);
        pool.submit(writer);
        pool.submit(reader);
        pool.submit(reader);
        start.countDown();
        pool.shutdown();

        assertTrue(pool.awaitTermination(25, TimeUnit.SECONDS), "Les threads doivent terminer");
        if (failure.get() != null) {
            fail("Acces concurrent en echec : " + failure.get());
        }
        assertEquals(500, minecraftServer.getQueue().size(),
                "Chaque ajout de passage ayant son retrait, seuls les presents restent");
    }

    /**
     * L'unicité doit tenir même si deux threads placent le même joueur au même
     * instant — un joueur qui insiste sur deux connexions, par exemple.
     */
    @Test
    @Timeout(20)
    public void testConcurrentEnqueuesOfTheSamePlayerAddItOnce() throws Exception {
        java.util.UUID uuid = java.util.UUID.randomUUID();
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                minecraftServer.enqueue(new MockCommonPlayer("Vivi", uuid));
                return null;
            });
        }
        start.countDown();
        pool.shutdown();

        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));
        assertEquals(1, minecraftServer.getQueue().size(),
                "Le joueur ne doit figurer qu'une fois, quel que soit l'entrelacement");
    }
}
