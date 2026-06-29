package proxyBanSystem.platform;

import java.util.UUID;

public interface ProxyPlayer extends ProxyCommandSource {
    UUID getUniqueId();

    String getUsername();

    void disconnect(String message);

    @Override
    default String getName() {
        return getUsername();
    }

    @Override
    default boolean isPlayer() {
        return true;
    }
}
