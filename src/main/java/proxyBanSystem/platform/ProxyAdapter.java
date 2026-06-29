package proxyBanSystem.platform;

import java.io.File;
import java.util.Collection;

public interface ProxyAdapter {
    File getDataDirectory();

    void info(String message);

    void error(String message);

    void error(String message, Throwable throwable);

    void registerCommand(String name, ProxyCommand command);

    ProxyPlayer getPlayer(String name);

    Collection<? extends ProxyPlayer> getAllPlayers();
}
