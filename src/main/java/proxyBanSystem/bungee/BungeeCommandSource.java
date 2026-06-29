package proxyBanSystem.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import proxyBanSystem.platform.ProxyCommandSource;

public class BungeeCommandSource implements ProxyCommandSource {

    private final CommandSender sender;

    public BungeeCommandSource(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public String getName() {
        if (sender instanceof ProxiedPlayer player) {
            return player.getName();
        }

        return "Console";
    }

    protected static BaseComponent[] deserialize(String message) {
        String text = message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
        return TextComponent.fromLegacyText(text);
    }
}
