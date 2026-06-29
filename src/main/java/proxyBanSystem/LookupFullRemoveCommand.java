package proxyBanSystem;

import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;
import proxyBanSystem.platform.ProxyPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class LookupFullRemoveCommand implements ProxyCommand {

    private final ProxyAdapter adapter;

    public LookupFullRemoveCommand(ProxyAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void execute(ProxyCommandSource source, String[] args) {
        if (source.isPlayer() && !source.hasPermission("punishsystem.admin.use")) {
            source.sendMessage(ProxyBanSystem.message("no-permission"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(ProxyBanSystem.message("lookupfullremove-usage"));
            return;
        }

        if (ProxyBanSystem.mysql == null || !ProxyBanSystem.mysql.isConnected()) {
            source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
            return;
        }

        Target target = resolveTarget(args[0]);
        if (target == null) {
            source.sendMessage(ProxyBanSystem.message("player-not-found"));
            return;
        }

        try {
            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
                return;
            }

            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM punishment_history WHERE uuid=?")) {
                ps.setString(1, target.uuid().toString());
                ps.executeUpdate();
            }

            ProxyBanSystem.bannedPlayers.remove(target.uuid().toString());
            ProxyBanSystem.saveBannedPlayers();

            source.sendMessage(ProxyBanSystem.message(
                    "lookupfullremove-success",
                    ProxyBanSystem.placeholders("player", target.name())
            ));
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(ProxyBanSystem.message("lookupfullremove-failed"));
        }
    }

    private Target resolveTarget(String input) {
        ProxyPlayer onlinePlayer = adapter.getPlayer(input);
        if (onlinePlayer != null) {
            return new Target(onlinePlayer.getUniqueId(), onlinePlayer.getUsername());
        }

        try {
            UUID uuid = UUID.fromString(input);
            String name = ProxyBanSystem.mysql.getNameByUUID(uuid.toString());
            return new Target(uuid, name == null || name.isBlank() ? input : name);
        } catch (Exception ignored) {
        }

        String uuidString = ProxyBanSystem.mysql.getUUIDByName(input);
        if (uuidString != null) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String name = ProxyBanSystem.mysql.getNameByUUID(uuidString);
                return new Target(uuid, name == null || name.isBlank() ? input : name);
            } catch (Exception ignored) {
            }
        }

        for (UUID uuid : ProxyBanSystem.players.keySet()) {
            String name = ProxyBanSystem.players.get(uuid);
            if (name != null && name.equalsIgnoreCase(input)) {
                return new Target(uuid, name);
            }
        }

        return null;
    }

    @Override
    public List<String> suggest(ProxyCommandSource source, String[] args) {
        if (args.length == 1) {
            Set<String> suggestions = new HashSet<>();
            suggestions.addAll(ProxyBanSystem.players.values());

            if (ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
                suggestions.addAll(ProxyBanSystem.mysql.getRegisteredPlayers());
            }

            return suggestions.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return List.of();
    }

    private record Target(UUID uuid, String name) {
    }
}
