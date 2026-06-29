package proxyBanSystem.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyPlayer;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

public class BungeeAdapter implements ProxyAdapter {

    private final Plugin plugin;

    public BungeeAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public File getDataDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void error(String message) {
        plugin.getLogger().severe(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.getLogger().severe(message + " - " + throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void registerCommand(String name, ProxyCommand command) {
        ProxyServer.getInstance().getPluginManager().registerCommand(plugin, new BungeeCommandWrapper(name, command));
    }

    @Override
    public ProxyPlayer getPlayer(String name) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        return player == null ? null : new BungeePlayer(player);
    }

    @Override
    public Collection<? extends ProxyPlayer> getAllPlayers() {
        return ProxyServer.getInstance().getPlayers().stream()
                .map(BungeePlayer::new)
                .collect(Collectors.toList());
    }
}
