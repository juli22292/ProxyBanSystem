package proxyBanSystem.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyPlayer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

public class BukkitAdapter implements ProxyAdapter {

    private final JavaPlugin plugin;
    private final CommandMap commandMap;

    public BukkitAdapter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap();
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
        if (commandMap != null) {
            commandMap.register("proxybansystem", new BukkitCommandWrapper(name, command));
            return;
        }

        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand == null) {
            error("Command /" + name + " konnte nicht registriert werden");
            return;
        }

        BukkitCommandExecutor executor = new BukkitCommandExecutor(command);
        pluginCommand.setExecutor(executor);
        pluginCommand.setTabCompleter(executor);
    }

    @Override
    public ProxyPlayer getPlayer(String name) {
        Player player = Bukkit.getPlayerExact(name);
        return player == null ? null : new BukkitPlayer(player);
    }

    @Override
    public Collection<? extends ProxyPlayer> getAllPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(BukkitPlayer::new)
                .collect(Collectors.toList());
    }

    private CommandMap resolveCommandMap() {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
            Object value = method.invoke(Bukkit.getServer());
            if (value instanceof CommandMap map) {
                return map;
            }
        } catch (Exception e) {
            error("Bukkit CommandMap konnte nicht aufgeloest werden", e);
        }

        return null;
    }
}
