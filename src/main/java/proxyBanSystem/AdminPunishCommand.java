package proxyBanSystem;

import proxyBanSystem.platform.ProxyCommand;
import proxyBanSystem.platform.ProxyCommandSource;

import java.util.List;

public class AdminPunishCommand implements ProxyCommand {

    @Override
    public void execute(ProxyCommandSource source, String[] args) {
        if (source.isPlayer() && !source.hasPermission("punishsystem.admin.use")) {
            source.sendMessage(ProxyBanSystem.message("no-permission"));
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase(ProxyBanSystem.Config.adminPunishHelpSubCommand)) {
            sendHelp(source);
            return;
        }

        if (args[0].equalsIgnoreCase(ProxyBanSystem.Config.adminPunishReloadSubCommand)) {
            if (ProxyBanSystem.reloadConfig()) {
                source.sendMessage(ProxyBanSystem.message("admin-reload-success"));
            } else {
                source.sendMessage(ProxyBanSystem.message("admin-reload-failed"));
            }

            return;
        }

        source.sendMessage(ProxyBanSystem.message("admin-unknown-subcommand"));
    }

    private void sendHelp(ProxyCommandSource source) {
        source.sendMessage(ProxyBanSystem.formatList("admin-help", ProxyBanSystem.placeholders()));
    }

    @Override
    public List<String> suggest(ProxyCommandSource source, String[] args) {
        if (args.length == 1) {
            return List.of(
                            ProxyBanSystem.Config.adminPunishReloadSubCommand,
                            ProxyBanSystem.Config.adminPunishHelpSubCommand
                    ).stream()
                    .filter(subCommand -> subCommand.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
