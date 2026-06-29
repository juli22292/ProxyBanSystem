package proxyBanSystem.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

public class BungeeCommandWrapper extends Command implements TabExecutor {

    private final ProxyCommand command;

    public BungeeCommandWrapper(String name, ProxyCommand command) {
        super(name);
        this.command = command;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        command.execute(wrap(sender), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return command.suggest(wrap(sender), args);
    }

    private ProxyCommandSource wrap(CommandSender sender) {
        if (sender instanceof net.md_5.bungee.api.connection.ProxiedPlayer player) {
            return new BungeePlayer(player);
        }

        return new BungeeCommandSource(sender);
    }
}
