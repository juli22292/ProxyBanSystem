package proxyBanSystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerRegistry {

    public static void registerPlayer(UUID uuid, String name) {

        try {
            if (ProxyBanSystem.mysql == null) {
                return;
            }

            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                return;
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT IGNORE INTO player_registry (uuid, name) VALUES (?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllPlayers() {

        List<String> list = new ArrayList<>();

        try {
            if (ProxyBanSystem.mysql == null) {
                return list;
            }

            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                return list;
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT name FROM player_registry ORDER BY name ASC"
            );
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("name"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static String getName(UUID uuid) {

        try {
            if (ProxyBanSystem.mysql == null) {
                return null;
            }

            Connection connection = ProxyBanSystem.mysql.getConnection();
            if (connection == null) {
                return null;
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT name FROM player_registry WHERE uuid=?"
            )) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("name");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
