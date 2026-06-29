package proxyBanSystem.bukkit;

import org.bukkit.entity.Player;
import proxyBanSystem.platform.ProxyPlayer;

import java.util.UUID;

public class BukkitPlayer extends BukkitCommandSource implements ProxyPlayer {

    private final Player player;

    public BukkitPlayer(Player player) {
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
        player.kickPlayer(deserialize(message));
    }
}
