package proxyBanSystem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQL {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;
    private static final long RECONNECT_RETRY_INTERVAL_MS = 10_000L;
    private static final long CONNECTION_ERROR_LOG_INTERVAL_MS = 30_000L;

    private volatile Connection connection;
    private long lastConnectionAttempt;
    private long lastConnectionErrorLog;

    private final String host;
    private final int port;
    private final String database;
    private final String user;
    private final String password;
    private final String jdbcParameters;

    public MySQL(String host, int port, String database, String user, String password, String jdbcParameters) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.jdbcParameters = jdbcParameters;
    }

    public synchronized void connect() {
        closeCurrentConnection();
        openConnection(true);
    }

    private boolean ensureConnected() {
        synchronized (this) {
            if (isConnectionUsable()) {
                return true;
            }

            Connection previousConnection = connection;
            closeCurrentConnection();
            return openConnection(previousConnection != null);
        }
    }

    private boolean isConnectionUsable() {
        try {
            return connection != null &&
                    !connection.isClosed() &&
                    connection.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean openConnection(boolean force) {
        long now = System.currentTimeMillis();

        if (!force && now - lastConnectionAttempt < RECONNECT_RETRY_INTERVAL_MS) {
            return false;
        }

        lastConnectionAttempt = now;
        Connection newConnection = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            newConnection = DriverManager.getConnection(
                    buildJdbcUrl(),
                    user,
                    password
            );

            createTable(newConnection);
            connection = newConnection;
            return true;

        } catch (Exception e) {
            closeQuietly(newConnection);
            connection = null;
            logConnectionError(e);
            return false;
        }
    }

    private String buildJdbcUrl() {
        String parameters = jdbcParameters == null ? "" : jdbcParameters.trim();

        return "jdbc:mysql://" + host + ":" + port + "/" + database +
                (parameters.isEmpty() ? "" : "?" + parameters);
    }

    private void createTable(Connection activeConnection) throws SQLException {

        if (activeConnection == null) {
            return;
        }

        try (Statement st = activeConnection.createStatement()) {

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS bans (" +
                            "uuid VARCHAR(36) PRIMARY KEY," +
                            "name TEXT," +
                            "reason TEXT," +
                            "message TEXT," +
                            "ban_until BIGINT" +
                            ")"
            );

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS punishment_history (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "uuid VARCHAR(36)," +
                            "name TEXT," +
                            "type TEXT," +
                            "reason TEXT," +
                            "executor TEXT," +
                            "date TEXT" +
                            ")"
            );

            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS player_registry (" +
                            "uuid VARCHAR(36) PRIMARY KEY," +
                            "name TEXT" +
                            ")"
            );

        }
    }

    private synchronized void closeCurrentConnection() {
        closeQuietly(connection);
        connection = null;
    }

    private void closeQuietly(Connection connectionToClose) {
        try {
            if (connectionToClose != null && !connectionToClose.isClosed()) {
                connectionToClose.close();
            }
        } catch (SQLException ignored) {
        }
    }

    private void logConnectionError(Exception e) {
        long now = System.currentTimeMillis();

        if (now - lastConnectionErrorLog < CONNECTION_ERROR_LOG_INTERVAL_MS) {
            return;
        }

        lastConnectionErrorLog = now;
        System.err.println("[ProxyBanSystem] MySQL-Verbindung konnte nicht hergestellt werden: " + e.getMessage());
    }

    // =========================
    // CONNECTION
    // =========================

    public boolean isConnected() {
        return ensureConnected();
    }

    public Connection getConnection() {
        return ensureConnected() ? connection : null;
    }

    public void disconnect() {
        closeCurrentConnection();
    }

    // =========================
    // PLAYER REGISTRY
    // =========================

    public void registerPlayer(String uuid, String name) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "INSERT INTO player_registry (uuid, name) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE name=VALUES(name)"
        )) {

            ps.setString(1, uuid);
            ps.setString(2, name);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUUIDByName(String name) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return null;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT uuid FROM player_registry WHERE LOWER(name)=LOWER(?)"
        )) {

            ps.setString(1, name);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getNameByUUID(String uuid) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return null;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT name FROM player_registry WHERE uuid=?"
        )) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String> getRegisteredPlayers() {

        List<String> list = new ArrayList<>();
        Connection activeConnection = getConnection();
        if (activeConnection == null) return list;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT name FROM player_registry ORDER BY name ASC"
        )) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("name"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // BAN SYSTEM
    // =========================

    public boolean isBanned(String uuid) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return false;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT uuid FROM bans WHERE uuid=?"
        )) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getBanMessage(String uuid) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return ProxyBanSystem.message("fallback-ban-message");

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT message FROM bans WHERE uuid=?"
        )) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("message");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ProxyBanSystem.message("fallback-ban-message");
    }

    public BanEntry getBanEntry(String uuid) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return null;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT reason, message, ban_until FROM bans WHERE uuid=?"
        )) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BanEntry(
                            rs.getString("reason"),
                            rs.getString("message"),
                            rs.getLong("ban_until")
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public long getBanUntil(String uuid) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return -1;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "SELECT ban_until FROM bans WHERE uuid=?"
        )) {

            ps.setString(1, uuid);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("ban_until");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public void unban(String uuid) {
        Connection activeConnection = getConnection();
        if (activeConnection == null) return;

        try (PreparedStatement ps = activeConnection.prepareStatement(
                "DELETE FROM bans WHERE uuid=?"
        )) {

            ps.setString(1, uuid);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
