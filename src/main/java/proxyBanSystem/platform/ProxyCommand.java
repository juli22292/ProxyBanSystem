package proxyBanSystem.platform;

import java.util.List;

public interface ProxyCommand {
    void execute(ProxyCommandSource source, String[] args);

    default List<String> suggest(ProxyCommandSource source, String[] args) {
        return List.of();
    }
}
