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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.i18n.Translations;
import fr.farmvivi.panelautostarter.menu.HotbarItem;
import fr.farmvivi.panelautostarter.menu.MenuSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Boussole posée dans la barre d'action rapide du serveur d'attente, qui ouvre
 * le menu des serveurs.
 * <p>
 * <strong>Cet objet n'existe que chez le client.</strong> Le proxy ne possède
 * pas l'inventaire du joueur — c'est le serveur d'arrière-plan qui le tient —
 * et se contente d'affirmer au client qu'une case contient une boussole. La
 * conséquence est nette : le jour où ce serveur touche à l'inventaire, il
 * réaffirme le sien et la boussole disparaît. Sur un serveur d'attente digne
 * de ce nom, qui ne fait rien de l'inventaire, elle tient ; sur un serveur de
 * jeu ordinaire, elle clignoterait. D'où sa limitation au seul serveur
 * d'attente.
 * <p>
 * L'usage se reconnaît sans jamais lire l'inventaire réel : la case tenue est
 * suivie au fil des changements d'objet en main, et un clic droit depuis cette
 * case, sur le serveur d'attente, vaut ouverture. Le paquet est alors retenu —
 * le serveur d'attente n'a pas à recevoir un clic sur un objet qu'il ne connaît
 * pas.
 */
public final class LimboCompass implements HotbarItem {

    /**
     * Décalage de la barre d'action rapide dans la fenêtre d'inventaire du
     * joueur : ses neuf cases y occupent les emplacements 36 à 44.
     */
    private static final int HOTBAR_OFFSET = 36;

    /** Fenêtre de l'inventaire propre au joueur. */
    private static final int PLAYER_INVENTORY_WINDOW = 0;

    private final MenuSettings.CompassSettings settings;
    private final Function<UUID, CommonPlayer> playerResolver;
    private final Consumer<CommonPlayer> onUse;

    /** Case tenue par chaque joueur, pour reconnaître un clic sur la boussole. */
    private final Map<UUID, Integer> heldSlots = new ConcurrentHashMap<>();

    /**
     * Joueurs a qui la boussole a ete posee. Seuls leurs clics sont
     * interceptes : ailleurs, le plugin n'a rien a dire sur un inventaire.
     */
    private final Set<UUID> holders = ConcurrentHashMap.newKeySet();

    /**
     * @param settings       réglages de la boussole
     * @param playerResolver retrouve un joueur depuis son identifiant
     * @param onUse          ce qu'il faut faire quand elle est utilisée
     */
    public LimboCompass(MenuSettings.CompassSettings settings,
                        Function<UUID, CommonPlayer> playerResolver,
                        Consumer<CommonPlayer> onUse) {
        this.settings = settings;
        this.playerResolver = playerResolver;
        this.onUse = onUse;
        PacketEvents.getAPI().getEventManager().registerListener(new UseListener());
    }

    @Override
    public void give(CommonPlayer player) {
        ItemType type = ItemTypes.getByName(settings.item());
        if (type == null) {
            type = ItemTypes.COMPASS;
        }

        ItemStack stack = ItemStack.builder()
                .type(type)
                .amount(1)
                // Sans cela le client applique son propre style aux noms
                // d'objets : italique et violet.
                .component(ComponentTypes.CUSTOM_NAME,
                        Component.text(settings.name(), NamedTextColor.AQUA)
                                .decoration(TextDecoration.ITALIC, false))
                // Traduit ici : l'objet part en paquet, il ne passe pas par
                // CommonPlayer et personne d'autre ne le rendra.
                .component(ComponentTypes.LORE, new ItemLore(List.of(
                        Translations.render(Component.translatable(
                                "panelautostarter.menu.compass.hint", NamedTextColor.GRAY),
                                player.getLocale())
                                .decoration(TextDecoration.ITALIC, false))))
                .build();

        PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlatformHandle(),
                new WrapperPlayServerSetSlot(PLAYER_INVENTORY_WINDOW, 0,
                        HOTBAR_OFFSET + settings.slot(), stack));

        // Selectionner la case, pour que la boussole soit en main a l'arrivee
        // plutot que d'attendre que le joueur la cherche.
        PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlatformHandle(),
                new WrapperPlayServerHeldItemChange(settings.slot()));

        // Le client ne signale pas un changement de case decide par le serveur :
        // sans cette note, on croirait qu'il tient toujours la premiere, et le
        // premier clic droit sur la boussole passerait inapercu.
        heldSlots.put(player.getUniqueId(), settings.slot());
        holders.add(player.getUniqueId());
    }

    /**
     * Repose la boussole et vide le curseur.
     * <p>
     * Le curseur d'abord : c'est la qu'atterrit l'objet que le client
     * croit avoir ramasse.
     */
    private void restore(UUID uuid) {
        CommonPlayer player = playerResolver.apply(uuid);
        if (player == null) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player.getPlatformHandle(),
                new WrapperPlayServerSetSlot(-1, 0, -1, ItemStack.EMPTY));
        give(player);
    }

    /**
     * Oublie ce qu'on savait d'un joueur qui s'en va.
     *
     * @param uuid son identifiant
     */
    public void forget(UUID uuid) {
        heldSlots.remove(uuid);
        holders.remove(uuid);
    }

    private final class UseListener extends PacketListenerAbstract {

        private UseListener() {
            super(PacketListenerPriority.NORMAL);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getUser() == null || event.getUser().getUUID() == null) {
                return;
            }
            UUID uuid = event.getUser().getUUID();

            if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
                heldSlots.put(uuid, new WrapperPlayClientHeldItemChange(event).getSlot());
                return;
            }

            // Un clic dans un inventaire, quel qu'il soit, peut emporter la
            // boussole : le client anticipe le ramassage, et personne ne le
            // dement — le serveur d'attente ignore l'existence de cet objet.
            // On refuse le clic et on la repose.
            if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
                if (holders.contains(uuid)) {
                    event.setCancelled(true);
                    restore(uuid);
                }
                return;
            }

            // Un menu se referme sur l'inventaire du joueur : c'est le moment
            // ou une reaffirmation porte, une fenetre ouverte masquant les
            // cases de l'inventaire propre.
            if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
                if (holders.contains(uuid)) {
                    restore(uuid);
                }
                return;
            }

            boolean used = event.getPacketType() == PacketType.Play.Client.USE_ITEM
                    || event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT;
            if (!used) {
                return;
            }

            // La case tenue par defaut est la premiere : un joueur qui n'a
            // jamais change d'objet en main n'a envoye aucun paquet.
            if (heldSlots.getOrDefault(uuid, 0) != settings.slot()) {
                return;
            }

            CommonPlayer player = playerResolver.apply(uuid);
            if (player == null) {
                return;
            }

            // Le serveur d'attente n'a pas a recevoir un clic sur un objet
            // qu'il ne connait pas.
            event.setCancelled(true);
            onUse.accept(player);
        }
    }
}
