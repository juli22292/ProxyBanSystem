package proxyBanSystem.velocity;

import com.velocitypowered.api.proxy.Player;
import proxyBanSystem.platform.ProxyPlayer;

import java.util.UUID;

public class VelocityPlayer extends VelocityCommandSource implements ProxyPlayer {

    private final Player player;

    public VelocityPlayer(Player player) {
        super(player);
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public String getUsername() {
        return player.getUsername();
    }

    @Override
    public void disconnect(String message) {
        player.disconnect(deserialize(message));
    }
}
