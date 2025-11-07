package LTBPaintCenter.dao;

import LTBPaintCenter.model.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides lookup functionality for database tables.
 * It can retrieve all values from a specific column in any table.
 * Used for populating dropdown lists and combo boxes.
 */
public class LookupDAO {

    /**
     * Gets all values from the "name" column of a specified table.
     * Used to get lists of brands, colors, types, etc. for dropdown menus.
     * 
     * @param table The name of the table to query
     * @return A list of all name values, ordered alphabetically
     */
    public static List<String> getAll(String table) {
        List<String> values = new ArrayList<>();
        String sql = "SELECT name FROM " + table + " ORDER BY name ASC";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                values.add(rs.getString("name"));
            }
        } catch (Exception e) {
            System.err.println("Failed to load from " + table + ": " + e.getMessage());
        }
        
        return values;
    }
}
