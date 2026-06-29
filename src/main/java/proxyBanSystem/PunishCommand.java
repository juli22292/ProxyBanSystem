package proxyBanSystem;

import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;
import proxyBanSystem.platform.ProxyPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PunishCommand implements ProxyCommand {

    private final ProxyAdapter adapter;

    public PunishCommand(ProxyAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void execute(ProxyCommandSource source, String[] args) {
        if (source.isPlayer() && !source.hasPermission("punishsystem.admin.use")) {
            source.sendMessage(ProxyBanSystem.message("no-permission"));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(ProxyBanSystem.message("punish-usage"));
            return;
        }

        String inputNameOrUuid = args[0];
        String rawReason = args[1];

        ProxyBanSystem.BanReasonConfig reasonConfig = ProxyBanSystem.resolveBanReason(rawReason);
        String reason = reasonConfig.display();

        ProxyPlayer target = adapter.getPlayer(inputNameOrUuid);

        UUID uuid = null;
        String resolvedName = null;

        try {
            uuid = UUID.fromString(inputNameOrUuid);
            if (ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
                resolvedName = ProxyBanSystem.mysql.getNameByUUID(uuid.toString());
            }
        } catch (Exception ignored) {
        }

        if (uuid == null) {
            if (target != null) {
                uuid = target.getUniqueId();
                resolvedName = target.getUsername();
            } else {
                String uuidString = null;
                if (ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
                    uuidString = ProxyBanSystem.mysql.getUUIDByName(inputNameOrUuid);
                }

                if (uuidString != null) {
                    uuid = UUID.fromString(uuidString);
                    resolvedName = ProxyBanSystem.mysql.getNameByUUID(uuidString);
                }

                if (uuid == null) {
                    for (UUID id : ProxyBanSystem.players.keySet()) {
                        if (ProxyBanSystem.players.get(id).equalsIgnoreCase(inputNameOrUuid)) {
                            uuid = id;
                            resolvedName = ProxyBanSystem.players.get(id);
                            break;
                        }
                    }
                }
            }
        }

        if (uuid == null) {
            source.sendMessage(ProxyBanSystem.message("player-not-found"));
            return;
        }

        if (resolvedName == null || resolvedName.isBlank()) {
            resolvedName = target != null ? target.getUsername() : inputNameOrUuid;
        }

        if (ProxyBanSystem.isWhitelisted(uuid, resolvedName)) {
            source.sendMessage(ProxyBanSystem.message("player-whitelisted"));
            return;
        }

        if (ProxyBanSystem.mysql == null || !ProxyBanSystem.mysql.isConnected()) {
            source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
            return;
        }

        if (ProxyBanSystem.mysql.isBanned(uuid.toString())) {
            source.sendMessage(ProxyBanSystem.message("player-already-banned"));
            return;
        }

        long duration = reasonConfig.durationMillis();
        long banUntil = duration == -1 ? -1 : System.currentTimeMillis() + duration;

        String expireText;
        String remainingText;
        String durationText;

        if (banUntil == -1) {
            expireText = ProxyBanSystem.configuredPermanent();
            remainingText = ProxyBanSystem.configuredPermanent();
            durationText = ProxyBanSystem.configuredPermanent();
        } else {
            expireText = ProxyBanSystem.formatDate(banUntil);
            remainingText = ProxyBanSystem.formatDuration(duration);
            durationText = remainingText;
        }

        String executor = source.isPlayer() ? source.getName() : ProxyBanSystem.configuredConsoleName();

        String message = ProxyBanSystem.buildBanMessage(
                executor,
                reason,
                durationText,
                remainingText,
                expireText
        );

        try {
            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
                return;
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "REPLACE INTO bans (uuid, name, reason, message, ban_until) VALUES (?, ?, ?, ?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, resolvedName);
                ps.setString(3, reason);
                ps.setString(4, message);
                ps.setLong(5, banUntil);
                ps.executeUpdate();
            }

            try (PreparedStatement history = connection.prepareStatement(
                    "INSERT INTO punishment_history (uuid, name, type, reason, executor, date) VALUES (?, ?, ?, ?, ?, ?)"
            )) {
                history.setString(1, uuid.toString());
                history.setString(2, resolvedName);
                history.setString(3, "BAN");
                history.setString(4, reason);
                history.setString(5, executor);
                history.setString(6, ProxyBanSystem.currentHistoryDate());
                history.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(ProxyBanSystem.message("ban-save-failed"));
            return;
        }

        ProxyBanSystem.broadcastBan(resolvedName, executor, reason);

        ProxyBanSystem.BanData data = ProxyBanSystem.bannedPlayers.get(uuid.toString());
        if (data == null) {
            ProxyBanSystem.bannedPlayers.put(
                    uuid.toString(),
                    new ProxyBanSystem.BanData(resolvedName, uuid.toString(), executor, reason)
            );
        } else {
            data.increment(executor, reason);
        }

        ProxyBanSystem.saveBannedPlayers();

        if (target != null) {
            target.disconnect(ProxyBanSystem.formatBanMessage(message));
        }

        source.sendMessage(ProxyBanSystem.message("ban-success"));
    }

    @Override
    public List<String> suggest(ProxyCommandSource source, String[] args) {
        if (args.length == 1) {
            Set<String> list = new HashSet<>();

            if (ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
                list.addAll(ProxyBanSystem.mysql.getRegisteredPlayers());
            }

            for (proxyBanSystem.platform.ProxyPlayer player : adapter.getAllPlayers()) {
                list.add(player.getUsername());
            }

            return list.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            return ProxyBanSystem.getBanReasonSuggestions().stream()
                    .filter(reason -> reason.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
