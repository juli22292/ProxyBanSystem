package proxyBanSystem.sponge;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import proxyBanSystem.platform.ProxyCommandSource;

public class SpongeCommandSource implements ProxyCommandSource {

    protected static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final CommandCause cause;

    public SpongeCommandSource(CommandCause cause) {
        this.cause = cause;
    }

    @Override
    public void sendMessage(String message) {
        cause.sendMessage(deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return cause.subject().hasPermission(permission);
    }

    @Override
    public String getName() {
        return cause.first(ServerPlayer.class)
                .map(SpongeCommandSource::nameOf)
                .orElse("Console");
    }

    @Override
    public boolean isPlayer() {
        return cause.first(ServerPlayer.class).isPresent();
    }

    protected static Component deserialize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.text("");
        }

        return SERIALIZER.deserialize(message);
    }

    private static String nameOf(ServerPlayer player) {
        return player.profile().name().orElse(player.uniqueId().toString());
    }
}
