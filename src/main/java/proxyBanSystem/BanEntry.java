package proxyBanSystem;

public class BanEntry {

    private final String reason;
    private final String message;
    private final long banUntil;

    public BanEntry(String reason, String message, long banUntil) {
        this.reason = reason;
        this.message = message;
        this.banUntil = banUntil;
    }

    public String getReason() {
        return reason;
    }

    public String getMessage() {
        return message;
    }

    public long getBanUntil() {
        return banUntil;
    }
}
