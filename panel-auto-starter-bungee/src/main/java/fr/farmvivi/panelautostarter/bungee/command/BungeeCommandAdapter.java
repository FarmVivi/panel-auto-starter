package fr.farmvivi.panelautostarter.bungee.command;

import fr.farmvivi.panelautostarter.bungee.BungeePlayer;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.command.CommonCommand;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

/**
 * Traduit une {@link CommonCommand} vers l'API de commandes de BungeeCord.
 * <p>
 * La permission est laissée à null au niveau de BungeeCord : le détail des
 * droits se joue serveur par serveur à l'exécution, et une permission déclarée
 * ici bloquerait l'accès avant que la commande n'ait pu en juger.
 */
public final class BungeeCommandAdapter extends Command implements TabExecutor {

    private final CommonCommand command;
    private final CommonProxy proxy;

    public BungeeCommandAdapter(CommonCommand command, CommonProxy proxy) {
        super(command.getName(), null, command.getAliases().toArray(new String[0]));
        this.command = command;
        this.proxy = proxy;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        command.execute(wrap(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return command.suggest(wrap(sender), args);
    }

    private CommonCommandSource wrap(CommandSender sender) {
        return new CommonCommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return sender.hasPermission(permission);
            }

            @Override
            public void sendMessage(Component message) {
                // BungeeCord ne connait pas Adventure : on retombe sur les codes
                // de couleur historiques, comme partout ailleurs dans ce module.
                sender.sendMessage(new TextComponent(
                        LegacyComponentSerializer.legacySection().serialize(message)));
            }

            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public CommonPlayer asPlayer() {
                return sender instanceof ProxiedPlayer player
                        ? new BungeePlayer(proxy, player) : null;
            }
        };
    }
}
