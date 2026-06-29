package proxyBanSystem.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

import java.util.List;

public class VelocityCommandWrapper implements SimpleCommand {

    private final ProxyCommand command;

    public VelocityCommandWrapper(ProxyCommand command) {
        this.command = command;
    }

    @Override
    public void execute(Invocation invocation) {
        command.execute(wrap(invocation.source()), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return command.suggest(wrap(invocation.source()), invocation.arguments());
    }

    private ProxyCommandSource wrap(CommandSource source) {
        if (source instanceof com.velocitypowered.api.proxy.Player player) {
            return new VelocityPlayer(player);
        }

        return new VelocityCommandSource(source);
    }
}
