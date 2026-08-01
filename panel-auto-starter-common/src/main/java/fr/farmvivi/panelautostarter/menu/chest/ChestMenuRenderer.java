package fr.farmvivi.panelautostarter.menu.chest;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.PlayerSkin;
import fr.farmvivi.panelautostarter.menu.Menu;
import fr.farmvivi.panelautostarter.menu.MenuItem;
import fr.farmvivi.panelautostarter.menu.MenuRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Rend un menu dans un coffre, en fabriquant les paquets d'inventaire.
 * <p>
 * <strong>Le proxy ouvre ici une fenêtre que le serveur d'arrière-plan
 * ignore.</strong> C'est toute la difficulté. Le client, lui, la croit réelle :
 * il enverra ses clics au backend, qui n'en sait rien, et surtout il
 * <em>anticipe</em> le résultat de chaque clic sans attendre de réponse. Refuser
 * de transmettre le paquet ne suffit donc pas — l'objet suit quand même le
 * curseur à l'écran. Il faut, en plus, réaffirmer au client ce qu'il aurait dû
 * voir : curseur vide, case inchangée.
 * <p>
 * L'identifiant de fenêtre est volontairement élevé : les serveurs numérotent
 * les leurs à partir de 1, et une collision ferait passer nos clics pour ceux
 * d'un vrai conteneur.
 * <p>
 * Ce rendu est facultatif. Il n'est instancié que si PacketEvents est présent,
 * et se déclare indisponible pour les clients trop anciens — auquel cas le
 * menu s'affiche en chat plutôt que de ne pas s'ouvrir.
 */
public final class ChestMenuRenderer implements MenuRenderer {

    /** Nom sous lequel ce rendu se désigne dans la configuration. */
    public static final String NAME = "chest";

    /**
     * Identifiant des fenêtres ouvertes par le plugin. Hors de portée de celles
     * qu'un serveur attribue, pour qu'aucun clic ne soit confondu.
     */
    private static final int WINDOW_ID = 119;

    private static final int COLUMNS = 9;

    /** Colonnes utiles : la première et la dernière portent la bordure. */
    private static final int CONTENT_COLUMNS = COLUMNS - 2;

    /** Un coffre ne dépasse pas six rangées ; deux servent aux bordures. */
    private static final int MAX_CONTENT_ROWS = 4;

    /**
     * Le format d'objet moderne repose sur les composants de données, qu'un
     * client antérieur ne sait pas lire.
     */
    private static final ClientVersion MINIMUM_VERSION = ClientVersion.V_1_20_5;

    private final LoggerProxy logger;
    private final Function<UUID, CommonPlayer> playerResolver;
    private final BiConsumer<CommonPlayer, String> commandRunner;

    /** Fenêtres actuellement ouvertes, pour interpréter et corriger les clics. */
    private final Map<UUID, OpenView> openViews = new ConcurrentHashMap<>();

    /**
     * Ce qu'une fenêtre ouverte contient, tel que le client devrait le voir.
     *
     * @param actions   action de chaque case, null pour les cases inertes
     * @param contents  objet de chaque case, pour rétablir ce qu'un clic a déplacé
     * @param closeSlot case du bouton de fermeture
     */
    private record OpenView(List<MenuItem> actions, List<ItemStack> contents, int closeSlot) {
    }

    /**
     * @param logger         pour signaler ce qui échoue silencieusement
     * @param playerResolver retrouve un joueur depuis son identifiant
     * @param commandRunner  exécute une commande au nom d'un joueur
     */
    public ChestMenuRenderer(LoggerProxy logger, Function<UUID, CommonPlayer> playerResolver,
                             BiConsumer<CommonPlayer, String> commandRunner) {
        this.logger = logger;
        this.playerResolver = playerResolver;
        this.commandRunner = commandRunner;
        PacketEvents.getAPI().getEventManager().registerListener(new ClickListener());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailable(CommonPlayer viewer) {
        try {
            ClientVersion version = PacketEvents.getAPI().getPlayerManager()
                    .getUser(viewer.getPlatformHandle()).getClientVersion();
            return version != null && version.isNewerThanOrEquals(MINIMUM_VERSION);
        } catch (Throwable ex) {
            // Un joueur que PacketEvents ne connait pas encore, ou une
            // bibliotheque en desaccord avec la version du jeu : le menu doit
            // s'ouvrir quand meme, en chat.
            return false;
        }
    }

    // ===================== Mise en page =====================

    @Override
    public void open(CommonPlayer viewer, Menu menu) {
        List<MenuItem> items = menu.items();
        int contentRows = Math.max(1, Math.min(MAX_CONTENT_ROWS,
                (items.size() + CONTENT_COLUMNS - 1) / CONTENT_COLUMNS));
        int rows = contentRows + 2;
        int size = rows * COLUMNS;

        List<ItemStack> contents = new ArrayList<>(size);
        List<MenuItem> actions = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            contents.add(border());
            actions.add(null);
        }

        placeContent(items, contents, actions, contentRows);

        int closeSlot = size - COLUMNS + COLUMNS / 2;
        contents.set(closeSlot, closeButton());

        Object platform = viewer.getPlatformHandle();
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(platform,
                    new WrapperPlayServerOpenWindow(WINDOW_ID, rows - 1, menu.title()));
            PacketEvents.getAPI().getPlayerManager().sendPacket(platform,
                    new WrapperPlayServerWindowItems(WINDOW_ID, 0, contents, ItemStack.EMPTY));
            openViews.put(viewer.getUniqueId(), new OpenView(actions, contents, closeSlot));
        } catch (RuntimeException ex) {
            // Ne pas laisser le joueur devant rien : on rend la main a
            // l'appelant, qui retombera sur le rendu en chat.
            openViews.remove(viewer.getUniqueId());
            logger.warning("Ouverture du menu en coffre impossible pour "
                    + viewer.getUsername() + " : " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * Dispose les entrées dans la zone intérieure, rangée par rangée.
     * <p>
     * Une rangée incomplète est centrée : un seul serveur posé dans le coin
     * gauche d'un coffre vide se lit comme un oubli, au milieu comme un choix.
     */
    private void placeContent(List<MenuItem> items, List<ItemStack> contents,
                              List<MenuItem> actions, int contentRows) {
        int index = 0;
        for (int row = 0; row < contentRows && index < items.size(); row++) {
            int remaining = items.size() - index;
            int onThisRow = Math.min(CONTENT_COLUMNS, remaining);
            int leftPadding = (CONTENT_COLUMNS - onThisRow) / 2;

            for (int column = 0; column < onThisRow; column++) {
                MenuItem item = items.get(index++);
                int slot = (row + 1) * COLUMNS + 1 + leftPadding + column;
                contents.set(slot, toItemStack(item));
                actions.set(slot, item);
            }
        }
    }

    /**
     * Vitre de bordure : un nom vide plutôt qu'absent, sans quoi le client
     * affiche « Vitre teintée grise » sous le curseur.
     */
    private ItemStack border() {
        return ItemStack.builder()
                .type(ItemTypes.GRAY_STAINED_GLASS_PANE)
                .amount(1)
                .component(ComponentTypes.CUSTOM_NAME, Component.empty())
                .build();
    }

    private ItemStack closeButton() {
        return ItemStack.builder()
                .type(ItemTypes.BARRIER)
                .amount(1)
                .component(ComponentTypes.CUSTOM_NAME,
                        Component.text("Fermer", NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false))
                .build();
    }

    private ItemStack toItemStack(MenuItem item) {
        ItemType type = ItemTypes.getByName(item.icon());
        if (type == null) {
            // Un identifiant d'objet inconnu vient de la configuration ou d'une
            // version de jeu differente : un objet neutre vaut mieux qu'un trou.
            type = ItemTypes.PAPER;
        }

        ItemStack.Builder builder = ItemStack.builder().type(type).amount(1);

        // L'effigie du joueur, quand le proxy la connait. La signature
        // accompagne la valeur : sans elle, un client en ligne refuse la
        // texture et retombe sur une tete generique.
        PlayerSkin skin = item.skin();
        if (skin != null && skin.value() != null) {
            builder.component(ComponentTypes.PROFILE, new ItemProfile(
                    skin.username(), skin.uuid(),
                    List.of(new ItemProfile.Property("textures", skin.value(),
                            skin.signature()))));
        }

        // Sans cela le client applique son propre style aux noms d'objets :
        // italique et violet, ce qui n'est pas ce qu'on a ecrit.
        builder.component(ComponentTypes.CUSTOM_NAME, item.label()
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        List<Component> lore = new ArrayList<>(item.lore());
        if (item.isActionable()) {
            lore.add(Component.text("Cliquez pour choisir", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) {
            List<Component> styled = new ArrayList<>(lore.size());
            for (Component line : lore) {
                styled.add(line.decorationIfAbsent(TextDecoration.ITALIC,
                        TextDecoration.State.FALSE));
            }
            builder.component(ComponentTypes.LORE, new ItemLore(styled));
        }

        return builder.build();
    }

    // ===================== Clics =====================

    private void close(CommonPlayer viewer) {
        openViews.remove(viewer.getUniqueId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer.getPlatformHandle(),
                new WrapperPlayServerCloseWindow(WINDOW_ID));
    }

    /**
     * Rétablit chez le client l'état qu'il aurait dû garder.
     * <p>
     * Indispensable : le client anticipe le résultat d'un clic sans attendre de
     * réponse. Sans ce rappel, refuser le clic le laisse persuadé d'avoir pris
     * l'objet — il le voit accroché à son curseur, et le glisse dans son
     * inventaire.
     */
    private void resync(Object platform, OpenView view, int slot) {
        // Curseur d'abord : c'est la qu'atterrit l'objet qu'on refuse.
        PacketEvents.getAPI().getPlayerManager().sendPacket(platform,
                new WrapperPlayServerSetSlot(-1, 0, -1, ItemStack.EMPTY));

        if (slot >= 0 && slot < view.contents().size()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(platform,
                    new WrapperPlayServerSetSlot(WINDOW_ID, 0, slot, view.contents().get(slot)));
        }
    }

    /**
     * Intercepte les paquets portant notre identifiant de fenêtre.
     * <p>
     * Les laisser passer serait le vrai danger : le serveur d'arrière-plan
     * recevrait un clic dans un conteneur qu'il n'a jamais ouvert.
     */
    private final class ClickListener extends PacketListenerAbstract {

        private ClickListener() {
            super(PacketListenerPriority.NORMAL);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                handleClick(event);
            } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                handleClose(event);
            }
        }

        private void handleClick(PacketReceiveEvent event) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            if (packet.getWindowId() != WINDOW_ID) {
                return;
            }
            event.setCancelled(true);

            UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
            OpenView view = uuid == null ? null : openViews.get(uuid);
            if (view == null) {
                return;
            }

            CommonPlayer player = playerResolver.apply(uuid);
            if (player == null) {
                return;
            }

            int slot = packet.getSlot();

            // Toujours corriger, y compris pour un clic dans la partie
            // inventaire de la fenetre : le client anticipe la aussi, et
            // deplacerait ses propres objets dans une fenetre imaginaire.
            resync(player.getPlatformHandle(), view, slot);

            if (slot == view.closeSlot()) {
                close(player);
                return;
            }

            MenuItem item = slot >= 0 && slot < view.actions().size()
                    ? view.actions().get(slot) : null;
            if (item == null || !item.isActionable()) {
                return;
            }

            close(player);
            commandRunner.accept(player, item.command());
        }

        private void handleClose(PacketReceiveEvent event) {
            WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);
            if (packet.getWindowId() != WINDOW_ID) {
                return;
            }
            event.setCancelled(true);
            UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
            if (uuid != null) {
                openViews.remove(uuid);
            }
        }
    }
}
