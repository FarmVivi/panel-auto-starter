package fr.farmvivi.panelautostarter.listener;

import fr.farmvivi.panelautostarter.MinecraftServer;
import fr.farmvivi.panelautostarter.PanelAutoStarter;
import fr.farmvivi.panelautostarter.PendingNotifications;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.event.PlayerKickedEvent;
import fr.farmvivi.panelautostarter.mocks.MockCommonPlayer;
import fr.farmvivi.panelautostarter.mocks.MockCommonServer;
import fr.farmvivi.panelautostarter.message.Messages;
import fr.farmvivi.panelautostarter.message.SoundSpec;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests du rattrapage des joueurs éjectés d'un serveur géré.
 * <p>
 * Le plugin impose la destination plutôt que de laisser le proxy décider : selon
 * la façon dont un serveur coupe la connexion, celui-ci renvoie le joueur
 * ailleurs ou le déconnecte, d'où un comportement erratique à l'arrêt d'un
 * serveur.
 */
public class PlayerKickedEventListenerTest {

    @Mock
    private PanelAutoStarter mockPlugin;

    @Mock
    private CommonProxy mockProxy;

    @Mock
    private Configuration mockConfiguration;

    @Mock
    private MinecraftServer mockManagedServer;

    private CommonServer queueServer;
    private CommonServer managedServer;
    private CommonServer unmanagedServer;
    private MockCommonPlayer player;
    private PlayerKickedEventListener listener;
    private PendingNotifications pendingNotifications;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        queueServer = new MockCommonServer("limbo");
        managedServer = new MockCommonServer("survie", "Survie");
        unmanagedServer = new MockCommonServer("creatif", "Creatif");
        player = new MockCommonPlayer("Vivi");

        pendingNotifications = new PendingNotifications();
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.defaults());
        when(mockPlugin.getConfig()).thenReturn(mockConfiguration);
        when(mockPlugin.getProxy()).thenReturn(mockProxy);
        when(mockConfiguration.getBoolean(eq("queue.catch-kicks"), anyBoolean())).thenReturn(true);
        when(mockConfiguration.getString("queue.server")).thenReturn("limbo");
        when(mockConfiguration.getStringList("queue.shutdown-reasons"))
                .thenReturn(java.util.List.of("Server closed"));
        // Par defaut les tests simulent un serveur qui s'eteint ; les cas
        // d'exclusion volontaire le redressent explicitement.
        when(mockManagedServer.isShuttingDownOrDown()).thenReturn(true);
        when(mockProxy.getServer("limbo")).thenReturn(queueServer);

        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(managedServer, mockManagedServer);
        doReturn(servers).when(mockPlugin).getServers();

        listener = new PlayerKickedEventListener(mockPlugin, pendingNotifications);
    }

    private PlayerKickedEvent kickedFrom(CommonServer from) {
        return kickedFrom(from, Component.text("Server closed"));
    }

    private PlayerKickedEvent kickedFrom(CommonServer from, Component reason) {
        PlayerKickedEvent event = new PlayerKickedEvent(player, from, reason);
        listener.onPlayerKicked(event);
        return event;
    }

    /** Le serveur tourne toujours : le plugin ne peut pas conclure a un arret. */
    private void givenServerStillRunning() {
        when(mockManagedServer.isShuttingDownOrDown()).thenReturn(false);
    }

    @Test
    public void testPlayerKickedFromManagedServerGoesToTheQueueServer() {
        PlayerKickedEvent event = kickedFrom(managedServer);

        assertSame(queueServer, event.getRedirectTo(),
                "Le joueur doit etre renvoye vers le serveur d'attente");
    }

    /**
     * Le plugin n'a pas à s'immiscer dans le sort des joueurs d'un serveur
     * qu'il ne gère pas.
     */
    @Test
    public void testPlayerKickedFromUnmanagedServerIsLeftToTheProxy() {
        PlayerKickedEvent event = kickedFrom(unmanagedServer);

        assertNull(event.getRedirectTo());
    }

    /**
     * Renvoyer vers le serveur dont le joueur vient d'être éjecté l'enfermerait
     * dans une boucle de reconnexions.
     */
    @Test
    public void testKickFromTheQueueServerIsNotRedirectedToItself() {
        Map<CommonServer, MinecraftServer> servers = new HashMap<>();
        servers.put(queueServer, mockManagedServer);
        doReturn(servers).when(mockPlugin).getServers();

        PlayerKickedEvent event = kickedFrom(queueServer);

        assertNull(event.getRedirectTo(),
                "Aucune redirection ne doit boucler sur le serveur d'origine");
    }

    @Test
    public void testDisabledByConfiguration() {
        when(mockConfiguration.getBoolean(eq("queue.catch-kicks"), anyBoolean())).thenReturn(false);

        PlayerKickedEvent event = kickedFrom(managedServer);

        assertNull(event.getRedirectTo());
    }

    @Test
    public void testMissingQueueServerLeavesTheProxyInCharge() {
        when(mockProxy.getServer("limbo")).thenReturn(null);

        PlayerKickedEvent event = kickedFrom(managedServer);

        assertNull(event.getRedirectTo(),
                "Sans serveur d'attente joignable, mieux vaut ne rien imposer");
    }

    @Test
    public void testUnknownOriginIsIgnored() {
        PlayerKickedEvent event = kickedFrom(null);

        assertNull(event.getRedirectTo());
    }

    /**
     * La raison annoncée par le serveur est conservée sur l'événement : les
     * adaptateurs de plateforme la transmettent au joueur, pour qu'une exclusion
     * volontaire ne soit pas masquée par la redirection.
     */
    @Test
    public void testKickReasonIsPreserved() {
        PlayerKickedEvent event = kickedFrom(managedServer);

        assertEquals(Component.text("Server closed"), event.getReason());
    }

    // ===================== Retour au joueur =====================

    /**
     * Le joueur est en cours de transfert au moment de l'éjection : lui parler
     * tout de suite ne produirait rien. Le retour est donc mis de côté et
     * délivré à son arrivée sur le serveur d'attente.
     */
    @Test
    public void testFeedbackIsDeferredUntilThePlayerLands() {
        kickedFrom(managedServer);

        assertNull(player.getLastMessage(), "Rien ne doit partir pendant le transfert");
        assertEquals(1, pendingNotifications.size(), "Le retour doit etre mis de cote");

        pendingNotifications.flush(player);

        assertNotNull(player.getLastMessage(), "Le chat doit etre delivre a l'arrivee");
        assertEquals(Messages.serverStoppedTitle(), player.getLastTitle());
        assertTrue(player.getPlayedSounds().contains(
                        MessageSettings.defaults().getKickFeedback().sound().name()),
                "Le son doit accompagner le retour");
    }

    @Test
    public void testFeedbackNamesBothServers() {
        kickedFrom(managedServer);
        pendingNotifications.flush(player);

        String chat = player.getLastMessage().toString();
        assertTrue(chat.contains("Survie"), "Le serveur disparu doit etre nomme");
        assertTrue(chat.contains("limbo"), "Le serveur d'accueil doit etre nomme");
    }

    @Test
    public void testFeedbackCanBeDisabled() {
        when(mockPlugin.getMessageSettings()).thenReturn(MessageSettings.of(
                MessageSettings.defaults().getTitle(),
                MessageSettings.defaults().getCountdown(),
                new MessageSettings.KickFeedbackSettings(false, SoundSpec.SILENT)));

        PlayerKickedEvent event = kickedFrom(managedServer);

        assertSame(queueServer, event.getRedirectTo(), "La redirection reste active");
        assertEquals(0, pendingNotifications.size(), "Mais aucun retour n'est prepare");
    }

    /**
     * Un serveur non géré n'est pas redirigé, donc aucun retour ne doit être
     * préparé non plus.
     */
    @Test
    public void testNoFeedbackWhenNothingIsRedirected() {
        kickedFrom(unmanagedServer);

        assertEquals(0, pendingNotifications.size());
    }

    // ===================== Arrêt contre exclusion volontaire =====================

    /**
     * Le cas qui motive toute cette distinction : un joueur expulsé par un
     * administrateur doit rester dehors et lire son motif, pas se retrouver
     * tranquillement déposé au lobby.
     */
    @Test
    public void testDeliberateKickIsNotCaught() {
        givenServerStillRunning();

        PlayerKickedEvent event = kickedFrom(managedServer,
                Component.text("Comportement inapproprie"));

        assertNull(event.getRedirectTo(), "Une exclusion volontaire ne doit pas etre rattrapee");
        assertEquals(0, pendingNotifications.size(), "Ni aucun retour prepare");
    }

    /**
     * Le plugin sait qu'il a demandé l'arrêt, ou que le serveur ne répond plus.
     * C'est l'indice le plus sûr, il prime sur le motif.
     */
    @Test
    public void testKnownShutdownIsCaughtWhateverTheReason() {
        PlayerKickedEvent event = kickedFrom(managedServer,
                Component.text("Un motif qui ne ressemble a rien"));

        assertSame(queueServer, event.getRedirectTo());
    }

    /**
     * Une connexion coupée sans un mot est le signe d'un plantage.
     */
    @Test
    public void testKickWithoutReasonIsTreatedAsAFailure() {
        givenServerStillRunning();

        PlayerKickedEvent event = kickedFrom(managedServer, null);

        assertSame(queueServer, event.getRedirectTo());
    }

    @Test
    public void testEmptyReasonIsTreatedAsAFailure() {
        givenServerStillRunning();

        PlayerKickedEvent event = kickedFrom(managedServer, Component.text("   "));

        assertSame(queueServer, event.getRedirectTo());
    }

    /**
     * Un arrêt lancé depuis le panel annonce « Server closed » avant que la
     * surveillance n'ait rien constaté : le motif est alors le seul indice.
     */
    @Test
    public void testConfiguredShutdownReasonIsCaught() {
        givenServerStillRunning();

        PlayerKickedEvent event = kickedFrom(managedServer,
                Component.text("Server closed"));

        assertSame(queueServer, event.getRedirectTo());
    }

    @Test
    public void testShutdownReasonMatchingIsCaseInsensitiveAndPartial() {
        givenServerStillRunning();

        PlayerKickedEvent event = kickedFrom(managedServer,
                Component.text("[Panel] SERVER CLOSED by admin"));

        assertSame(queueServer, event.getRedirectTo());
    }

    /**
     * Les couleurs du motif ne doivent pas empêcher la comparaison.
     */
    @Test
    public void testColouredReasonIsStillMatched() {
        givenServerStillRunning();

        PlayerKickedEvent event = kickedFrom(managedServer,
                Component.text("Server closed", net.kyori.adventure.text.format.NamedTextColor.RED));

        assertSame(queueServer, event.getRedirectTo());
    }
}
