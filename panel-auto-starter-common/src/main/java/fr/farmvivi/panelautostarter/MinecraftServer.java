package fr.farmvivi.panelautostarter;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import fr.farmvivi.panelautostarter.common.ping.CommonServerPing;
import fr.farmvivi.panelautostarter.message.MessageSettings;
import fr.farmvivi.panelautostarter.message.SoundSpec;
import fr.farmvivi.panelautostarter.message.Messages;
import fr.farmvivi.panelautostarter.motd.ServerMotd;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelServerState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class MinecraftServer {
    private final PanelAutoStarter plugin;

    private final CommonServer server;
    private final PanelServer panelServer;
    private final ServerMotd motd;
    /**
     * File d'attente du serveur.
     * <p>
     * Elle est lue et modifiee depuis trois threads : les evenements du proxy
     * quand un joueur demande le serveur ou se deconnecte, le planificateur
     * pendant le decompte et les teleportations, et le rappel de surveillance
     * quand le serveur disparait. D'ou une liste a copie sur ecriture, qui rend
     * les parcours surs sans verrouiller les lectures ; les operations qui
     * lisent puis ecrivent restent, elles, synchronisees.
     */
    private final List<CommonPlayer> queue = new CopyOnWriteArrayList<>();
    private MinecraftServerStatus status = MinecraftServerStatus.OFFLINE;

    /**
     * Dernier ping vu par la machine à états. Écrit uniquement par le scheduler,
     * qui en est l'unique proprietaire : c'est la memoire du "ping precedent" qui
     * permet de detecter les transitions en ligne / hors-ligne. Il ne doit jamais
     * etre touche par le rafraichissement a la demande, sinon une transition peut
     * etre manquee.
     */
    private CommonServerPing lastPolledPing;

    /**
     * Cache de presentation, servi aux requetes de ping des clients. Alimente par
     * le scheduler et par le rafraichissement a la demande, lu depuis les threads
     * d'evenements : d'ou le volatile.
     */
    private volatile CommonServerPing cachedPing;
    private volatile long cachedPingAt = 0;

    /**
     * Date du dernier ping client portant sur ce serveur, utilisee pour espacer
     * la surveillance des serveurs que personne ne regarde.
     */
    private volatile long lastWatchedAt = 0;

    /**
     * Empeche N pings clients simultanes de declencher N requetes vers le backend.
     */
    private final AtomicBoolean refreshInFlight = new AtomicBoolean(false);

    /**
     * Empeche deux sequences de teleportation de se chevaucher : la boucle de
     * surveillance passe regulierement et relancerait sinon un decompte a
     * chaque sondage.
     */
    private final AtomicBoolean teleportSequenceRunning = new AtomicBoolean(false);
    private volatile int countdownRemaining = 0;

    /**
     * Un arret a ete demande et le serveur n'est pas revenu en ligne depuis.
     * Permet de distinguer une ejection due a l'extinction du serveur d'une
     * exclusion volontaire, alors meme que la surveillance n'a pas encore
     * constate la disparition.
     */
    private volatile boolean stopRequested = false;

    private long lastBusyTime = System.currentTimeMillis();
    private long serverStartedTime = 0;
    private long lastTeleportTime = 0;

    public MinecraftServer(PanelAutoStarter plugin, CommonServer server, PanelServer panelServer, ServerMotd motd) {
        this.plugin = plugin;
        this.server = server;
        this.panelServer = panelServer;
        this.motd = motd;

        scheduleServerStatusCheck();
    }

    /**
     * Retourne le MOTD conservé pour ce serveur, réutilisé quand il n'est plus
     * en ligne.
     *
     * @return le MOTD du serveur
     */
    public ServerMotd getMotd() {
        return motd;
    }

    /**
     * Scheduler adaptatif : utilise un refresh rate plus rapide pendant le démarrage
     */
    private void scheduleServerStatusCheck() {
        checkServerStatusAndReschedule();
    }

    private void checkServerStatusAndReschedule() {
        // Faire le check
        server.ping(result -> {
            long curMillis = System.currentTimeMillis();
            storePing(result, curMillis);
            updateServerStatus(result, curMillis);

            // Le MOTD n'est conserve que depuis la surveillance, jamais depuis un
            // rafraichissement declenche par un client : un seul ecrivain, et
            // aucune ecriture disque sur le chemin d'un ping.
            if (status.equals(MinecraftServerStatus.ONLINE)) {
                motd.observeOnline(result);
            }

            handleQueueAndShutdown(result, curMillis);

            // Re-scheduler : l'intervalle est calcule apres la mise a jour du
            // statut, pour qu'il reflete l'etat courant et non le precedent.
            this.plugin.getProxy().schedule(plugin.getPlugin(),
                this::checkServerStatusAndReschedule,
                resolveCheckInterval(curMillis),
                TimeUnit.SECONDS);
        });
    }

    /**
     * Choisit la frequence de surveillance selon l'etat du serveur et selon que
     * quelqu'un le regarde ou non.
     * <p>
     * La surveillance ne peut pas etre supprimee au profit du seul
     * rafraichissement a la demande : c'est elle qui pilote l'arret automatique
     * apres inactivite et le vidage de la file d'attente. En revanche, un serveur
     * eteint que personne ne ping n'a pas besoin d'etre interroge toutes les 15
     * secondes.
     *
     * @param curMillis l'instant courant
     * @return l'intervalle en secondes avant la prochaine verification
     */
    private long resolveCheckInterval(long curMillis) {
        if (status.equals(MinecraftServerStatus.STARTING)) {
            return plugin.getConfig().getLong("server-start.check-interval-startup", 3);
        }
        if (status.equals(MinecraftServerStatus.OFFLINE) && !isWatched(curMillis)) {
            return plugin.getConfig().getLong("server-start.check-interval-idle", 60);
        }
        return plugin.getConfig().getLong("server-start.check-interval-normal", 15);
    }

    private boolean isWatched(long curMillis) {
        long idleThreshold = plugin.getConfig().getLong("server-start.idle-threshold", 300) * 1000;
        return lastWatchedAt > 0 && curMillis - lastWatchedAt < idleThreshold;
    }

    /**
     * Met a jour le cache de presentation. Volontairement distinct de la machine
     * a etats.
     */
    private void storePing(CommonServerPing result, long at) {
        this.cachedPing = result;
        this.cachedPingAt = at;
    }

    /**
     * Signale qu'un client vient de pinger ce serveur. Sert uniquement a decider
     * de la frequence de surveillance.
     */
    public void notifyWatched() {
        this.lastWatchedAt = System.currentTimeMillis();
    }

    /**
     * Retourne le dernier ping connu du serveur, en declenchant un
     * rafraichissement en arriere-plan si la donnee est perimee.
     * <p>
     * <strong>Ne bloque jamais</strong> : la valeur en cache est rendue
     * immediatement et le rafraichissement profite au ping suivant. Attendre la
     * reponse du backend ajouterait sa latence a chaque ping client et
     * offrirait un levier d'abus trivial.
     * <p>
     * Le rafraichissement ne met a jour que le cache : il ne touche pas au
     * statut, a la file d'attente ni aux compteurs d'inactivite, qui restent
     * pilotes par le scheduler seul.
     *
     * @return le dernier ping connu, ou null si aucun n'a encore abouti
     */
    public CommonServerPing peekServerPing() {
        long curMillis = System.currentTimeMillis();
        long cacheTtl = plugin.getConfig().getLong("server-start.ping-cache-ttl", 5) * 1000;

        if (curMillis - cachedPingAt >= cacheTtl && refreshInFlight.compareAndSet(false, true)) {
            try {
                server.ping(result -> {
                    try {
                        storePing(result, System.currentTimeMillis());
                    } finally {
                        refreshInFlight.set(false);
                    }
                });
            } catch (RuntimeException ex) {
                // Sans ce rattrapage, un echec synchrone laisserait le drapeau leve
                // et bloquerait definitivement tout rafraichissement ulterieur.
                refreshInFlight.set(false);
                throw ex;
            }
        }

        return cachedPing;
    }

    private void updateServerStatus(CommonServerPing result, long curMillis) {
        if (lastPolledPing == null && result != null) {
            if (panelServer.retrieveState() == PanelServerState.STARTING) {
                return;
            }
            status = MinecraftServerStatus.ONLINE;
            stopRequested = false;
            serverStartedTime = curMillis;  // Enregistrer l'heure de démarrage
            logServerStatus("en ligne", NamedTextColor.GREEN);
        } else if (shouldSetOffline(result, curMillis)) {
            status = MinecraftServerStatus.OFFLINE;
            logServerStatus("hors-ligne", NamedTextColor.RED);
            clearQueue();
            serverStartedTime = 0;
        }
        lastPolledPing = result;
    }

    private boolean shouldSetOffline(CommonServerPing result, long curMillis) {
        return (lastPolledPing != null && result == null && !status.equals(MinecraftServerStatus.STARTING)) ||
                (status.equals(MinecraftServerStatus.STARTING) && lastBusyTime < curMillis);
    }

    private void logServerStatus(String statusMessage, NamedTextColor color) {
        plugin.getLogger().info(Component.text(server.getName()).color(NamedTextColor.YELLOW)
                .append(Component.text(" " + statusMessage).color(color)));
    }

    private void clearQueue() {
        // Le serveur n'est plus la : toute sequence de teleportation en cours
        // n'a plus d'objet.
        countdownRemaining = 0;
        teleportSequenceRunning.set(false);

        // La liste est vidée avant d'écrire aux joueurs : un parcours par
        // iterateur ne peut pas retirer d'element d'une liste a copie sur
        // ecriture, et prevenir d'abord laisserait la file dans un etat
        // incoherent le temps des envois.
        List<CommonPlayer> waiting = List.copyOf(queue);
        queue.clear();
        for (CommonPlayer player : waiting) {
            plugin.getLogger().warning("Impossible de téléporter " + player.getUsername() + " sur " + server.getName());
            player.sendMessage(Messages.teleportFailed(server.getDisplayName()));
        }
    }

    private void handleQueueAndShutdown(CommonServerPing result, long curMillis) {
        if (status.equals(MinecraftServerStatus.ONLINE)) {
            maybeStartTeleportSequence(curMillis);
            updateLastBusyTime(result, curMillis);
            if (curMillis - lastBusyTime > 5 * 60 * 1000) {
                stop();
            }
        }
    }

    /**
     * Démarre la séquence de téléportation si elle n'est pas déjà en cours.
     * <p>
     * Le décompte puis les téléportations sont menés par des tâches planifiées
     * à la seconde, et non par la boucle de surveillance : celle-ci ne passe que
     * toutes les 15 secondes, ce qui rendait jusqu'ici {@code teleport-delay}
     * illusoire — la file se vidait d'un joueur par sondage.
     *
     * @param curMillis l'instant courant
     */
    private void maybeStartTeleportSequence(long curMillis) {
        if (queue.isEmpty()) {
            return;
        }

        // Laisser au serveur le temps d'etre reellement pret avant d'annoncer
        // quoi que ce soit au joueur.
        long waitBeforeTeleport = plugin.getConfig().getLong("server-start.wait-before-teleport", 5) * 1000;
        if (curMillis - serverStartedTime < waitBeforeTeleport) {
            return;
        }

        if (!teleportSequenceRunning.compareAndSet(false, true)) {
            return;
        }

        MessageSettings.CountdownSettings countdown = plugin.getMessageSettings().getCountdown();
        if (countdown.enabled() && countdown.seconds() > 0) {
            countdownRemaining = countdown.seconds();
            tickCountdown();
        } else {
            drainQueue();
        }
    }

    /**
     * Affiche une seconde du décompte à toute la file, puis planifie la
     * suivante.
     */
    private void tickCountdown() {
        if (!status.equals(MinecraftServerStatus.ONLINE)) {
            teleportSequenceRunning.set(false);
            return;
        }

        int remaining = countdownRemaining;
        if (remaining <= 0) {
            announceDeparture();
            drainQueue();
            return;
        }

        MessageSettings.TitleSettings title = plugin.getMessageSettings().getTitle();
        SoundSpec tickSound = plugin.getMessageSettings().getCountdown().tickSound();
        for (CommonPlayer player : List.copyOf(queue)) {
            // Le titre reste affiche jusqu'au tic suivant, sans clignoter.
            player.showTitle(Messages.countdownTitle(remaining),
                    Messages.countdownSubtitle(server.getDisplayName()),
                    Duration.ZERO, Duration.ofSeconds(1), title.fadeOut());
            playSound(player, tickSound);
        }

        countdownRemaining = remaining - 1;
        plugin.getProxy().schedule(plugin.getPlugin(), this::tickCountdown, 1, TimeUnit.SECONDS);
    }

    private void announceDeparture() {
        MessageSettings.TitleSettings title = plugin.getMessageSettings().getTitle();
        SoundSpec goSound = plugin.getMessageSettings().getCountdown().goSound();
        for (CommonPlayer player : List.copyOf(queue)) {
            player.showTitle(Messages.readyTitle(), Component.empty(),
                    Duration.ZERO, Duration.ofMillis(800), title.fadeOut());
            playSound(player, goSound);
        }
    }

    private void playSound(CommonPlayer player, SoundSpec sound) {
        if (sound != null && !sound.isSilent()) {
            player.playSound(sound.name(), sound.volume(), sound.pitch());
        }
    }

    /**
     * Téléporte un joueur, puis se replanifie tant que la file n'est pas vide.
     * <p>
     * L'espacement évite d'envoyer toute une file d'un bloc sur un serveur qui
     * vient à peine de démarrer.
     */
    private void drainQueue() {
        if (!status.equals(MinecraftServerStatus.ONLINE)) {
            teleportSequenceRunning.set(false);
            return;
        }

        // Lire puis retirer doit etre indivisible : un joueur peut rejoindre ou
        // quitter la file depuis un thread d'evenement entre les deux.
        CommonPlayer next;
        synchronized (this) {
            // Un joueur deja arrive sur place n'a plus rien a faire dans la file.
            queue.removeIf(player -> server.equals(player.getServer()));

            if (queue.isEmpty()) {
                teleportSequenceRunning.set(false);
                return;
            }
            next = queue.remove(0);
        }

        teleportPlayer(next);
        lastTeleportTime = System.currentTimeMillis();

        if (queue.isEmpty()) {
            teleportSequenceRunning.set(false);
            return;
        }

        long teleportDelay = plugin.getConfig().getLong("server-start.teleport-delay", 1);
        plugin.getProxy().schedule(plugin.getPlugin(), this::drainQueue, teleportDelay, TimeUnit.SECONDS);
    }

    private void teleportPlayer(CommonPlayer player) {
        plugin.getLogger().info("Téléportation de " + player.getUsername() + " sur " + server.getName());
        player.sendMessage(Messages.teleporting(server.getDisplayName()));
        player.connectToServer(server);
    }

    private void updateLastBusyTime(CommonServerPing result, long curMillis) {
        if (result != null && result.getOnlinePlayers() > 0 && curMillis > lastBusyTime) {
            lastBusyTime = curMillis;
        }
    }

    public synchronized void start() {
        if (!status.equals(MinecraftServerStatus.OFFLINE)) {
            return;
        }
        this.plugin.getLogger().info("Démarrage de " + server.getName() + "...");
        this.panelServer.start();
        this.lastBusyTime = System.currentTimeMillis() + 15 * 60 * 1000;
        this.status = MinecraftServerStatus.STARTING;
        this.lastTeleportTime = 0;  // Réinitialiser le timer de téléportation
    }

    public synchronized void stop() {
        if (!status.equals(MinecraftServerStatus.ONLINE)) {
            return;
        }
        this.plugin.getLogger().info("Arrêt de " + server.getName() + "...");
        this.stopRequested = true;
        this.panelServer.stop();
        this.lastBusyTime = System.currentTimeMillis();
    }

    /**
     * Indique si le serveur est en train de s'éteindre ou n'est plus joignable.
     * <p>
     * L'arrêt demandé compte immédiatement : la surveillance ne passe que toutes
     * les quinze secondes, et les joueurs sont éjectés bien avant qu'elle ne
     * constate la disparition.
     *
     * @return true si le serveur n'est pas en état de recevoir des joueurs
     */
    public boolean isShuttingDownOrDown() {
        return stopRequested || !status.equals(MinecraftServerStatus.ONLINE);
    }

    public CommonServer getServer() {
        return server;
    }

    public List<CommonPlayer> getQueue() {
        return queue;
    }

    /**
     * Place un joueur dans la file, sans l'y mettre deux fois.
     * <p>
     * Rien n'empêche un joueur de redemander le même serveur — une commande
     * répétée, un clic insistant — et chaque demande passe par le même chemin.
     * Sans cette garde, il s'ajoutait autant de fois qu'il insistait, gonflant
     * la file et faussant les positions annoncées à tout le monde.
     * <p>
     * La comparaison se fait sur l'identifiant du joueur et non sur l'objet :
     * rien ne garantit qu'une même connexion soit toujours représentée par la
     * même instance.
     *
     * @param player le joueur
     * @return true s'il vient d'être ajouté, false s'il y était déjà
     */
    public synchronized boolean enqueue(CommonPlayer player) {
        if (player == null || isQueued(player)) {
            return false;
        }
        queue.add(player);
        return true;
    }

    /**
     * Retire un joueur de la file, quelle que soit l'instance qui le
     * représente.
     *
     * @param player le joueur
     */
    public synchronized void dequeue(CommonPlayer player) {
        if (player != null) {
            queue.removeIf(queued -> queued.getUniqueId().equals(player.getUniqueId()));
        }
    }

    /**
     * Indique si un joueur attend déjà ce serveur.
     *
     * @param player le joueur
     * @return true s'il est dans la file
     */
    public boolean isQueued(CommonPlayer player) {
        if (player == null) {
            return false;
        }
        return queue.stream().anyMatch(queued -> queued.getUniqueId().equals(player.getUniqueId()));
    }

    public MinecraftServerStatus getStatus() {
        return status;
    }

    /**
     * Indique qu'un décompte ou une vague de téléportations est en cours.
     * <p>
     * Ces deux moments occupent déjà l'écran du joueur avec un titre ; la barre
     * d'action s'efface alors pour ne pas répéter la même chose en deux
     * endroits.
     *
     * @return true si la séquence de téléportation est en cours
     */
    public boolean isTeleportSequenceRunning() {
        return teleportSequenceRunning.get();
    }

    /**
     * Retourne le dernier ping connu sans declencher de rafraichissement.
     *
     * @return le ping en cache, ou null si aucun n'a encore abouti
     * @see #peekServerPing() pour la variante qui rafraichit un cache perime
     */
    public CommonServerPing getServerPing() {
        return cachedPing;
    }
}
