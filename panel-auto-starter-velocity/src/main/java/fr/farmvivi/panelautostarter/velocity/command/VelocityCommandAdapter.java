package fr.farmvivi.panelautostarter.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.farmvivi.panelautostarter.common.CommonPlayer;
import fr.farmvivi.panelautostarter.common.CommonProxy;
import fr.farmvivi.panelautostarter.common.command.CommonCommand;
import fr.farmvivi.panelautostarter.common.command.CommonCommandSource;
import fr.farmvivi.panelautostarter.velocity.VelocityPlayer;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Traduit une {@link CommonCommand} vers l'API de commandes de Velocity.
 */
public final class VelocityCommandAdapter implements SimpleCommand {

    private final CommonCommand command;
    private final CommonProxy proxy;

    public VelocityCommandAdapter(CommonCommand command, CommonProxy proxy) {
        this.command = command;
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        command.execute(wrap(invocation.source()), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return command.suggest(wrap(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        String permission = command.getPermission();
        return permission == null || invocation.source().hasPermission(permission);
    }

    private CommonCommandSource wrap(CommandSource source) {
        return new CommonCommandSource() {
            @Override
            public boolean hasPermission(String permission) {
                return source.hasPermission(permission);
            }

            @Override
            public void sendMessage(Component message) {
                source.sendMessage(message);
            }

            @Override
            public String getName() {
                return source instanceof Player player ? player.getUsername() : "Console";
            }

            @Override
            public CommonPlayer asPlayer() {
                return source instanceof Player player ? new VelocityPlayer(proxy, player) : null;
            }
        };
    }
}
