package LTBPaintCenter.model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LookupDAO {

    public static List<String> getAll(String table) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT name FROM " + table + " ORDER BY name ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (Exception e) {
            System.err.println("Failed to load from " + table + ": " + e.getMessage());
        }
        return list;
    }

    public static void addIfNotExists(String table, String value) {
        if (value == null || value.isBlank()) return;
        String sql = "INSERT OR IGNORE INTO " + table + " (name) VALUES (?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, value.trim());
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to add to " + table + ": " + e.getMessage());
        }
    }
}