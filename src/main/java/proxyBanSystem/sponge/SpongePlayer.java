package proxyBanSystem.sponge;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import proxyBanSystem.platform.ProxyPlayer;

import java.util.UUID;

public class SpongePlayer implements ProxyPlayer {

    private final ServerPlayer player;

    public SpongePlayer(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(SpongeCommandSource.deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public String getName() {
        return getUsername();
    }

    @Override
    public UUID getUniqueId() {
        return player.uniqueId();
    }

    @Override
    public String getUsername() {
        return player.profile().name().orElse(player.uniqueId().toString());
    }

    @Override
    public void disconnect(String message) {
        player.kick(SpongeCommandSource.deserialize(message));
    }
}
