package proxyBanSystem.platform;

public interface ProxyCommandSource {
    void sendMessage(String message);

    boolean hasPermission(String permission);

    String getName();

    default boolean isPlayer() {
        return false;
    }
}
