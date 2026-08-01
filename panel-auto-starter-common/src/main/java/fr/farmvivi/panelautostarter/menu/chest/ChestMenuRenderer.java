package fr.farmvivi.panelautostarter.menu.chest;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemLore;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import fr.farmvivi.panelautostarter.LoggerProxy;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
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
 * ignore.</strong> C'est toute la difficulté : le client, lui, la croit réelle
 * et enverra ses clics et sa fermeture au backend, qui n'en sait rien et
 * répondrait par une resynchronisation — l'inventaire du joueur se mettrait à
 * clignoter, voire à perdre des objets. Les paquets portant notre identifiant
 * de fenêtre sont donc interceptés et <em>ne sont jamais transmis</em>.
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

    /** Type de conteneur « coffre » ; sa taille est donnée par le nombre de rangées. */
    private static final int CHEST_TYPE_OFFSET = 0;

    private static final int SLOTS_PER_ROW = 9;
    private static final int MAX_ROWS = 6;

    /**
     * Le format d'objet moderne repose sur les composants de données, qu'un
     * client antérieur ne sait pas lire.
     */
    private static final ClientVersion MINIMUM_VERSION = ClientVersion.V_1_20_5;

    private final LoggerProxy logger;
    private final Function<UUID, CommonPlayer> playerResolver;
    private final BiConsumer<CommonPlayer, String> commandRunner;

    /** Menus actuellement ouverts, pour retrouver l'action d'un clic. */
    private final Map<UUID, List<MenuItem>> openMenus = new ConcurrentHashMap<>();

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
                    .getUser(platformPlayer(viewer)).getClientVersion();
            return version != null && version.isNewerThanOrEquals(MINIMUM_VERSION);
        } catch (Throwable ex) {
            // Un joueur que PacketEvents ne connait pas encore, ou une
            // bibliotheque en desaccord avec la version du jeu : le menu doit
            // s'ouvrir quand meme, en chat.
            return false;
        }
    }

    @Override
    public void open(CommonPlayer viewer, Menu menu) {
        List<MenuItem> items = menu.items();
        int rows = Math.min(MAX_ROWS, Math.max(1,
                (items.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW));
        int size = rows * SLOTS_PER_ROW;

        List<ItemStack> contents = new ArrayList<>(size);
        List<MenuItem> placed = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            MenuItem item = slot < items.size() ? items.get(slot) : null;
            contents.add(item == null ? ItemStack.EMPTY : toItemStack(item));
            placed.add(item);
        }

        Object platform = platformPlayer(viewer);
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(platform,
                    new WrapperPlayServerOpenWindow(WINDOW_ID, CHEST_TYPE_OFFSET + rows - 1,
                            menu.title()));
            PacketEvents.getAPI().getPlayerManager().sendPacket(platform,
                    new WrapperPlayServerWindowItems(WINDOW_ID, 0, contents, ItemStack.EMPTY));
            openMenus.put(viewer.getUniqueId(), placed);
        } catch (RuntimeException ex) {
            // Ne pas laisser le joueur devant rien : on rend la main a
            // l'appelant, qui retombera sur le rendu en chat.
            openMenus.remove(viewer.getUniqueId());
            logger.warning("Ouverture du menu en coffre impossible pour "
                    + viewer.getUsername() + " : " + ex.getMessage());
            throw ex;
        }
    }

    private ItemStack toItemStack(MenuItem item) {
        ItemType type = ItemTypes.getByName(item.icon());
        if (type == null) {
            // Un identifiant d'objet inconnu vient de la configuration ou d'une
            // version de jeu differente : un objet neutre vaut mieux qu'un trou.
            type = ItemTypes.PAPER;
        }

        ItemStack.Builder builder = ItemStack.builder().type(type).amount(1);

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

    private static Object platformPlayer(CommonPlayer viewer) {
        return viewer.getPlatformHandle();
    }

    private void close(CommonPlayer viewer) {
        openMenus.remove(viewer.getUniqueId());
        PacketEvents.getAPI().getPlayerManager().sendPacket(platformPlayer(viewer),
                new WrapperPlayServerCloseWindow(WINDOW_ID));
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

            UUID uuid = event.getUser().getUUID();
            List<MenuItem> items = uuid == null ? null : openMenus.get(uuid);
            if (items == null) {
                return;
            }

            int slot = packet.getSlot();
            if (slot < 0 || slot >= items.size()) {
                // Un clic dans l'inventaire du joueur, ou hors de la fenetre :
                // rien a faire, mais surtout rien a transmettre.
                return;
            }

            MenuItem item = items.get(slot);
            if (item == null || !item.isActionable()) {
                return;
            }

            openMenus.remove(uuid);
            runChoice(uuid, item.command());
        }

        private void handleClose(PacketReceiveEvent event) {
            WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);
            if (packet.getWindowId() != WINDOW_ID) {
                return;
            }
            event.setCancelled(true);
            UUID uuid = event.getUser().getUUID();
            if (uuid != null) {
                openMenus.remove(uuid);
            }
        }
    }

    /**
     * Exécute la commande choisie.
     * <p>
     * Le joueur est retrouvé par son identifiant plutôt que porté depuis le
     * paquet : le clic arrive sur le fil réseau, et l'objet qui représentait le
     * joueur au moment de l'ouverture peut n'être plus valable.
     */
    private void runChoice(UUID uuid, String command) {
        CommonPlayer player = playerResolver.apply(uuid);
        if (player == null) {
            return;
        }
        close(player);
        commandRunner.accept(player, command);
    }
}
