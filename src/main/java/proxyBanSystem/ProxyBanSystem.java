package proxyBanSystem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import proxyBanSystem.platform.ProxyAdapter;
import proxyBanSystem.platform.ProxyPlayer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
public class ProxyBanSystem {

    private static ProxyBanSystem instance;
    private static ProxyAdapter proxyAdapter;

    public static MySQL mysql;
    public static Map<UUID, String> players = new HashMap<>();

    public static File bannedFile;

    public static Map<String, BanData> bannedPlayers = new LinkedHashMap<>();

    public static Set<String> whitelistedPlayers = new HashSet<>();

    private static final Map<String, String> MESSAGES = new HashMap<>();
    private static final Map<String, String> FORMATS = new HashMap<>();
    private static final Map<String, List<String>> FORMAT_LISTS = new HashMap<>();
    private static final Map<String, BanReasonConfig> BAN_REASONS = new LinkedHashMap<>();
    private static final Map<String, BanReasonConfig> BAN_REASONS_BY_DISPLAY = new HashMap<>();

    private final ProxyAdapter adapter;
    private final Yaml yaml = new Yaml();
    private final File configFile;
    private final File messagesFile;

    private static final List<String> CONFIG_SETTING_KEYS = List.of(
            "prefix",
            "ban-title",
            "ban-broadcast-prefix",
            "unban-broadcast-prefix"
    );

    private static final List<String> MESSAGE_SETTING_KEYS = List.of(
            "timezone",
            "date-format",
            "history-date-format",
            "line",
            "list-separator",
            "console-name",
            "unknown",
            "none",
            "permanent",
            "not-banned",
            "zero-duration",
            "reason-suggestion-fallback",
            "player-history-reason-format",
            "ban-message-body-marker",
            "discord-link"
    );

    private static final List<String> MESSAGE_TOP_LEVEL_KEYS = List.of(
            "commands",
            "messages",
            "formats",
            "duration",
            "default-ban-reason",
            "ban-reasons",
            "whitelist"
    );

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String PREFIX = "&8[&c&lBANSYSTEM&r&8] &8» ";
    public static String BAN_TITLE = "&8[&c&lBANSYSTEM&r&8]";
    public static String BAN_BROADCAST_PREFIX = "&c&lBAN &8» ";
    public static String UNBAN_BROADCAST_PREFIX = "&a&lUNBAN &8» ";

    public static class Config {
        static String host = "localhost";
        static int port = 3306;
        static String database = "proxybans";
        static String user = "root";
        static String password = "password";
        static String jdbcParameters =
                "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&useUnicode=true&characterEncoding=UTF-8";

        static String punishCommand = "punish";
        static String unpunishCommand = "unpunish";
        static String lookupCommand = "lookup";
        static String lookupRemoveCommand = "lookupremove";
        static String lookupFullRemoveCommand = "lookupfullremove";
        static String punishmentCommand = "punishment";
        static String adminPunishCommand = "adminpunish";
        static String adminPunishReloadSubCommand = "reload";
        static String adminPunishHelpSubCommand = "help";
        static String punishmentListAllBansSubCommand = "listallbans";
        static String punishmentReloadSubCommand = "reload";
        static String punishmentAddSubCommand = "add";
        static String punishmentRemoveSubCommand = "remove";
        static String punishmentWhitelistArgument = "whitelist";

        static String timezone = "Europe/Berlin";
        static String dateFormat = "dd.MM.yyyy HH:mm:ss";
        static String historyDateFormat = "dd-MM-yyyy HH:mm:ss";
        static String line = "&8&m----------------------------------------";
        static String listSeparator = ", ";
        static String consoleName = "Console";
        static String unknown = "Unbekannt";
        static String none = "Keine Einträge";
        static String permanent = "Permanent";
        static String notBanned = "Nicht gebannt";
        static String zeroDuration = "0 Minuten";
        static String reasonSuggestionFallback = "Grund";
        static String playerHistoryReasonFormat = "{executor} &8(&c{reason}&8)";
        static String banMessageBodyMarker = "&7Du wurdest";
        static String discordLink = "deinserver";

        static String daySingular = "Tag";
        static String dayPlural = "Tage";
        static String hourSingular = "Stunde";
        static String hourPlural = "Stunden";
        static String minuteSingular = "Minute";
        static String minutePlural = "Minuten";

        static String defaultReasonDisplay = "&7{input} &8(unspezifisch)";
        static String defaultReasonDuration = "1d";
    }

    public record BanReasonConfig(String key, String display, String duration, long durationMillis) {
    }

    public static class BanData {
        String name;
        String uuid;
        String lastBanDateTime;
        int banCount;
        List<String> gebanntVon = new ArrayList<>();
        boolean currentlyBanned;
        String activeReason = "";
        long activeBanUntil = -1;
        String activeBanUntilFormatted = "";
        String remainingFormatted = "";

        public BanData(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
            this.banCount = 1;
            this.lastBanDateTime = currentHistoryDate();
        }

        public BanData(String name, String uuid, String executor) {
            this(name, uuid, executor, null);
        }

        public BanData(String name, String uuid, String executor, String reason) {
            this(name, uuid);
            addExecutor(executor, reason);
        }

        public void increment(String executor) {
            increment(executor, null);
        }

        public void increment(String executor, String reason) {
            this.banCount++;
            this.lastBanDateTime = currentHistoryDate();
            addExecutor(executor, reason);
        }

        public void addExecutor(String executor, String reason) {
            if (gebanntVon == null) {
                gebanntVon = new ArrayList<>();
            }

            if (executor != null && !executor.isBlank()) {
                gebanntVon.add(formatExecutorWithReason(executor, reason));
            }
        }

        public void updateActiveBan(BanEntry entry) {
            if (entry == null) {
                currentlyBanned = false;
                activeReason = "";
                activeBanUntil = -1;
                activeBanUntilFormatted = configuredNotBanned();
                remainingFormatted = configuredNotBanned();
                return;
            }

            currentlyBanned = true;
            activeReason = normalizeText(entry.getReason());
            activeBanUntil = entry.getBanUntil();

            if (activeBanUntil == -1) {
                activeBanUntilFormatted = configuredPermanent();
                remainingFormatted = configuredPermanent();
            } else {
                activeBanUntilFormatted = formatDate(activeBanUntil);
                remainingFormatted = formatDuration(Math.max(0, activeBanUntil - System.currentTimeMillis()));
            }
        }
    }

    private ProxyBanSystem(ProxyAdapter adapter) {
        this.adapter = adapter;
        File dataDirectory = adapter.getDataDirectory();
        this.configFile = new File(dataDirectory, "config.yml");
        this.messagesFile = new File(dataDirectory, "messages.yml");
    }

    public static synchronized void initialize(ProxyAdapter adapter) {
        proxyAdapter = adapter;
        instance = new ProxyBanSystem(adapter);

        instance.loadConfig();

        adapter.info("=================================");
        adapter.info(" ProxyBanSystem wird gestartet...");
        adapter.info("=================================");

        instance.loadBannedPlayers();

        adapter.info("[1/3] Konfiguration geladen (OK)");
        adapter.info("[2/3] bannedplayers.json geladen (OK)");

        mysql = new MySQL(
                Config.host,
                Config.port,
                Config.database,
                Config.user,
                Config.password,
                Config.jdbcParameters
        );

        try {
            mysql.connect();

            if (mysql.isConnected()) {
                adapter.info("[3/3] MySQL Verbindung erfolgreich (OK)");
            } else {
                adapter.error("[3/3] MySQL Verbindung fehlgeschlagen (Connection null oder geschlossen)");
            }
        } catch (Exception e) {
            adapter.error("[3/3] MySQL Verbindung fehlgeschlagen", e);
        }

        instance.registerCommands();
        adapter.info("ProxyBanSystem gestartet");
    }

    private void registerCommands() {
        adapter.registerCommand(Config.punishCommand, new PunishCommand(adapter));
        adapter.registerCommand(Config.unpunishCommand, new UnpunishCommand(adapter));
        adapter.registerCommand(Config.lookupCommand, new LookuperCommand(adapter));
        adapter.registerCommand(Config.lookupRemoveCommand, new LookupRemoveCommand(adapter));
        adapter.registerCommand(Config.lookupFullRemoveCommand, new LookupFullRemoveCommand(adapter));
        adapter.registerCommand(Config.punishmentCommand, new PunishmentCommand(adapter));
        adapter.registerCommand(Config.adminPunishCommand, new AdminPunishCommand());
    }

    public static String withPrefix(String message) {
        String prefix = PREFIX == null ? "" : PREFIX;
        String text = message == null ? "" : message;

        if (prefix.isEmpty() || text.startsWith(prefix)) {
            return text;
        }

        return prefix + text;
    }

    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace(">>", "»")
                .replace("\u00C2\u00BB", "»")
                .replace("\u00C3\u00A4", "ä")
                .replace("\u00C3\u00B6", "ö")
                .replace("\u00C3\u00BC", "ü")
                .replace("\u00C3\u0084", "Ä")
                .replace("\u00C3\u0096", "Ö")
                .replace("\u00C3\u009C", "Ü")
                .replace("\u00C3\u009F", "ß");
    }

    public static String formatBanMessage(String message) {
        String text = message;

        if (text == null || text.isBlank()) {
            text = message("fallback-ban-message");
        }

        if (!Config.banMessageBodyMarker.isEmpty()) {
            int bodyStart = text.indexOf(Config.banMessageBodyMarker);
            if (bodyStart > 0) {
                text = text.substring(bodyStart);
            }
        }

        text = normalizeText(text);

        String configuredTitle = BAN_TITLE == null ? "" : BAN_TITLE;
        if (configuredTitle.isBlank()) {
            return text;
        }

        if (text.startsWith(configuredTitle)) {
            text = text.substring(configuredTitle.length()).replaceFirst("^(\\r?\\n)+", "");
        }

        String screen = formatList(
                "ban-screen",
                placeholders("message", text)
        );

        if (screen.isBlank()) {
            return configuredTitle + "\n\n" + text;
        }

        return screen;
    }

    public static String message(String key) {
        return message(key, Collections.emptyMap());
    }

    public static String message(String key, Map<String, String> placeholders) {
        return applyPlaceholders(MESSAGES.getOrDefault(key, key), placeholders);
    }

    public static String format(String key, Map<String, String> placeholders) {
        return applyPlaceholders(FORMATS.getOrDefault(key, ""), placeholders);
    }

    public static String formatList(String key, Map<String, String> placeholders) {
        List<String> lines = FORMAT_LISTS.get(key);
        if (lines == null) {
            return format(key, placeholders);
        }

        List<String> formatted = new ArrayList<>();
        for (String line : lines) {
            formatted.add(applyPlaceholders(line, placeholders));
        }

        return String.join("\n", formatted);
    }

    public static Map<String, String> placeholders(String... values) {
        Map<String, String> placeholders = new LinkedHashMap<>();

        if (values == null) {
            return placeholders;
        }

        for (int i = 0; i + 1 < values.length; i += 2) {
            placeholders.put(values[i], values[i + 1]);
        }

        return placeholders;
    }

    public static String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        Map<String, String> values = new LinkedHashMap<>();

        values.put("prefix", PREFIX);
        values.put("ban_title", BAN_TITLE);
        values.put("ban_broadcast_prefix", BAN_BROADCAST_PREFIX);
        values.put("unban_broadcast_prefix", UNBAN_BROADCAST_PREFIX);
        values.put("punish_command", Config.punishCommand);
        values.put("unpunish_command", Config.unpunishCommand);
        values.put("lookup_command", Config.lookupCommand);
        values.put("lookupremove_command", Config.lookupRemoveCommand);
        values.put("lookupfullremove_command", Config.lookupFullRemoveCommand);
        values.put("punishment_command", Config.punishmentCommand);
        values.put("adminpunish_command", Config.adminPunishCommand);
        values.put("adminpunish_reload_subcommand", Config.adminPunishReloadSubCommand);
        values.put("adminpunish_help_subcommand", Config.adminPunishHelpSubCommand);
        values.put("punishment_listallbans_subcommand", Config.punishmentListAllBansSubCommand);
        values.put("punishment_reload_subcommand", Config.punishmentReloadSubCommand);
        values.put("punishment_add_subcommand", Config.punishmentAddSubCommand);
        values.put("punishment_remove_subcommand", Config.punishmentRemoveSubCommand);
        values.put("punishment_whitelist_argument", Config.punishmentWhitelistArgument);
        values.put("line", Config.line);
        values.put("console", Config.consoleName);
        values.put("unknown", Config.unknown);
        values.put("none", Config.none);
        values.put("permanent", Config.permanent);
        values.put("not_banned", Config.notBanned);
        values.put("discord_link", Config.discordLink);

        if (placeholders != null) {
            values.putAll(placeholders);
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue() == null ? "" : entry.getValue()
            );
        }

        return normalizeText(result);
    }

    public static void broadcastBan(String player, String executor, String reason) {

        if (proxyAdapter == null) return;

        String text = format(
                "ban-broadcast",
                placeholders(
                        "player", player,
                        "gebannter_spieler", player,
                        "executor", executor,
                        "banausführer", executor,
                        "banausfuehrer", executor,
                        "bangrund", reason,
                        "reason", reason
                )
        );

        for (ProxyPlayer p : proxyAdapter.getAllPlayers()) {

            if (p.hasPermission("punishsystem.admin.seebans")) {

                p.sendMessage(text);
            }
        }
    }

    public static void broadcastUnban(String player, String executor) {

        if (proxyAdapter == null) return;

        String text = format(
                "unban-broadcast",
                placeholders(
                        "player", player,
                        "executor", executor
                )
        );

        for (ProxyPlayer p : proxyAdapter.getAllPlayers()) {

            if (p.hasPermission("punishsystem.admin.seebans")) {

                p.sendMessage(text);
            }
        }
    }

    public static boolean reloadConfig() {
        if (instance == null) {
            return false;
        }

        instance.loadConfig();
        return true;
    }

    public static boolean isWhitelisted(UUID uuid, String name) {
        if (uuid != null && whitelistedPlayers.contains(uuid.toString().toLowerCase(Locale.ROOT))) {
            return true;
        }

        return name != null && whitelistedPlayers.contains(name.toLowerCase(Locale.ROOT));
    }

    public static List<String> getBanReasonSuggestions() {
        List<String> suggestions = new ArrayList<>();

        for (BanReasonConfig reason : BAN_REASONS.values()) {
            String suggestion = stripLegacyColors(reason.display()).trim();

            if (suggestion.isBlank() || suggestion.contains(" ")) {
                suggestion = reason.key();
            }

            suggestions.add(normalizeText(suggestion));
        }

        return suggestions.stream().distinct().toList();
    }

    public static BanReasonConfig resolveBanReason(String input) {
        String originalInput = input == null ? "" : input;
        String key = originalInput.toLowerCase(Locale.ROOT);
        BanReasonConfig reason = BAN_REASONS.get(key);

        if (reason != null) {
            return reason;
        }

        reason = BAN_REASONS_BY_DISPLAY.get(normalizeReasonLookup(originalInput));
        if (reason != null) {
            return reason;
        }

        String display = applyPlaceholders(
                Config.defaultReasonDisplay,
                placeholders("input", originalInput)
        );

        return new BanReasonConfig(
                key,
                display,
                Config.defaultReasonDuration,
                parseDurationMillis(Config.defaultReasonDuration)
        );
    }

    public static String buildBanMessage(String reason, long banUntil) {
        String expireText;
        String remainingText;
        String durationText;

        if (banUntil == -1) {
            expireText = Config.permanent;
            remainingText = Config.permanent;
            durationText = Config.permanent;
        } else {
            long remaining = Math.max(0, banUntil - System.currentTimeMillis());
            expireText = formatDate(banUntil);
            remainingText = formatDuration(remaining);
            durationText = remainingText;
        }

        return buildBanMessage(Config.unknown, reason, durationText, remainingText, expireText);
    }

    public static String buildBanMessage(
            String executor,
            String reason,
            String duration,
            String remaining,
            String expires
    ) {

        return formatList(
                "ban-message",
                placeholders(
                        "banausführer", executor,
                        "banausfuehrer", executor,
                        "executor", executor,
                        "grund", boldReason(reason),
                        "reason", boldReason(reason),
                        "raw_reason", reason,
                        "duration", duration,
                        "zeit", duration,
                        "remaining", remaining,
                        "verbleibend", remaining,
                        "verlbleibend", remaining,
                        "expires", expires,
                        "bis", expires,
                        "discord_link", Config.discordLink
                )
        );
    }

    public static String boldReason(String reason) {
        String cleanReason = normalizeText(reason);

        if (cleanReason.isBlank()) {
            return "";
        }

        String bolded = cleanReason.replaceAll("(?i)&([0-9A-F])", "&$1&l");
        if (!bolded.startsWith("&l") && !bolded.matches("(?i)^&[0-9A-F]&l.*")) {
            bolded = "&l" + bolded;
        }

        return bolded;
    }

    public static String stripLegacyColors(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("(?i)[&§][0-9A-FK-ORX]", "");
    }

    private static String normalizeReasonLookup(String value) {
        return stripLegacyColors(normalizeText(value))
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public static String formatExecutorWithReason(String executor, String reason) {
        String cleanExecutor = executor == null || executor.isBlank() ? Config.unknown : executor;
        String cleanReason = normalizeText(reason);

        if (cleanReason.isBlank()) {
            return cleanExecutor;
        }

        return applyPlaceholders(
                Config.playerHistoryReasonFormat,
                placeholders(
                        "executor", cleanExecutor,
                        "reason", cleanReason
                )
        );
    }

    public static String formatDate(long epochMillis) {
        try {
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(configuredZone())
                    .format(DateTimeFormatter.ofPattern(Config.dateFormat));
        } catch (Exception ignored) {
            return Instant.ofEpochMilli(epochMillis)
                    .atZone(ZoneId.of("Europe/Berlin"))
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        }
    }

    public static String currentHistoryDate() {
        try {
            return LocalDateTime.now(configuredZone())
                    .format(DateTimeFormatter.ofPattern(Config.historyDateFormat));
        } catch (Exception ignored) {
            return LocalDateTime.now(ZoneId.of("Europe/Berlin"))
                    .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        }
    }

    public static String formatDuration(long millis) {
        long seconds = Math.max(0, millis) / 1000;

        long days = seconds / 86400;
        seconds %= 86400;

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;

        StringBuilder builder = new StringBuilder();

        appendDurationPart(builder, days, Config.daySingular, Config.dayPlural);
        appendDurationPart(builder, hours, Config.hourSingular, Config.hourPlural);
        appendDurationPart(builder, minutes, Config.minuteSingular, Config.minutePlural);

        if (builder.isEmpty()) {
            return Config.zeroDuration;
        }

        return builder.toString().trim();
    }

    private static void appendDurationPart(StringBuilder builder, long amount, String singular, String plural) {
        if (amount <= 0) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append(' ');
        }

        builder.append(amount)
                .append(' ')
                .append(amount == 1 ? singular : plural);
    }

    public static String configuredUnknown() {
        return Config.unknown;
    }

    public static String configuredNone() {
        return Config.none;
    }

    public static String configuredPermanent() {
        return Config.permanent;
    }

    public static String configuredNotBanned() {
        return Config.notBanned;
    }

    public static String configuredListSeparator() {
        return Config.listSeparator;
    }

    public static String configuredConsoleName() {
        return Config.consoleName;
    }

    public static String reasonSuggestionFallback() {
        return Config.reasonSuggestionFallback;
    }

    public static boolean addWhitelistEntry(String value) {
        return instance != null && instance.updateWhitelistEntry(value, true);
    }

    public static boolean removeWhitelistEntry(String value) {
        return instance != null && instance.updateWhitelistEntry(value, false);
    }

    private static ZoneId configuredZone() {
        try {
            return ZoneId.of(Config.timezone);
        } catch (Exception ignored) {
            return ZoneId.of("Europe/Berlin");
        }
    }

    public void loadConfig() {

        try {
            Map<String, Object> configDefaults = loadDefaultYamlData("config.yml", fallbackConfigText());
            Map<String, Object> messagesDefaults = loadDefaultYamlData("messages.yml", fallbackMessagesText());

            boolean messagesFileExisted = messagesFile.exists();
            ensureYamlFile(configFile, "config.yml", fallbackConfigText());
            ensureYamlFile(messagesFile, "messages.yml", fallbackMessagesText());

            Map<String, Object> rawConfigData = readYamlFile(configFile, configDefaults);
            Map<String, Object> rawMessagesData = readYamlFile(messagesFile, messagesDefaults);

            Map<String, Object> configData = extractConfigFileData(rawConfigData);
            Map<String, Object> messagesData =
                    migrateLegacyMessages(rawConfigData, rawMessagesData, !messagesFileExisted);

            Map<String, Object> mergedConfig = mergeConfigForFile(configData, configDefaults);
            Map<String, Object> mergedMessages = mergeMessagesForFile(messagesData, messagesDefaults);

            if (!mergedConfig.equals(rawConfigData)) {
                writeYaml(configFile, mergedConfig);
            }

            if (!mergedMessages.equals(rawMessagesData)) {
                writeYaml(messagesFile, mergedMessages);
            }

            loadConfigValues(mergedConfig, configDefaults, mergedMessages, messagesDefaults);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void loadConfigValues(
            Map<String, Object> configData,
            Map<String, Object> configDefaults,
            Map<String, Object> messagesData,
            Map<String, Object> messagesDefaults
    ) {
        Map<String, Object> settings = mergedSection(configData, configDefaults, "settings");

        PREFIX = getString(settings, "prefix", PREFIX);
        BAN_TITLE = getString(settings, "ban-title", BAN_TITLE);
        BAN_BROADCAST_PREFIX = getString(settings, "ban-broadcast-prefix", BAN_BROADCAST_PREFIX);
        UNBAN_BROADCAST_PREFIX = getString(settings, "unban-broadcast-prefix", UNBAN_BROADCAST_PREFIX);

        Map<String, Object> messageSettings = mergedSection(messagesData, messagesDefaults, "settings");

        Config.timezone = getString(messageSettings, "timezone", Config.timezone);
        Config.dateFormat = getString(messageSettings, "date-format", Config.dateFormat);
        Config.historyDateFormat = getString(messageSettings, "history-date-format", Config.historyDateFormat);
        Config.line = getString(messageSettings, "line", Config.line);
        Config.listSeparator = getString(messageSettings, "list-separator", Config.listSeparator);
        Config.consoleName = getString(messageSettings, "console-name", Config.consoleName);
        Config.unknown = getString(messageSettings, "unknown", Config.unknown);
        Config.none = getString(messageSettings, "none", Config.none);
        Config.permanent = getString(messageSettings, "permanent", Config.permanent);
        Config.notBanned = getString(messageSettings, "not-banned", Config.notBanned);
        Config.zeroDuration = getString(messageSettings, "zero-duration", Config.zeroDuration);
        Config.reasonSuggestionFallback =
                getString(messageSettings, "reason-suggestion-fallback", Config.reasonSuggestionFallback);
        Config.playerHistoryReasonFormat =
                getString(messageSettings, "player-history-reason-format", Config.playerHistoryReasonFormat);
        Config.banMessageBodyMarker =
                getString(messageSettings, "ban-message-body-marker", Config.banMessageBodyMarker);
        Config.discordLink = getString(messageSettings, "discord-link", Config.discordLink);

        Map<String, Object> commands = mergedSection(messagesData, messagesDefaults, "commands");
        Config.punishCommand = getString(commands, "punish", Config.punishCommand);
        Config.unpunishCommand = getString(commands, "unpunish", Config.unpunishCommand);
        Config.lookupCommand = getString(commands, "lookup", Config.lookupCommand);
        Config.lookupRemoveCommand = getString(commands, "lookupremove", Config.lookupRemoveCommand);
        Config.lookupFullRemoveCommand = getString(commands, "lookupfullremove", Config.lookupFullRemoveCommand);
        Config.punishmentCommand = getString(commands, "punishment", Config.punishmentCommand);
        Config.adminPunishCommand = getString(commands, "adminpunish", Config.adminPunishCommand);
        Config.adminPunishReloadSubCommand =
                getString(commands, "adminpunish-reload", Config.adminPunishReloadSubCommand);
        Config.adminPunishHelpSubCommand =
                getString(commands, "adminpunish-help", Config.adminPunishHelpSubCommand);
        Config.punishmentListAllBansSubCommand =
                getString(commands, "punishment-listallbans", Config.punishmentListAllBansSubCommand);
        Config.punishmentReloadSubCommand =
                getString(commands, "punishment-reload", Config.punishmentReloadSubCommand);
        Config.punishmentAddSubCommand =
                getString(commands, "punishment-add", Config.punishmentAddSubCommand);
        Config.punishmentRemoveSubCommand =
                getString(commands, "punishment-remove", Config.punishmentRemoveSubCommand);
        Config.punishmentWhitelistArgument =
                getString(commands, "punishment-whitelist", Config.punishmentWhitelistArgument);

        Map<String, Object> duration = mergedSection(messagesData, messagesDefaults, "duration");
        Config.daySingular = getString(duration, "day-singular", Config.daySingular);
        Config.dayPlural = getString(duration, "day-plural", Config.dayPlural);
        Config.hourSingular = getString(duration, "hour-singular", Config.hourSingular);
        Config.hourPlural = getString(duration, "hour-plural", Config.hourPlural);
        Config.minuteSingular = getString(duration, "minute-singular", Config.minuteSingular);
        Config.minutePlural = getString(duration, "minute-plural", Config.minutePlural);

        Map<String, Object> defaultReason = mergedSection(messagesData, messagesDefaults, "default-ban-reason");
        Config.defaultReasonDisplay = getString(defaultReason, "display", Config.defaultReasonDisplay);
        Config.defaultReasonDuration = getString(defaultReason, "duration", Config.defaultReasonDuration);

        loadStringSection(MESSAGES, mergedSection(messagesData, messagesDefaults, "messages"));
        loadFormatSection(mergedSection(messagesData, messagesDefaults, "formats"));
        loadBanReasons(configuredSection(messagesData, messagesDefaults, "ban-reasons"));
        loadWhitelist(configuredObject(messagesData, messagesDefaults, "whitelist"));

        Map<String, Object> mysqlMap = mergedSection(configData, configDefaults, "mysql");

        Config.host = getString(mysqlMap, "host", Config.host);
        Config.port = parseInt(getString(mysqlMap, "port", String.valueOf(Config.port)), Config.port);
        Config.database = getString(mysqlMap, "database", Config.database);
        Config.user = getString(mysqlMap, "user", Config.user);
        Config.password = getString(mysqlMap, "password", Config.password);
        Config.jdbcParameters = getString(mysqlMap, "jdbc-parameters", Config.jdbcParameters);
    }

    private void loadStringSection(Map<String, String> target, Map<String, Object> source) {
        target.clear();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            target.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private void loadFormatSection(Map<String, Object> source) {
        FORMATS.clear();
        FORMAT_LISTS.clear();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Iterable<?> values) {
                FORMAT_LISTS.put(entry.getKey(), stringList(values));
            } else {
                FORMATS.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }

    private void loadBanReasons(Map<String, Object> source) {
        BAN_REASONS.clear();
        BAN_REASONS_BY_DISPLAY.clear();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Map<String, Object> reasonData = asMap(entry.getValue());
            if (reasonData.isEmpty()) {
                continue;
            }

            String key = entry.getKey().toLowerCase(Locale.ROOT);
            String display = getString(reasonData, "display", entry.getKey());
            String duration = getString(reasonData, "duration", Config.defaultReasonDuration);

            BanReasonConfig config = new BanReasonConfig(
                    key,
                    display,
                    duration,
                    parseDurationMillis(duration)
            );

            BAN_REASONS.put(key, config);
            BAN_REASONS_BY_DISPLAY.put(normalizeReasonLookup(display), config);
        }
    }

    private void loadWhitelist(Object whitelist) {
        whitelistedPlayers.clear();

        if (whitelist instanceof Iterable<?> entries) {

            for (Object entry : entries) {

                if (entry != null) {

                    String value =
                            String.valueOf(entry).trim();

                    if (!value.isEmpty()) {

                        whitelistedPlayers.add(
                                value.toLowerCase(Locale.ROOT)
                        );
                    }
                }
            }
        }
    }

    private boolean updateWhitelistEntry(String value, boolean add) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String cleanValue = value.trim();

        try {
            Map<String, Object> defaults = loadDefaultYamlData("messages.yml", fallbackMessagesText());
            ensureYamlFile(messagesFile, "messages.yml", fallbackMessagesText());

            Map<String, Object> data;

            data = readYamlFile(messagesFile, defaults);

            List<String> whitelist = new ArrayList<>();
            Object currentWhitelist = data.get("whitelist");

            if (currentWhitelist instanceof Iterable<?> entries) {
                for (Object entry : entries) {
                    if (entry != null && !String.valueOf(entry).isBlank()) {
                        whitelist.add(String.valueOf(entry).trim());
                    }
                }
            }

            boolean contains = whitelist.stream()
                    .anyMatch(entry -> entry.equalsIgnoreCase(cleanValue));

            if (add) {
                if (contains) {
                    return false;
                }

                whitelist.add(cleanValue);
            } else {
                if (!contains) {
                    return false;
                }

                whitelist.removeIf(entry -> entry.equalsIgnoreCase(cleanValue));
            }

            data.put("whitelist", whitelist);
            writeYaml(messagesFile, mergeMessagesForFile(data, defaults));
            loadConfig();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static long parseDurationMillis(String duration) {
        if (duration == null) {
            return -1;
        }

        String value = duration.trim().toLowerCase(Locale.ROOT).replace(" ", "");

        if (value.equals("-1") ||
                value.equals("permanent") ||
                value.equals("perm") ||
                value.equals("dauerhaft")) {

            return -1;
        }

        long multiplier = 1;
        String number = value;

        if (value.endsWith("ms")) {
            number = value.substring(0, value.length() - 2);
        } else if (value.endsWith("s")) {
            multiplier = 1000L;
            number = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 60_000L;
            number = value.substring(0, value.length() - 1);
        } else if (value.endsWith("h")) {
            multiplier = 60L * 60L * 1000L;
            number = value.substring(0, value.length() - 1);
        } else if (value.endsWith("d")) {
            multiplier = 24L * 60L * 60L * 1000L;
            number = value.substring(0, value.length() - 1);
        }

        try {
            return Long.parseLong(number) * multiplier;
        } catch (NumberFormatException ignored) {
            return 24L * 60L * 60L * 1000L;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> mergeConfigForFile(
            Map<String, Object> data,
            Map<String, Object> defaults
    ) {

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put(
                "settings",
                deepMerge(
                        filterSettings(asMap(defaults.get("settings")), CONFIG_SETTING_KEYS),
                        filterSettings(asMap(data.get("settings")), CONFIG_SETTING_KEYS)
                )
        );
        merged.put("mysql", deepMerge(asMap(defaults.get("mysql")), asMap(data.get("mysql"))));

        return merged;
    }

    private Map<String, Object> mergeMessagesForFile(
            Map<String, Object> data,
            Map<String, Object> defaults
    ) {

        Map<String, Object> merged = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();

            if (data.containsKey(key)) {
                Object currentValue = data.get(key);

                if (entry.getValue() instanceof Map<?, ?> &&
                        currentValue instanceof Map<?, ?>) {

                    merged.put(key, deepMerge(asMap(entry.getValue()), asMap(currentValue)));
                } else {
                    merged.put(key, currentValue);
                }
            } else {
                merged.put(key, entry.getValue());
            }
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }

        removeDeprecatedMessageKeys(merged);
        return merged;
    }

    private Map<String, Object> extractConfigFileData(Map<String, Object> data) {
        Map<String, Object> configData = new LinkedHashMap<>();
        Map<String, Object> settings = filterSettings(asMap(data.get("settings")), CONFIG_SETTING_KEYS);

        if (!settings.isEmpty()) {
            configData.put("settings", settings);
        }

        if (data.containsKey("mysql")) {
            configData.put("mysql", data.get("mysql"));
        }

        return configData;
    }

    private Map<String, Object> migrateLegacyMessages(
            Map<String, Object> configData,
            Map<String, Object> messagesData,
            boolean overwriteExisting
    ) {

        Map<String, Object> migrated = new LinkedHashMap<>(messagesData);
        Map<String, Object> legacySettings = asMap(configData.get("settings"));

        if (!legacySettings.isEmpty()) {
            Map<String, Object> settings = new LinkedHashMap<>(asMap(migrated.get("settings")));

            for (String key : MESSAGE_SETTING_KEYS) {
                if (legacySettings.containsKey(key) && (overwriteExisting || !settings.containsKey(key))) {
                    settings.put(key, legacySettings.get(key));
                }
            }

            for (Map.Entry<String, Object> entry : legacySettings.entrySet()) {
                String key = entry.getKey();

                if (CONFIG_SETTING_KEYS.contains(key) ||
                        MESSAGE_SETTING_KEYS.contains(key) ||
                        key.equals("legacy" + "-ban-title")) {

                    continue;
                }

                if (overwriteExisting || !settings.containsKey(key)) {
                    settings.put(key, entry.getValue());
                }
            }

            if (!settings.isEmpty()) {
                migrated.put("settings", settings);
            }
        }

        Set<String> migratedKeys = new HashSet<>();
        for (String key : MESSAGE_TOP_LEVEL_KEYS) {
            migratedKeys.add(key);

            if (!configData.containsKey(key)) {
                continue;
            }

            if (overwriteExisting || !migrated.containsKey(key)) {
                migrated.put(key, mergeMigratedValue(migrated.get(key), configData.get(key), true));
                continue;
            }

            Object currentValue = migrated.get(key);
            Object legacyValue = configData.get(key);
            if (currentValue instanceof Map<?, ?> && legacyValue instanceof Map<?, ?>) {
                migrated.put(key, mergeMigratedValue(currentValue, legacyValue, false));
            }
        }

        for (Map.Entry<String, Object> entry : configData.entrySet()) {
            String key = entry.getKey();

            if (key.equals("settings") || key.equals("mysql") || migratedKeys.contains(key)) {
                continue;
            }

            if (overwriteExisting || !migrated.containsKey(key)) {
                migrated.put(key, mergeMigratedValue(migrated.get(key), entry.getValue(), true));
                continue;
            }

            Object currentValue = migrated.get(key);
            Object legacyValue = entry.getValue();
            if (currentValue instanceof Map<?, ?> && legacyValue instanceof Map<?, ?>) {
                migrated.put(key, mergeMigratedValue(currentValue, legacyValue, false));
            }
        }

        return migrated;
    }

    private Object mergeMigratedValue(Object currentValue, Object legacyValue, boolean legacyWins) {
        if (currentValue instanceof Map<?, ?> && legacyValue instanceof Map<?, ?>) {
            if (legacyWins) {
                return deepMerge(asMap(currentValue), asMap(legacyValue));
            }

            return deepMerge(asMap(legacyValue), asMap(currentValue));
        }

        if (legacyWins || currentValue == null) {
            return legacyValue;
        }

        return currentValue;
    }

    private Map<String, Object> filterSettings(Map<String, Object> settings, Iterable<String> allowedKeys) {
        Map<String, Object> filtered = new LinkedHashMap<>();

        for (String key : allowedKeys) {
            if (settings.containsKey(key)) {
                filtered.put(key, settings.get(key));
            }
        }

        return filtered;
    }

    private void removeDeprecatedMessageKeys(Map<String, Object> config) {
        Map<String, Object> messages = asMap(config.get("messages"));
        Map<String, Object> settings = asMap(config.get("settings"));
        Map<String, Object> formats = asMap(config.get("formats"));
        Map<String, Object> banReasons = asMap(config.get("ban-reasons"));

        if (!settings.isEmpty()) {
            String discordLink = String.valueOf(settings.getOrDefault("discord-link", ""));
            if (discordLink.startsWith("https://discord.gg/")) {
                settings.put("discord-link", discordLink.substring("https://discord.gg/".length()));
            } else if (discordLink.startsWith("discord.gg/")) {
                settings.put("discord-link", discordLink.substring("discord.gg/".length()));
            }
        }

        if (!formats.isEmpty()) {
            if ("{ban_broadcast_prefix}&c{player} &8v. &c{executor}"
                    .equals(String.valueOf(formats.get("ban-broadcast")))) {

                formats.put(
                        "ban-broadcast",
                        "&c&lBAN&r &8» &a{gebannter_spieler} &8v. &a{banausführer} &8(&c{bangrund}&8)"
                );
            }

            Object banMessage = formats.get("ban-message");
            if (banMessage instanceof List<?> lines &&
                    lines.stream().map(String::valueOf).anyMatch(line -> line.contains("Du wurdest von"))) {

                formats.put(
                        "ban-message",
                        List.of(
                                "{prefix}",
                                "",
                                "&7Grund&8:&r {grund} &8» {zeit}",
                                "",
                                "&7Verbleibend&8:&r {verbleibend}",
                                "&7Gebannt bis&8:&r {bis}",
                                "",
                                "&9Discord&8:&r &adiscord.gg/{discord_link}"
                        )
                );
            }
        }

        if (!banReasons.isEmpty() &&
                banReasons.containsKey("teaminghintergehung") &&
                !banReasons.containsKey("teamhintergehung")) {

            banReasons.put("teamhintergehung", banReasons.remove("teaminghintergehung"));
        }

        Map<String, Object> teamReason = asMap(banReasons.get("teamhintergehung"));
        if (!teamReason.isEmpty() &&
                "&cTeaminghintergehung".equals(String.valueOf(teamReason.get("display")))) {

            teamReason.put("display", "&cTeamhintergehung");
        }

        if (messages.isEmpty()) {
            return;
        }

        messages.remove("log-start-line");
        messages.remove("log-starting");
        messages.remove("log-config-loaded");
        messages.remove("log-bannedplayers-loaded");
        messages.remove("log-mysql-success");
        messages.remove("log-mysql-failed-closed");
        messages.remove("log-mysql-failed");
        messages.remove("log-started");
        messages.remove("mysql-connection-error");

        if ("{prefix}&a/{punishment_command} &8<&alistallbans/reload/add/remove&8> <&awhitelist&8> <&aSpieler&8>"
                .equals(String.valueOf(messages.get("punishment-usage")))) {

            messages.put(
                    "punishment-usage",
                    "{prefix}&a/{punishment_command} &8<&alistallbans/reload/whitelist&8> <&aadd/remove&8> <&aSpieler&8>"
            );
        }
    }

    private Map<String, Object> deepMerge(
            Map<String, Object> defaults,
            Map<String, Object> data
    ) {

        Map<String, Object> merged = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            String key = entry.getKey();

            if (data.containsKey(key)) {
                Object currentValue = data.get(key);

                if (entry.getValue() instanceof Map<?, ?> &&
                        currentValue instanceof Map<?, ?>) {

                    merged.put(key, deepMerge(asMap(entry.getValue()), asMap(currentValue)));
                } else {
                    merged.put(key, currentValue);
                }
            } else {
                merged.put(key, entry.getValue());
            }
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return merged;
    }

    private void writeYaml(File file, Map<String, Object> config) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Yaml writer = new Yaml(options);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        Files.writeString(file.toPath(), writer.dump(config), StandardCharsets.UTF_8);
    }

    private String getString(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);

        if (value == null) {
            return fallback;
        }

        return normalizeText(String.valueOf(value));
    }

    private Map<String, Object> mergedSection(
            Map<String, Object> data,
            Map<String, Object> defaults,
            String section
    ) {

        Map<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(asMap(defaults.get(section)));
        merged.putAll(asMap(data.get(section)));
        return merged;
    }

    private Map<String, Object> configuredSection(
            Map<String, Object> data,
            Map<String, Object> defaults,
            String section
    ) {

        if (data.containsKey(section)) {
            return asMap(data.get(section));
        }

        return asMap(defaults.get(section));
    }

    private Object configuredObject(
            Map<String, Object> data,
            Map<String, Object> defaults,
            String section
    ) {

        if (data.containsKey(section)) {
            return data.get(section);
        }

        return defaults.get(section);
    }

    private List<String> stringList(Iterable<?> values) {
        List<String> list = new ArrayList<>();

        for (Object value : values) {
            list.add(String.valueOf(value));
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }

        return Collections.emptyMap();
    }

    private void ensureYamlFile(File file, String resourceName, String fallbackText) throws IOException {
        if (file.exists()) {
            return;
        }

        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        Files.writeString(
                file.toPath(),
                getDefaultResourceText(resourceName, fallbackText),
                StandardCharsets.UTF_8
        );
    }

    private Map<String, Object> readYamlFile(File file, Map<String, Object> fallback) throws IOException {
        if (!file.exists()) {
            return new LinkedHashMap<>(fallback);
        }

        try (Reader reader =
                     Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {

            Object loaded = yaml.load(reader);
            if (loaded instanceof Map<?, ?>) {
                return new LinkedHashMap<>(asMap(loaded));
            }
        }

        return new LinkedHashMap<>(fallback);
    }

    private Map<String, Object> loadDefaultYamlData(String resourceName, String fallbackText) throws IOException {
        Map<String, Object> defaults = yaml.load(getDefaultResourceText(resourceName, fallbackText));
        return defaults == null ? Collections.emptyMap() : defaults;
    }

    private String getDefaultResourceText(String resourceName, String fallbackText) throws IOException {
        try (InputStream inputStream =
                     getClass().getClassLoader().getResourceAsStream(resourceName)) {

            if (inputStream != null) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return fallbackText;
    }

    private String fallbackConfigText() {
        return "settings:\n" +
                "  prefix: \"&8[&c&lBANSYSTEM&r&8] &8» \"\n" +
                "  ban-title: \"&8[&c&lBANSYSTEM&r&8]\"\n" +
                "  ban-broadcast-prefix: \"&c&lBAN &8» \"\n" +
                "  unban-broadcast-prefix: \"&a&lUNBAN &8» \"\n" +
                "mysql:\n" +
                "  host: localhost\n" +
                "  port: 3306\n" +
                "  database: proxybans\n" +
                "  user: root\n" +
                "  password: password\n" +
                "  jdbc-parameters: \"useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&useUnicode=true&characterEncoding=UTF-8\"\n";
    }

    private String fallbackMessagesText() {
        return "settings:\n" +
                "  timezone: \"Europe/Berlin\"\n" +
                "  date-format: \"dd.MM.yyyy HH:mm:ss\"\n" +
                "  history-date-format: \"dd-MM-yyyy HH:mm:ss\"\n" +
                "  line: \"&8&m----------------------------------------\"\n" +
                "  list-separator: \", \"\n" +
                "  console-name: \"Console\"\n" +
                "  unknown: \"Unbekannt\"\n" +
                "  none: \"Keine Einträge\"\n" +
                "  permanent: \"Permanent\"\n" +
                "  not-banned: \"Nicht gebannt\"\n" +
                "  zero-duration: \"0 Minuten\"\n" +
                "  reason-suggestion-fallback: \"Grund\"\n" +
                "  player-history-reason-format: \"{executor} &8(&c{reason}&8)\"\n" +
                "  ban-message-body-marker: \"&7Du wurdest\"\n" +
                "  discord-link: \"deinserver\"\n" +
                "commands: {}\n" +
                "messages: {}\n" +
                "formats: {}\n" +
                "duration:\n" +
                "  day-singular: \"Tag\"\n" +
                "  day-plural: \"Tage\"\n" +
                "  hour-singular: \"Stunde\"\n" +
                "  hour-plural: \"Stunden\"\n" +
                "  minute-singular: \"Minute\"\n" +
                "  minute-plural: \"Minuten\"\n" +
                "default-ban-reason:\n" +
                "  display: \"{input}\"\n" +
                "  duration: \"1d\"\n" +
                "ban-reasons: {}\n" +
                "whitelist: []\n";
    }

    public void loadBannedPlayers() {

        try {

            bannedFile = new File(instance.adapter.getDataDirectory(), "bannedplayers.json");

            if (!bannedFile.exists()) {

                bannedFile.getParentFile().mkdirs();
                Files.writeString(bannedFile.toPath(), "[]", StandardCharsets.UTF_8);

                saveBannedPlayers();
                return;
            }

            String json =
                    Files.readString(bannedFile.toPath(), StandardCharsets.UTF_8);

            if (!json.isBlank()) {

                BanData[] data =
                        gson.fromJson(json, BanData[].class);

                if (data != null) {

                    for (BanData bd : data) {

                        if (bd == null || bd.uuid == null || bd.uuid.isBlank()) {
                            continue;
                        }

                        if (bd.gebanntVon == null) {
                            bd.gebanntVon = new ArrayList<>();
                        }

                        bannedPlayers.put(
                                bd.uuid,
                                bd
                        );
                    }
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void saveBannedPlayers() {

        try {

            if (bannedFile == null) return;

            refreshStoredBanDetails();

            Gson gson =
                    new GsonBuilder()
                            .setPrettyPrinting()
                            .create();

            String json =
                    gson.toJson(bannedPlayers.values());

            Files.writeString(bannedFile.toPath(), json, StandardCharsets.UTF_8);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private static void refreshStoredBanDetails() {
        for (BanData data : bannedPlayers.values()) {
            if (data == null || data.uuid == null || data.uuid.isBlank()) {
                continue;
            }

            BanEntry entry = null;
            if (mysql != null && mysql.isConnected()) {
                entry = mysql.getBanEntry(data.uuid);
            }

            data.updateActiveBan(entry);
        }
    }

    public static String handleLogin(UUID uuid, String playerName) {
        if (mysql == null || !mysql.isConnected()) {
            return message("login-ban-system-unavailable");
        }

        try {
            String uuidStr = uuid.toString();
            players.put(uuid, playerName);

            if (playerName != null && !playerName.isEmpty()) {
                mysql.registerPlayer(uuidStr, playerName);
            }

            if (!mysql.isBanned(uuidStr)) {
                return null;
            }

            if (isWhitelisted(uuid, playerName)) {
                mysql.unban(uuidStr);
                return null;
            }

            BanEntry banEntry = mysql.getBanEntry(uuidStr);
            long banUntil = banEntry == null ? -1 : banEntry.getBanUntil();

            if (banUntil != -1 && System.currentTimeMillis() > banUntil) {
                mysql.unban(uuidStr);
                return null;
            }

            String msg;
            if (banEntry != null && banEntry.getReason() != null && !banEntry.getReason().isBlank()) {
                msg = buildBanMessage(
                        normalizeText(banEntry.getReason()),
                        banUntil
                );
            } else {
                msg = mysql.getBanMessage(uuidStr);
            }

            if (msg == null || msg.isEmpty()) {
                msg = message("fallback-ban-message");
            }

            return formatBanMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void handleDisconnect(UUID uuid) {
        if (uuid != null) {
            players.remove(uuid);
        }
    }

    public static void shutdown() {
        try {
            if (mysql != null && mysql.isConnected()) {
                mysql.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


