package LTBPaintCenter.dao;

import LTBPaintCenter.model.InventoryBatch;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database operations for InventoryBatch records.
 */
public class InventoryDAO {

    private final Connection conn;

    public InventoryDAO(Connection conn) {
        this.conn = conn;
    }

    // ───────────────────────────────
    // Create
    // ───────────────────────────────
    public boolean addBatch(InventoryBatch batch) {
        String sql = "INSERT INTO inventory (product_code, name, brand, color, type, price, qty, date_imported, expiration_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getProductCode());
            ps.setString(2, batch.getName());
            ps.setString(3, batch.getBrand());
            ps.setString(4, batch.getColor());
            ps.setString(5, batch.getType());
            ps.setDouble(6, batch.getPrice());
            ps.setInt(7, batch.getQuantity());
            ps.setDate(8, batch.getDateImported() != null ? Date.valueOf(batch.getDateImported()) : null);
            ps.setDate(9, batch.getExpirationDate() != null ? Date.valueOf(batch.getExpirationDate()) : null);
            ps.setString(10, batch.getStatus());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ───────────────────────────────
    // Read all
    // ───────────────────────────────
    public List<InventoryBatch> getAllBatches() {
        List<InventoryBatch> list = new ArrayList<>();
        String sql = "SELECT * FROM inventory ORDER BY id ASC";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractBatch(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ───────────────────────────────
    // Update
    // ───────────────────────────────
    public boolean updateBatch(InventoryBatch batch) {
        String sql = "UPDATE inventory SET product_code=?, name=?, brand=?, color=?, type=?, price=?, qty=?, " +
                "date_imported=?, expiration_date=?, status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, batch.getProductCode());
            ps.setString(2, batch.getName());
            ps.setString(3, batch.getBrand());
            ps.setString(4, batch.getColor());
            ps.setString(5, batch.getType());
            ps.setDouble(6, batch.getPrice());
            ps.setInt(7, batch.getQuantity());
            ps.setDate(8, batch.getDateImported() != null ? Date.valueOf(batch.getDateImported()) : null);
            ps.setDate(9, batch.getExpirationDate() != null ? Date.valueOf(batch.getExpirationDate()) : null);
            ps.setString(10, batch.getStatus());
            ps.setInt(11, batch.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ───────────────────────────────
    // Delete
    // ───────────────────────────────
    public boolean deleteBatch(int id) {
        String sql = "DELETE FROM inventory WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ───────────────────────────────
    // Helper: Extract from ResultSet
    // ───────────────────────────────
    private InventoryBatch extractBatch(ResultSet rs) throws SQLException {
        InventoryBatch b = new InventoryBatch();
        b.setId(rs.getInt("id"));
        b.setName(rs.getString("name"));
        try { b.setProductCode(rs.getString("product_code")); } catch (SQLException ignored) {}
        b.setBrand(rs.getString("brand"));
        b.setColor(rs.getString("color"));
        b.setType(rs.getString("type"));
        b.setPrice(rs.getDouble("price"));
        b.setQuantity(rs.getInt("qty"));
        b.setDateImported(readLocalDate(rs, "date_imported"));
        b.setExpirationDate(readLocalDate(rs, "expiration_date"));
        b.setStatus(rs.getString("status"));
        return b;
    }

    private LocalDate readLocalDate(ResultSet rs, String column) throws SQLException {
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
        // Numeric string (epoch millis)
        if (s.matches("\\d+")) {
            try {
                long epochMillis = Long.parseLong(s);
                return java.time.Instant.ofEpochMilli(epochMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception ignored) {}
        }
        // Try ISO date or datetime; if contains space or 'T', take first 10 chars
        try {
            if (s.length() >= 10) {
                String d = s.substring(0, 10);
                return LocalDate.parse(d);
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            // As a last resort, try using java.sql.Date.valueOf if format is yyyy-mm-dd
            try {
                return java.sql.Date.valueOf(s).toLocalDate();
            } catch (Exception ex) {
                // Give up; log minimal info and return null
                System.err.println("[InventoryDAO] Failed to parse date in column '" + column + "': " + s);
                return null;
            }
        }
    }

    // ───────────────────────────────
    // Maintenance: auto-update statuses
    // ───────────────────────────────
    public void refreshStatuses() {
        String sql = "SELECT * FROM inventory";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                InventoryBatch b = extractBatch(rs);

                String newStatus;
                if (b.isExpired()) newStatus = "Expired";
                else if (b.isExpiringSoon()) newStatus = "Expiring Soon";
                else if (b.isLowStock()) newStatus = "Low Stock";
                else newStatus = "Active";

                if (!newStatus.equals(b.getStatus())) {
                    b.setStatus(newStatus);
                    updateBatch(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
