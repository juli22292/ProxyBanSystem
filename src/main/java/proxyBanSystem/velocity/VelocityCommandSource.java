package proxyBanSystem.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import proxyBanSystem.platform.ProxyCommandSource;

public class VelocityCommandSource implements ProxyCommandSource {

    protected static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final CommandSource source;

    public VelocityCommandSource(CommandSource source) {
        this.source = source;
    }

    @Override
    public void sendMessage(String message) {
        source.sendMessage(deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public String getName() {
        if (source instanceof Player player) {
            return player.getUsername();
        }

        return "Console";
    }

    protected static Component deserialize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.text("");
        }

        return SERIALIZER.deserialize(message);
    }
}
