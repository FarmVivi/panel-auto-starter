package fr.farmvivi.panelautostarter.panel.pterodactyl;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import com.mattmalec.pterodactyl4j.client.entities.PteroClient;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelServerState;

import java.util.function.Consumer;

/**
 * Implémentation de {@link PanelServer} adossée à Pterodactyl4J.
 * <p>
 * Les méthodes sont volontairement surchargeables : une divergence d'API côté
 * Pelican se traite en surchargeant la méthode concernée dans une sous-classe.
 * <p>
 * Le serveur Pterodactyl4J n'est pas résolu à la construction mais à la première
 * utilisation, et seule une résolution réussie est retenue. Il l'était autrefois
 * au démarrage du proxy, une fois pour toutes : Pterodactyl4J renvoie
 * {@code null} quand la requête échoue, ce {@code null} était gardé, et un panel
 * injoignable pendant quelques secondes au démarrage rendait le serveur
 * impossible à démarrer jusqu'au redémarrage suivant du proxy.
 */
public class Pterodactyl4JPanelServer implements PanelServer {
    private final String identifier;
    private final PteroClient client;
    // volatile suffit : deux résolutions concurrentes donnent deux objets
    // équivalents, et en garder un au hasard est sans conséquence.
    private volatile ClientServer delegate;

    public Pterodactyl4JPanelServer(String identifier, PteroClient client) {
        this.identifier = identifier;
        this.client = client;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public PanelServerState retrieveState() {
        return toPanelServerState(getDelegate().retrieveUtilization().execute().getState());
    }

    @Override
    public void start() {
        withDelegate(server -> server.start().executeAsync());
    }

    @Override
    public void stop() {
        withDelegate(server -> server.stop().executeAsync());
    }

    /**
     * Traduit l'état Pterodactyl4J vers l'état exposé par l'abstraction.
     * <p>
     * Un état inconnu est mappé sur {@link PanelServerState#UNKNOWN} plutôt que
     * de lever : si un panel introduit un nouvel état, le plugin doit continuer
     * à tourner plutôt que de casser la boucle de surveillance.
     *
     * @param state l'état renvoyé par la bibliothèque, éventuellement null
     * @return l'état correspondant
     */
    protected PanelServerState toPanelServerState(UtilizationState state) {
        if (state == null) {
            return PanelServerState.UNKNOWN;
        }
        return switch (state) {
            case OFFLINE -> PanelServerState.OFFLINE;
            case STARTING -> PanelServerState.STARTING;
            case RUNNING -> PanelServerState.RUNNING;
            case STOPPING -> PanelServerState.STOPPING;
        };
    }

    /**
     * Donne accès au serveur Pterodactyl4J sous-jacent, en le résolvant s'il ne
     * l'est pas encore.
     * <p>
     * Appel <strong>bloquant</strong> à la première utilisation. Un échec lève
     * une exception plutôt que de renvoyer {@code null} : l'appelant de
     * {@link #retrieveState()} traite déjà un panel injoignable, et le tour
     * suivant réessaiera.
     *
     * @return le serveur délégué
     * @throws IllegalStateException si le panel ne renvoie pas le serveur
     */
    protected ClientServer getDelegate() {
        ClientServer resolved = delegate;
        if (resolved == null) {
            resolved = client.retrieveServerByIdentifier(identifier).execute();
            if (resolved == null) {
                throw new IllegalStateException("le panel n'a pas renvoye le serveur " + identifier);
            }
            delegate = resolved;
        }
        return resolved;
    }

    /**
     * Applique une action au serveur délégué sans jamais bloquer : s'il n'est pas
     * encore résolu, la résolution part elle aussi en asynchrone.
     * <p>
     * {@link #start()} et {@link #stop()} sont appelés depuis des événements du
     * proxy ; une résolution bloquante y ajouterait un aller-retour vers le
     * panel. Un échec est journalisé par Pterodactyl4J lui-même, comme l'était
     * déjà celui d'un démarrage, et la prochaine demande réessaiera.
     *
     * @param action l'action à appliquer
     */
    private void withDelegate(Consumer<ClientServer> action) {
        ClientServer resolved = delegate;
        if (resolved != null) {
            action.accept(resolved);
            return;
        }
        client.retrieveServerByIdentifier(identifier).executeAsync(server -> {
            if (server != null) {
                delegate = server;
                action.accept(server);
            }
        });
    }
}
