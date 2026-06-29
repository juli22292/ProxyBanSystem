package proxyBanSystem.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyPlayer;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

public class VelocityAdapter implements ProxyAdapter {

    private final ProxyServer server;
    private final Logger logger;
    private final File dataDirectory;

    public VelocityAdapter(ProxyServer server, Logger logger, Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory.toFile();
    }

    @Override
    public File getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public void registerCommand(String name, ProxyCommand command) {
        CommandManager commandManager = server.getCommandManager();
        commandManager.register(
                commandManager.metaBuilder(name).build(),
                new VelocityCommandWrapper(command)
        );
    }

    @Override
    public ProxyPlayer getPlayer(String name) {
        Player player = server.getPlayer(name).orElse(null);
        return player == null ? null : new VelocityPlayer(player);
    }

    @Override
    public Collection<? extends ProxyPlayer> getAllPlayers() {
        return server.getAllPlayers().stream()
                .map(VelocityPlayer::new)
                .collect(Collectors.toList());
    }
}
