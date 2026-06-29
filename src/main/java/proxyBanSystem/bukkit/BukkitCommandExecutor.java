package proxyBanSystem.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

import java.util.List;

public class BukkitCommandExecutor implements CommandExecutor, TabCompleter {

    private final ProxyCommand command;

    public BukkitCommandExecutor(ProxyCommand command) {
        this.command = command;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        this.command.execute(wrap(sender), args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.suggest(wrap(sender), args);
    }

    private ProxyCommandSource wrap(CommandSender sender) {
        if (sender instanceof Player player) {
            return new BukkitPlayer(player);
        }

        return new BukkitCommandSource(sender);
    }
}
