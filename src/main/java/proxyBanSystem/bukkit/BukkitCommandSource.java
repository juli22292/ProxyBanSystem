package proxyBanSystem.bukkit;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import proxyBanSystem.platform.ProxyCommandSource;

public class BukkitCommandSource implements ProxyCommandSource {

    private final CommandSender sender;

    public BukkitCommandSource(CommandSender sender) {
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
        if (sender instanceof Player player) {
            return player.getName();
        }

        return "Console";
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    protected static String deserialize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
