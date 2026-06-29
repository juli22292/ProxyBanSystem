package proxyBanSystem.sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.plugin.PluginContainer;
import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyPlayer;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SpongeAdapter implements ProxyAdapter {

    private final PluginContainer plugin;
    private final RegisterCommandEvent<Command.Raw> commandEvent;
    private final File dataDirectory;

    public SpongeAdapter(PluginContainer plugin, RegisterCommandEvent<Command.Raw> commandEvent, File dataDirectory) {
        this.plugin = plugin;
        this.commandEvent = commandEvent;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public File getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public void info(String message) {
        plugin.logger().info(message);
    }

    @Override
    public void error(String message) {
        plugin.logger().error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.logger().error(message, throwable);
    }

    @Override
    public void registerCommand(String name, ProxyCommand command) {
        commandEvent.register(plugin, new SpongeCommandWrapper(command), name);
    }

    @Override
    public ProxyPlayer getPlayer(String name) {
        if (!Sponge.isServerAvailable()) {
            return null;
        }

        ServerPlayer player = Sponge.server().player(name).orElse(null);
        return player == null ? null : new SpongePlayer(player);
    }

    @Override
    public Collection<? extends ProxyPlayer> getAllPlayers() {
        if (!Sponge.isServerAvailable()) {
            return List.of();
        }

        return Sponge.server().onlinePlayers().stream()
                .map(SpongePlayer::new)
                .collect(Collectors.toList());
    }
}
