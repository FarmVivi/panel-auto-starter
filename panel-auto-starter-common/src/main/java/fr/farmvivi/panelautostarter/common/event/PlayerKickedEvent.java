package fr.farmvivi.panelautostarter.common.event;

import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonServer;
import net.kyori.adventure.text.Component;

/**
 * Déclenché quand un joueur est éjecté d'un serveur, notamment lorsque
 * celui-ci s'arrête.
 * <p>
 * Sans intervention, le proxy décide seul du sort du joueur : selon la manière
 * dont le serveur a coupé la connexion, il est renvoyé ailleurs ou purement
 * déconnecté. Cet événement permet d'imposer une destination.
 */
public class PlayerKickedEvent extends Event {
    private final CommonPlayer player;
    private final CommonServer from;
    private final Component reason;
    private CommonServer redirectTo;

    public PlayerKickedEvent(CommonPlayer player, CommonServer from, Component reason) {
        this.player = player;
        this.from = from;
        this.reason = reason;
    }

    public CommonPlayer getPlayer() {
        return player;
    }

    /**
     * Retourne le serveur dont le joueur a été éjecté.
     *
     * @return le serveur d'origine, éventuellement null
     */
    public CommonServer getFrom() {
        return from;
    }

    /**
     * Retourne la raison annoncée par le serveur.
     *
     * @return la raison, éventuellement null
     */
    public Component getReason() {
        return reason;
    }

    /**
     * Retourne la destination imposée, s'il y en a une.
     *
     * @return le serveur de repli, ou null pour laisser le proxy décider
     */
    public CommonServer getRedirectTo() {
        return redirectTo;
    }

    /**
     * Impose une destination plutôt que de laisser le joueur être déconnecté.
     *
     * @param redirectTo le serveur de repli
     */
    public void setRedirectTo(CommonServer redirectTo) {
        this.redirectTo = redirectTo;
    }
}
