package proxyBanSystem;

import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PunishmentCommand implements ProxyCommand {

    private final ProxyAdapter adapter;

    public PunishmentCommand(ProxyAdapter adapter) {
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

        String subCommand = args[0];

        if (subCommand.equalsIgnoreCase(ProxyBanSystem.Config.punishmentListAllBansSubCommand)) {
            sendAllBans(source);
            return;
        }

        if (subCommand.equalsIgnoreCase(ProxyBanSystem.Config.punishmentReloadSubCommand)) {
            if (ProxyBanSystem.reloadConfig()) {
                source.sendMessage(ProxyBanSystem.message("admin-reload-success"));
            } else {
                source.sendMessage(ProxyBanSystem.message("admin-reload-failed"));
            }

            return;
        }

        if (subCommand.equalsIgnoreCase(ProxyBanSystem.Config.punishmentWhitelistArgument) &&
                args.length >= 3 &&
                (args[1].equalsIgnoreCase(ProxyBanSystem.Config.punishmentAddSubCommand) ||
                        args[1].equalsIgnoreCase(ProxyBanSystem.Config.punishmentRemoveSubCommand))) {

            updateWhitelist(source, args[1], args[2]);
            return;
        }

        sendUsage(source);
    }

    private void sendAllBans(ProxyCommandSource source) {
        if (ProxyBanSystem.mysql == null || !ProxyBanSystem.mysql.isConnected()) {
            source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
            return;
        }

        try {
            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                source.sendMessage(ProxyBanSystem.message("mysql-not-connected"));
                return;
            }

            List<String> entries = new ArrayList<>();

            try (PreparedStatement ps = connection.prepareStatement("SELECT uuid, name FROM bans ORDER BY name ASC");
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    entries.add(ProxyBanSystem.format(
                            "punishment-list-entry",
                            ProxyBanSystem.placeholders(
                                    "uuid", rs.getString("uuid"),
                                    "name", rs.getString("name")
                            )
                    ));
                }
            }

            if (entries.isEmpty()) {
                source.sendMessage(ProxyBanSystem.message("punishment-list-empty"));
                return;
            }

            source.sendMessage(ProxyBanSystem.formatList(
                    "punishment-list",
                    ProxyBanSystem.placeholders("entries", String.join("\n", entries))
            ));
        } catch (Exception e) {
            e.printStackTrace();
            source.sendMessage(ProxyBanSystem.message("ban-save-failed"));
        }
    }

    private void updateWhitelist(ProxyCommandSource source, String subCommand, String target) {
        boolean add = subCommand.equalsIgnoreCase(ProxyBanSystem.Config.punishmentAddSubCommand);

        if (add && ProxyBanSystem.isWhitelisted(null, target)) {
            source.sendMessage(ProxyBanSystem.message(
                    "punishment-whitelist-already-added",
                    ProxyBanSystem.placeholders("player", target)
            ));
            return;
        }

        if (!add && !ProxyBanSystem.isWhitelisted(null, target)) {
            source.sendMessage(ProxyBanSystem.message(
                    "punishment-whitelist-not-found",
                    ProxyBanSystem.placeholders("player", target)
            ));
            return;
        }

        boolean changed = add
                ? ProxyBanSystem.addWhitelistEntry(target)
                : ProxyBanSystem.removeWhitelistEntry(target);

        source.sendMessage(ProxyBanSystem.message(
                changed
                        ? (add ? "punishment-whitelist-added" : "punishment-whitelist-removed")
                        : "punishment-whitelist-save-failed",
                ProxyBanSystem.placeholders("player", target)
        ));
    }

    private void sendUsage(ProxyCommandSource source) {
        source.sendMessage(ProxyBanSystem.message("punishment-usage"));
    }

    @Override
    public List<String> suggest(ProxyCommandSource source, String[] args) {
        if (args.length == 1) {
            return List.of(
                            ProxyBanSystem.Config.punishmentListAllBansSubCommand,
                            ProxyBanSystem.Config.punishmentReloadSubCommand,
                            ProxyBanSystem.Config.punishmentWhitelistArgument
                    ).stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 2 &&
                args[0].equalsIgnoreCase(ProxyBanSystem.Config.punishmentWhitelistArgument)) {

            return List.of(
                            ProxyBanSystem.Config.punishmentAddSubCommand,
                            ProxyBanSystem.Config.punishmentRemoveSubCommand
                    ).stream()
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if (args.length == 3 &&
                args[0].equalsIgnoreCase(ProxyBanSystem.Config.punishmentWhitelistArgument) &&
                (args[1].equalsIgnoreCase(ProxyBanSystem.Config.punishmentAddSubCommand) ||
                        args[1].equalsIgnoreCase(ProxyBanSystem.Config.punishmentRemoveSubCommand))) {

            List<String> suggestions = new ArrayList<>();
            suggestions.addAll(ProxyBanSystem.players.values());

            if (ProxyBanSystem.mysql != null && ProxyBanSystem.mysql.isConnected()) {
                suggestions.addAll(ProxyBanSystem.mysql.getRegisteredPlayers());
            }

            return suggestions.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return List.of();
    }
}
