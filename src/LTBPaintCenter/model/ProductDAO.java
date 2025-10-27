package LTBPaintCenter.model;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static Product fromResultSet(ResultSet rs) throws SQLException {
        // Map to current schema: inventory table uses qty, date_imported, expiration_date
        LocalDate importedDate = readLocalDate(rs, "date_imported");
        LocalDate expDate = readLocalDate(rs, "expiration_date");
        Product p = new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("price"),
                rs.getInt("qty"),
                rs.getString("brand"),
                rs.getString("color"),
                rs.getString("type"),
                importedDate,
                expDate,
                rs.getString("status")
        );
        return p;
    }

    private static LocalDate readLocalDate(ResultSet rs, String column) throws SQLException {
        Object val = rs.getObject(column);
        if (val == null) return null;
        if (val instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (val instanceof Number n) {
            long epochMillis = n.longValue();
            return java.time.Instant.ofEpochMilli(epochMillis)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        }
        String s = val.toString().trim();
        if (s.isEmpty()) return null;
        if (s.matches("\\d+")) {
            try {
                long epochMillis = Long.parseLong(s);
                return java.time.Instant.ofEpochMilli(epochMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception ignored) {}
        }
        try {
            if (s.length() >= 10) {
                String d = s.substring(0, 10);
                return LocalDate.parse(d);
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            try {
                return java.sql.Date.valueOf(s).toLocalDate();
            } catch (Exception ex) {
                System.err.println("[ProductDAO] Failed to parse date in column '" + column + "': " + s);
                return null;
            }
        }
    }

    public static List<Product> getAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY name ASC";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(fromResultSet(rs));
        } catch (Exception e) {
            System.err.println("Failed to load products: " + e.getMessage());
        }
        return list;
    }

    public static List<Product> getAvailableForPOS() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            LocalDate today = LocalDate.now();
            while (rs.next()) {
                Product p = fromResultSet(rs);
                // Exclude expired (expiration <= today) and out-of-stock
                if (p.getQuantity() > 0 && (p.getExpirationDate() == null || p.getExpirationDate().isAfter(today))) {
                    list.add(p);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load available products: " + e.getMessage());
        }
        return list;
    }

    // ✅ Add a new product batch
    public static void add(Product p) {
        String sql = "INSERT INTO inventory (name, brand, color, type, price, qty, date_imported, expiration_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getBrand());
            pstmt.setString(3, p.getColor());
            pstmt.setString(4, p.getType());
            pstmt.setDouble(5, p.getPrice());
            pstmt.setInt(6, p.getQuantity());
            pstmt.setDate(7, p.getDateImported() == null ? null : Date.valueOf(p.getDateImported()));
            pstmt.setDate(8, p.getExpirationDate() == null ? null : Date.valueOf(p.getExpirationDate()));
            pstmt.setString(9, p.getStatus());

            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to add product: " + e.getMessage());
        }
    }

    public static void update(Product p) {
        String sql = "UPDATE inventory SET name=?, brand=?, color=?, type=?, price=?, qty=?, date_imported=?, expiration_date=?, status=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getBrand());
            pstmt.setString(3, p.getColor());
            pstmt.setString(4, p.getType());
            pstmt.setDouble(5, p.getPrice());
            pstmt.setInt(6, p.getQuantity());
            pstmt.setDate(7, p.getDateImported() == null ? null : Date.valueOf(p.getDateImported()));
            pstmt.setDate(8, p.getExpirationDate() == null ? null : Date.valueOf(p.getExpirationDate()));
            pstmt.setString(9, p.getStatus());
            pstmt.setInt(10, p.getId());

            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to update product: " + e.getMessage());
        }
    }

    public static void updateExpiredStatuses() {
        String sql = "UPDATE inventory SET status='Expired' WHERE expiration_date IS NOT NULL AND expiration_date < DATE('now')";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.err.println("Failed to mark expired products: " + e.getMessage());
        }
    }

    public static List<Product> getAlerts() {
        List<Product> alerts = new ArrayList<>();
        String sql = "SELECT * FROM inventory WHERE " +
                "(expiration_date BETWEEN DATE('now') AND DATE('now', '+7 day')) " +
                "OR qty <= 5";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product p = fromResultSet(rs);
                alerts.add(p);
            }
        } catch (Exception e) {
            System.err.println("Failed to get alerts: " + e.getMessage());
        }

        return alerts;
    }
}
