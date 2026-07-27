package fr.farmvivi.panelautostarter.panel;

/**
 * État d'un serveur tel que rapporté par le panel.
 * <p>
 * À ne pas confondre avec {@link fr.farmvivi.panelautostarter.MinecraftServerStatus},
 * qui est l'état <em>métier</em> déduit du ping Minecraft. Cet enum-ci reflète
 * uniquement ce que le panel déclare de son côté.
 */
public enum PanelServerState {
    OFFLINE,
    STARTING,
    RUNNING,
    STOPPING,
    /**
     * Le panel a renvoyé un état que cette version ne connaît pas.
     */
    UNKNOWN
}
