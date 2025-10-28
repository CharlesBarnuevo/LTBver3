package LTBPaintCenter.dao;

import LTBPaintCenter.model.Database;

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
}