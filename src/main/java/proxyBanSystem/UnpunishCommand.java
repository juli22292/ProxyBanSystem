package proxyBanSystem;

import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

public class UnpunishCommand implements ProxyCommand {

    private final ProxyAdapter adapter;

    public UnpunishCommand(ProxyAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void execute(ProxyCommandSource source, String[] args) {
        String executorName = null;

        if (source.isPlayer()) {
            executorName = source.getName();

            if (!source.hasPermission("punishsystem.admin.use")) {
                source.sendMessage(ProxyBanSystem.message("no-permission"));
                return;
            }
        }

        if (args.length < 1) {
            source.sendMessage(ProxyBanSystem.message("unpunish-usage"));
            return;
        }

        if (ProxyBanSystem.mysql == null || !ProxyBanSystem.mysql.isConnected()) {
            source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
            return;
        }

        String name = args[0];

        boolean found = false;
        UUID targetUUID = null;
        String resolvedName = null;

        try {
            targetUUID = UUID.fromString(name);
            resolvedName = ProxyBanSystem.mysql.getNameByUUID(targetUUID.toString());

            if (ProxyBanSystem.mysql.isBanned(targetUUID.toString())) {
                found = true;
            }
        } catch (Exception ignored) {
            String uuidString = ProxyBanSystem.mysql.getUUIDByName(name);

            if (uuidString != null) {
                targetUUID = UUID.fromString(uuidString);
                resolvedName = ProxyBanSystem.mysql.getNameByUUID(uuidString);

                if (ProxyBanSystem.mysql.isBanned(uuidString)) {
                    found = true;
                }
            }

            if (targetUUID == null) {
                for (UUID id : ProxyBanSystem.players.keySet()) {
                    if (ProxyBanSystem.players.get(id).equalsIgnoreCase(name)) {
                        targetUUID = id;
                        resolvedName = ProxyBanSystem.players.get(id);

                        if (ProxyBanSystem.mysql.isBanned(id.toString())) {
                            found = true;
                        }

                        break;
                    }
                }
            }
        }

        if (!found) {
            source.sendMessage(ProxyBanSystem.message("player-not-banned"));
            return;
        }

        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = name;
        }

        try {
            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
                return;
            }

            if (targetUUID != null) {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM bans WHERE uuid=?")) {
                    ps.setString(1, targetUUID.toString());
                    ps.executeUpdate();
                }

                try (PreparedStatement history = connection.prepareStatement(
                        "INSERT INTO punishment_history (uuid, name, type, reason, executor, date) VALUES (?, ?, ?, ?, ?, ?)"
                )) {
                    history.setString(1, targetUUID.toString());
                    history.setString(2, resolvedName);
                    history.setString(3, "UNBAN");
                    history.setString(4, ProxyBanSystem.message("history-unban-reason"));
                    history.setString(5, executorName != null ? executorName : ProxyBanSystem.configuredConsoleName());
                    history.setString(6, ProxyBanSystem.currentHistoryDate());
                    history.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM bans WHERE name=?")) {
                    ps.setString(1, name);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(ProxyBanSystem.message("unban-save-failed"));
            return;
        }

        String executor = executorName != null ? executorName : ProxyBanSystem.configuredConsoleName();
        ProxyBanSystem.broadcastUnban(resolvedName, executor);
        source.sendMessage(ProxyBanSystem.message("unban-success"));
    }

    @Override
    public List<String> suggest(ProxyCommandSource source, String[] args) {
        if (args.length == 1 && ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
            return ProxyBanSystem.mysql.getRegisteredPlayers().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
