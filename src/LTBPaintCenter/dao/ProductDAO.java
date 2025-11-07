package LTBPaintCenter.dao;

import LTBPaintCenter.model.Database;
import LTBPaintCenter.model.Product;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles database operations for Product objects.
 * It provides methods to get, add, update, and query products from the database.
 */
public class ProductDAO {

    /**
     * Converts a database ResultSet row into a Product object.
     * 
     * @param rs The ResultSet containing the database row
     * @return A Product object
     * @throws SQLException If there's an error reading from the database
     */
    private static Product fromResultSet(ResultSet rs) throws SQLException {
        LocalDate importedDate = readLocalDate(rs, "date_imported");
        LocalDate expirationDate = readLocalDate(rs, "expiration_date");
        
        Product product = new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price"),
                rs.getInt("qty"),
                rs.getString("brand"),
                rs.getString("color"),
                rs.getString("type"),
                importedDate,
                expirationDate,
                rs.getString("status")
        );
        return product;
    }

    /**
     * Reads a LocalDate from a database column.
     * Handles different date formats that might be stored.
     * 
     * @param rs The ResultSet to read from
     * @param column The name of the date column
     * @return A LocalDate object, or null if the date is null or can't be parsed
     * @throws SQLException If there's an error reading from the database
     */
    private static LocalDate readLocalDate(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        
        // Try different date formats
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        
        if (value instanceof Number number) {
            // Sometimes dates are stored as numbers (milliseconds)
            long epochMillis = number.longValue();
            return java.time.Instant.ofEpochMilli(epochMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        
        // Try parsing as a string
        String dateString = value.toString().trim();
        if (dateString.isEmpty()) {
            return null;
        }
        
        // Check if it's a numeric string (milliseconds)
        if (dateString.matches("\\d+")) {
            try {
                long epochMillis = Long.parseLong(dateString);
                return java.time.Instant.ofEpochMilli(epochMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception ignored) {
                // Not a valid timestamp, continue to string parsing
            }
        }
        
        // Try parsing as ISO date string (yyyy-MM-dd)
        try {
            if (dateString.length() >= 10) {
                String datePart = dateString.substring(0, 10);
                return LocalDate.parse(datePart);
            }
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            // Last resort: try java.sql.Date.valueOf
            try {
                return java.sql.Date.valueOf(dateString).toLocalDate();
            } catch (Exception ex) {
                System.err.println("[ProductDAO] Failed to parse date in column '" + column + "': " + dateString);
                return null;
            }
        }
    }

    /**
     * Gets all products from the database, ordered by name.
     * 
     * @return A list of all products
     */
    public static List<Product> getAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY name ASC";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(fromResultSet(rs));
            }
        } catch (Exception e) {
            System.err.println("Failed to load products: " + e.getMessage());
        }
        
        return products;
    }

    /**
     * Gets all products that are available for Point of Sale.
     * Filters out expired products and products with zero quantity.
     * 
     * @return A list of available products
     */
    public static List<Product> getAvailableForPOS() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM inventory";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            LocalDate today = LocalDate.now();
            while (rs.next()) {
                Product product = fromResultSet(rs);
                // Only include products that are in stock and not expired
                if (product.getQuantity() > 0 && 
                    (product.getExpirationDate() == null || 
                     product.getExpirationDate().isAfter(today))) {
                    products.add(product);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load available products: " + e.getMessage());
        }
        
        return products;
    }

    /**
     * Adds a new product to the database.
     * 
     * @param product The Product object to add
     */
    public static void add(Product product) {
        String sql = "INSERT INTO inventory (name, brand, color, type, price, qty, " +
                     "date_imported, expiration_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getBrand());
            pstmt.setString(3, product.getColor());
            pstmt.setString(4, product.getType());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setInt(6, product.getQuantity());
            pstmt.setDate(7, product.getDateImported() == null ? null : 
                    Date.valueOf(product.getDateImported()));
            pstmt.setDate(8, product.getExpirationDate() == null ? null : 
                    Date.valueOf(product.getExpirationDate()));
            pstmt.setString(9, product.getStatus());
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to add product: " + e.getMessage());
        }
    }

    /**
     * Updates an existing product in the database.
     * 
     * @param product The Product object with updated information
     */
    public static void update(Product product) {
        String sql = "UPDATE inventory SET name=?, brand=?, color=?, type=?, price=?, " +
                     "qty=?, date_imported=?, expiration_date=?, status=? WHERE id=?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getBrand());
            pstmt.setString(3, product.getColor());
            pstmt.setString(4, product.getType());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setInt(6, product.getQuantity());
            pstmt.setDate(7, product.getDateImported() == null ? null : 
                    Date.valueOf(product.getDateImported()));
            pstmt.setDate(8, product.getExpirationDate() == null ? null : 
                    Date.valueOf(product.getExpirationDate()));
            pstmt.setString(9, product.getStatus());
            pstmt.setInt(10, product.getId());
            
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update product: " + e.getMessage());
        }
    }

    /**
     * Updates the status of all expired products to "Expired".
     * This is a maintenance function that should be run periodically.
     */
    public static void updateExpiredStatuses() {
        String sql = "UPDATE inventory SET status='Expired' WHERE expiration_date IS NOT NULL " +
                     "AND expiration_date < DATE('now')";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.err.println("Failed to mark expired products: " + e.getMessage());
        }
    }

    /**
     * Gets all products that need alerts (expiring soon or low stock).
     * 
     * @return A list of products that need attention
     */
    public static List<Product> getAlerts() {
        List<Product> alerts = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE " +
                     "(expiration_date BETWEEN DATE('now') AND DATE('now', '+7 day')) " +
                     "OR qty <= 5";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Product product = fromResultSet(rs);
                alerts.add(product);
            }
        } catch (Exception e) {
            System.err.println("Failed to get alerts: " + e.getMessage());
        }
        
        return alerts;
    }
}
