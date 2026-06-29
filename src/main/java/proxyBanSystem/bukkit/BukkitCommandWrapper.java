package proxyBanSystem.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

import java.util.List;

public class BukkitCommandWrapper extends Command {

    private final ProxyCommand command;

    public BukkitCommandWrapper(String name, ProxyCommand command) {
        super(name);
        this.command = command;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        command.execute(wrap(sender), args);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return command.suggest(wrap(sender), args);
    }

    private ProxyCommandSource wrap(CommandSender sender) {
        if (sender instanceof Player player) {
            return new BukkitPlayer(player);
        }

        return new BukkitCommandSource(sender);
    }
}
