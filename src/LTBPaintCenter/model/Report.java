package LTBPaintCenter.model;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Report {
    private final List<Sale> sales = new ArrayList<>();
    private final Map<Integer, Integer> cumulativeProductSales = new HashMap<>();
    private static final SimpleDateFormat DB_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Persist and record sale
    public void recordSale(Sale s) {
        // First, append to in-memory structures
        sales.add(s);
        for (SaleItem it : s.getItems()) {
            cumulativeProductSales.put(
                    it.getProductId(),
                    cumulativeProductSales.getOrDefault(it.getProductId(), 0) + it.getQty()
            );
        }

        // Then, persist each item into the sales table
        String sql = "INSERT INTO sales (sale_reference, product_id, product_name, quantity, price, total, sale_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SaleItem it : s.getItems()) {
                ps.setString(1, s.getId());
                ps.setInt(2, it.getProductId());
                ps.setString(3, it.getName());
                ps.setInt(4, it.getQty());
                ps.setDouble(5, it.getPrice());
                ps.setDouble(6, it.getSubtotal());
                ps.setString(7, DB_DATETIME.format(s.getDate()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load historical sales from database
    public void loadFromDatabase() {
        sales.clear();
        cumulativeProductSales.clear();

        String sql = "SELECT id, sale_reference, product_id, product_name, quantity, price, total, sale_date " +
                "FROM sales ORDER BY id ASC";
        Map<String, Sale> byRef = new LinkedHashMap<>();
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String ref = rs.getString("sale_reference");
                if (ref == null || ref.isBlank()) {
                    ref = "S" + rs.getInt("id");
                }
                String dateStr = rs.getString("sale_date");
                Date when = parseDbDate(dateStr);
                Sale sale = byRef.computeIfAbsent(ref, k -> new Sale(k, when));

                int productId = rs.getInt("product_id");
                String name = rs.getString("product_name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                SaleItem item = new SaleItem(productId, name, price, qty);
                sale.addItem(item);

                // Track cumulative totals
                cumulativeProductSales.put(productId,
                        cumulativeProductSales.getOrDefault(productId, 0) + qty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sales.addAll(byRef.values());
    }

    private Date parseDbDate(String s) {
        if (s == null || s.isBlank()) return new Date();
        try {
            // Try common formats from SQLite
            if (s.length() >= 19) {
                String trimmed = s.substring(0, 19).replace('T', ' ');
                return DB_DATETIME.parse(trimmed);
            }
            return DB_DATETIME.parse(s);
        } catch (ParseException e) {
            // Fallback: try parsing as ISO date only
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s.substring(0, Math.min(10, s.length())));
            } catch (Exception ignored) {
                return new Date();
            }
        }
    }

    public List<Sale> getSales() {
        return Collections.unmodifiableList(sales);
    }

    public Map<Integer, Integer> getCumulativeProductSales() {
        return Collections.unmodifiableMap(cumulativeProductSales);
    }
}
