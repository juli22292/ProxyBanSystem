package proxyBanSystem;

import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;
import proxyBanSystem.platform.ProxyPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupRemoveCommand implements ProxyCommand {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9A-FK-OR]");

    private final ProxyAdapter adapter;

    public LookupRemoveCommand(ProxyAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void execute(ProxyCommandSource source, String[] args) {
        if (source.isPlayer() && !source.hasPermission("punishsystem.admin.use")) {
            source.sendMessage(ProxyBanSystem.message("no-permission"));
            return;
        }

        if (args.length < 1) {
            sendUsage(source);
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

            List<BanReason> reasons = getBanReasons(connection, target.uuid().toString());

            if (reasons.isEmpty()) {
                source.sendMessage(ProxyBanSystem.message("no-ban-reasons"));
                return;
            }

            if (args.length < 2) {
                sendReasonList(source, target, reasons);
                return;
            }

            String selector = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            BanReason selectedReason = findReason(reasons, selector);

            if (selectedReason == null) {
                source.sendMessage(ProxyBanSystem.message("ban-reason-not-found"));
                sendReasonList(source, target, reasons);
                return;
            }

            deleteReason(connection, target.uuid().toString(), selectedReason.id());
            updateActiveBanReason(connection, target.uuid().toString(), selectedReason.reason());
            refreshBanData(connection, target);

            source.sendMessage(ProxyBanSystem.message(
                    "ban-reason-removed",
                    ProxyBanSystem.placeholders("reason", displayReason(selectedReason.reason()))
            ));
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(ProxyBanSystem.message("ban-reason-remove-failed"));
        }
    }

    private void sendUsage(ProxyCommandSource source) {
        source.sendMessage(ProxyBanSystem.message("lookupremove-usage"));
    }

    private void sendReasonList(ProxyCommandSource source, Target target, List<BanReason> reasons) {
        List<String> entries = new ArrayList<>();

        for (int i = 0; i < reasons.size(); i++) {
            entries.add(ProxyBanSystem.format(
                    "lookupremove-entry",
                    ProxyBanSystem.placeholders(
                            "index", String.valueOf(i + 1),
                            "reason", displayReason(reasons.get(i).reason())
                    )
            ));
        }

        source.sendMessage(ProxyBanSystem.formatList(
                "lookupremove-list",
                ProxyBanSystem.placeholders(
                        "player", target.name(),
                        "entries", String.join("\n", entries)
                )
        ));
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

    private List<BanReason> getBanReasons(Connection connection, String uuid) throws SQLException {
        List<BanReason> reasons = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, reason FROM punishment_history WHERE uuid=? AND UPPER(type)='BAN' ORDER BY id DESC"
        )) {
            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String reason = rs.getString("reason");
                    if (reason != null && !reason.isBlank()) {
                        reasons.add(new BanReason(rs.getInt("id"), reason));
                    }
                }
            }
        }

        return reasons;
    }

    private BanReason findReason(List<BanReason> reasons, String selector) {
        String cleanSelector = selector == null ? "" : selector.trim();
        if (cleanSelector.isEmpty()) {
            return null;
        }

        Matcher numberMatcher = Pattern.compile("^#?(\\d+)(?:[_\\s].*)?$").matcher(cleanSelector);

        if (numberMatcher.matches()) {
            int index = Integer.parseInt(numberMatcher.group(1)) - 1;
            if (index >= 0 && index < reasons.size()) {
                return reasons.get(index);
            }
        }

        String normalizedSelector = normalizeForMatch(cleanSelector);

        for (BanReason reason : reasons) {
            String normalizedReason = normalizeForMatch(reason.reason());

            if (normalizedReason.equals(normalizedSelector) ||
                    normalizedReason.contains(normalizedSelector) ||
                    normalizedSelector.contains(normalizedReason)) {

                return reason;
            }
        }

        return null;
    }

    private void deleteReason(Connection connection, String uuid, int reasonId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM punishment_history WHERE id=? AND uuid=? AND UPPER(type)='BAN'"
        )) {
            ps.setInt(1, reasonId);
            ps.setString(2, uuid);
            ps.executeUpdate();
        }
    }

    private void updateActiveBanReason(Connection connection, String uuid, String removedReason) throws SQLException {
        String activeReason = null;
        long banUntil = -1;

        try (PreparedStatement ps = connection.prepareStatement("SELECT reason, ban_until FROM bans WHERE uuid=?")) {
            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    activeReason = rs.getString("reason");
                    banUntil = rs.getLong("ban_until");
                }
            }
        }

        if (activeReason == null || !normalizeForMatch(activeReason).equals(normalizeForMatch(removedReason))) {
            return;
        }

        String replacementReason = getLatestBanReason(connection, uuid);
        if (replacementReason == null || replacementReason.isBlank()) {
            replacementReason = ProxyBanSystem.configuredUnknown();
        }
        replacementReason = ProxyBanSystem.normalizeText(replacementReason);

        try (PreparedStatement ps = connection.prepareStatement("UPDATE bans SET reason=?, message=? WHERE uuid=?")) {
            ps.setString(1, replacementReason);
            ps.setString(2, ProxyBanSystem.buildBanMessage(replacementReason, banUntil));
            ps.setString(3, uuid);
            ps.executeUpdate();
        }
    }

    private String getLatestBanReason(Connection connection, String uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT reason FROM punishment_history WHERE uuid=? AND UPPER(type)='BAN' ORDER BY id DESC LIMIT 1"
        )) {
            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reason");
                }
            }
        }

        return null;
    }

    private void refreshBanData(Connection connection, Target target) throws SQLException {
        int count = 0;
        String lastDate = null;
        List<String> executors = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT executor, reason, date FROM punishment_history WHERE uuid=? AND UPPER(type)='BAN' ORDER BY id ASC"
        )) {
            ps.setString(1, target.uuid().toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;

                    String executor = rs.getString("executor");
                    if (executor != null && !executor.isBlank()) {
                        executors.add(ProxyBanSystem.formatExecutorWithReason(executor, rs.getString("reason")));
                    }

                    String date = rs.getString("date");
                    if (date != null && !date.isBlank()) {
                        lastDate = date;
                    }
                }
            }
        }

        if (count == 0) {
            ProxyBanSystem.bannedPlayers.remove(target.uuid().toString());
        } else {
            ProxyBanSystem.BanData data = new ProxyBanSystem.BanData(target.name(), target.uuid().toString());
            data.banCount = count;
            data.gebanntVon = executors;

            if (lastDate != null) {
                data.lastBanDateTime = lastDate;
            }

            ProxyBanSystem.bannedPlayers.put(target.uuid().toString(), data);
        }

        ProxyBanSystem.saveBannedPlayers();
    }

    private String displayReason(String reason) {
        return ProxyBanSystem.normalizeText(reason);
    }

    private String normalizeForMatch(String value) {
        return stripColors(ProxyBanSystem.normalizeText(value))
                .replace('»', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String stripColors(String value) {
        if (value == null) {
            return "";
        }

        return LEGACY_COLOR_PATTERN.matcher(value).replaceAll("");
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

        if (args.length >= 2 && ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
            Target target = resolveTarget(args[0]);
            if (target == null) {
                return List.of();
            }

            try {
                Connection connection = ProxyBanSystem.mysql.getConnection();
                if (connection == null) {
                    return List.of();
                }

                List<BanReason> reasons = getBanReasons(connection, target.uuid().toString());
                String currentInput = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String normalizedInput = normalizeForMatch(currentInput);

                List<String> suggestions = new ArrayList<>();

                for (int i = 0; i < reasons.size(); i++) {
                    String suggestion = createReasonSuggestion(i + 1, reasons.get(i).reason());

                    if (normalizedInput.isEmpty() ||
                            normalizeForMatch(suggestion).startsWith(normalizedInput) ||
                            normalizeForMatch(reasons.get(i).reason()).contains(normalizedInput)) {

                        suggestions.add(suggestion);
                    }
                }

                return suggestions;
            } catch (Exception ignored) {
            }
        }

        return List.of();
    }

    private String createReasonSuggestion(int index, String reason) {
        String cleanReason = stripColors(ProxyBanSystem.normalizeText(reason))
                .replace('»', ' ')
                .replaceAll("[^\\p{L}\\p{N}]+", "_")
                .replaceAll("^_+|_+$", "");

        if (cleanReason.isBlank()) {
            cleanReason = ProxyBanSystem.reasonSuggestionFallback();
        }

        return index + "_" + cleanReason;
    }

    private record Target(UUID uuid, String name) {
    }

    private record BanReason(int id, String reason) {
    }
}
