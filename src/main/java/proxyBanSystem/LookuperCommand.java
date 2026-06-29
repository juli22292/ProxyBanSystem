package proxyBanSystem;

import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;
import proxyBanSystem.platform.ProxyPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LookuperCommand implements ProxyCommand {

    private final ProxyAdapter adapter;

    public LookuperCommand(ProxyAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void execute(ProxyCommandSource source, String[] args) {
        if (source.isPlayer() && !source.hasPermission("punishsystem.admin.use")) {
            source.sendMessage(ProxyBanSystem.message("no-permission"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(ProxyBanSystem.message("lookup-usage"));
            return;
        }

        if (ProxyBanSystem.mysql == null || !ProxyBanSystem.mysql.isConnected()) {
            source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
            return;
        }

        String input = args[0];

        UUID uuid = null;
        String playerName = input;

        ProxyPlayer onlinePlayer = adapter.getPlayer(input);
        if (onlinePlayer != null) {
            uuid = onlinePlayer.getUniqueId();
            playerName = onlinePlayer.getUsername();
        }

        if (uuid == null) {
            try {
                uuid = UUID.fromString(input);

                for (Map.Entry<UUID, String> entry : ProxyBanSystem.players.entrySet()) {
                    if (entry.getKey().equals(uuid)) {
                        playerName = entry.getValue();
                        break;
                    }
                }
            } catch (Exception ignored) {
                for (UUID id : ProxyBanSystem.players.keySet()) {
                    String cachedName = ProxyBanSystem.players.get(id);
                    if (cachedName != null && cachedName.equalsIgnoreCase(input)) {
                        uuid = id;
                        playerName = cachedName;
                        break;
                    }
                }
            }
        }

        if (uuid != null && (playerName == null || playerName.equals(input))) {
            String regName = ProxyBanSystem.mysql.getNameByUUID(uuid.toString());
            if (regName != null && !regName.isBlank()) {
                playerName = regName;
            }
        }

        if (uuid == null) {
            String uuidString = ProxyBanSystem.mysql.getUUIDByName(input);
            if (uuidString != null) {
                try {
                    uuid = UUID.fromString(uuidString);
                    String regName = ProxyBanSystem.mysql.getNameByUUID(uuidString);
                    if (regName != null && !regName.isBlank()) {
                        playerName = regName;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (uuid == null) {
            source.sendMessage(ProxyBanSystem.message("player-not-found"));
            return;
        }

        int banCount = 0;
        int unbanCount = 0;

        String reason = ProxyBanSystem.configuredUnknown();
        String bannedBy = ProxyBanSystem.configuredUnknown();
        String allBannedBy = ProxyBanSystem.configuredNone();
        String unbannedBy = ProxyBanSystem.configuredUnknown();
        String bannedAt = ProxyBanSystem.configuredUnknown();
        String bannedUntil = ProxyBanSystem.configuredNotBanned();
        boolean currentlyBanned = false;

        ProxyBanSystem.BanData banData = ProxyBanSystem.bannedPlayers.get(uuid.toString());
        if (banData != null) {
            banCount = banData.banCount;
            if (banData.gebanntVon != null && !banData.gebanntVon.isEmpty()) {
                allBannedBy = joinBannedByEntries(banData.gebanntVon);
            }
        }

        try {
            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
                return;
            }

            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM bans WHERE uuid=?")) {
                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentlyBanned = true;

                        String dbReason = rs.getString("reason");
                        if (dbReason != null && !dbReason.isBlank()) {
                            reason = ProxyBanSystem.normalizeText(dbReason);
                        }

                        long until = rs.getLong("ban_until");
                        if (until == -1) {
                            bannedUntil = ProxyBanSystem.configuredPermanent();
                        } else {
                            bannedUntil = ProxyBanSystem.formatDate(until);
                        }
                    }
                }
            }

            int historyBanCount = 0;
            int historyUnbanCount = 0;
            boolean latestBanFound = false;
            boolean latestUnbanFound = false;
            List<String> allBannedByList = new ArrayList<>();

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT * FROM punishment_history WHERE uuid=? ORDER BY id DESC"
            )) {
                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("type");
                        if (type == null) {
                            continue;
                        }

                        if (type.equalsIgnoreCase("BAN")) {
                            historyBanCount++;

                            String exec = rs.getString("executor");
                            if (exec == null || exec.isBlank()) {
                                exec = ProxyBanSystem.configuredUnknown();
                            }

                            String histReason = rs.getString("reason");
                            if (histReason == null || histReason.isBlank()) {
                                histReason = ProxyBanSystem.configuredUnknown();
                            }

                            histReason = ProxyBanSystem.normalizeText(histReason);
                            String executorWithReason = ProxyBanSystem.formatExecutorWithReason(exec, histReason);
                            allBannedByList.add(executorWithReason);

                            if (!latestBanFound) {
                                bannedBy = executorWithReason;
                                reason = histReason;

                                String histDate = rs.getString("date");
                                if (histDate != null && !histDate.isBlank()) {
                                    bannedAt = histDate;
                                }

                                latestBanFound = true;
                            }
                        } else if (type.equalsIgnoreCase("UNBAN")) {
                            historyUnbanCount++;

                            if (!latestUnbanFound) {
                                String exec = rs.getString("executor");
                                if (exec != null && !exec.isBlank()) {
                                    unbannedBy = exec;
                                }

                                latestUnbanFound = true;
                            }
                        }
                    }
                }
            }

            if (historyBanCount > 0 || historyUnbanCount > 0) {
                banCount = historyBanCount;
                unbanCount = historyUnbanCount;
            }

            if (!allBannedByList.isEmpty()) {
                allBannedBy = joinBannedByEntries(allBannedByList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        reason = ProxyBanSystem.normalizeText(reason);

        source.sendMessage(ProxyBanSystem.formatList(
                "lookup",
                ProxyBanSystem.placeholders(
                        "player", playerName,
                        "uuid", uuid.toString(),
                        "status", ProxyBanSystem.message(
                                currentlyBanned ? "lookup-status-active" : "lookup-status-inactive"
                        ),
                        "banned_at", bannedAt,
                        "banned_until", bannedUntil,
                        "reason", reason,
                        "ban_count", String.valueOf(banCount),
                        "banned_by", bannedBy,
                        "all_banned_by", allBannedBy,
                        "unban_count", String.valueOf(unbanCount),
                        "unbanned_by", unbannedBy
                )
        ));
    }

    private String joinBannedByEntries(List<String> entries) {
        return String.join(ProxyBanSystem.configuredListSeparator() + "&f", entries);
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
                    .filter(name -> name != null && !name.isBlank())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return List.of();
    }
}
