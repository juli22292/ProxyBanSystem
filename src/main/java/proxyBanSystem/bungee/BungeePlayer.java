package proxyBanSystem.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import proxyBanSystem.platform.ProxyPlayer;

import java.util.UUID;

public class BungeePlayer extends BungeeCommandSource implements ProxyPlayer {

    private final ProxiedPlayer player;

    public BungeePlayer(ProxiedPlayer player) {
        super(player);
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getUsername() {
        return player.getName();
    }

    @Override
    public void disconnect(String message) {
        player.disconnect(deserialize(message));
    }
}
