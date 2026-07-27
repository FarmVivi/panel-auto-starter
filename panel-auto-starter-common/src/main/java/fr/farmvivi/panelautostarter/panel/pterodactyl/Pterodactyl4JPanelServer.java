package fr.farmvivi.panelautostarter.panel.pterodactyl;

import com.mattmalec.pterodactyl4j.UtilizationState;
import com.mattmalec.pterodactyl4j.client.entities.ClientServer;
import fr.farmvivi.panelautostarter.panel.PanelServer;
import fr.farmvivi.panelautostarter.panel.PanelServerState;

/**
 * Implémentation de {@link PanelServer} adossée à Pterodactyl4J.
 * <p>
 * Les méthodes sont volontairement surchargeables : une divergence d'API côté
 * Pelican se traite en surchargeant la méthode concernée dans une sous-classe.
 */
public class Pterodactyl4JPanelServer implements PanelServer {
    private final String identifier;
    private final ClientServer delegate;

    public Pterodactyl4JPanelServer(String identifier, ClientServer delegate) {
        this.identifier = identifier;
        this.delegate = delegate;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public PanelServerState retrieveState() {
        return toPanelServerState(delegate.retrieveUtilization().execute().getState());
    }

    @Override
    public void start() {
        delegate.start().executeAsync();
    }

    @Override
    public void stop() {
        delegate.stop().executeAsync();
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
     * Donne accès au serveur Pterodactyl4J sous-jacent aux sous-classes.
     *
     * @return le serveur délégué
     */
    protected ClientServer getDelegate() {
        return delegate;
    }
}
