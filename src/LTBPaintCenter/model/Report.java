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

/**
 * This class manages sales reports and statistics.
 * It tracks all sales, stores them in the database, and provides
 * cumulative sales data for reporting purposes.
 */
public class Report {
    
    private final List<Sale> sales = new ArrayList<>();
    private final Map<Integer, Integer> cumulativeProductSales = new HashMap<>();
    private static final SimpleDateFormat DB_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Records a new sale and saves it to the database.
     * Also updates the cumulative sales totals.
     * 
     * @param sale The Sale object to record
     */
    public void recordSale(Sale sale) {
        // Add to in-memory list
        sales.add(sale);
        
        // Update cumulative sales totals for each product
        for (SaleItem item : sale.getItems()) {
            int currentTotal = cumulativeProductSales.getOrDefault(item.getProductId(), 0);
            cumulativeProductSales.put(item.getProductId(), currentTotal + item.getQty());
        }

        // Save to database
        String sql = "INSERT INTO sales (sale_reference, product_id, product_name, quantity, price, total, sale_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Insert each item in the sale as a separate row
            for (SaleItem item : sale.getItems()) {
                ps.setString(1, sale.getId());
                ps.setInt(2, item.getProductId());
                ps.setString(3, item.getName());
                ps.setInt(4, item.getQty());
                ps.setDouble(5, item.getPrice());
                ps.setDouble(6, item.getSubtotal());
                ps.setString(7, DB_DATETIME.format(sale.getDate()));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("Error recording sale: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads all historical sales from the database.
     * Groups items by sale reference number and reconstructs Sale objects.
     */
    public void loadFromDatabase() {
        sales.clear();
        cumulativeProductSales.clear();

        String sql = "SELECT id, sale_reference, product_id, product_name, quantity, price, total, sale_date " +
                "FROM sales ORDER BY id ASC";
        
        // Use LinkedHashMap to preserve insertion order
        Map<String, Sale> salesByReference = new LinkedHashMap<>();
        
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            
            while (rs.next()) {
                // Get or create sale reference number
                String reference = rs.getString("sale_reference");
                if (reference == null || reference.isBlank()) {
                    reference = "S" + rs.getInt("id");
                }
                
                // Parse the sale date
                String dateStr = rs.getString("sale_date");
                Date saleDate = parseDbDate(dateStr);
                
                // Get or create the Sale object for this reference
                Sale sale = salesByReference.computeIfAbsent(reference, 
                        k -> new Sale(k, saleDate));

                // Create the sale item
                int productId = rs.getInt("product_id");
                String name = rs.getString("product_name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                SaleItem item = new SaleItem(productId, name, price, qty);
                sale.addItem(item);

                // Track cumulative totals
                int currentTotal = cumulativeProductSales.getOrDefault(productId, 0);
                cumulativeProductSales.put(productId, currentTotal + qty);
            }
        } catch (SQLException e) {
            System.err.println("Error loading sales from database: " + e.getMessage());
            e.printStackTrace();
        }

        // Add all loaded sales to the list
        sales.addAll(salesByReference.values());
    }

    /**
     * Parses a date string from the database.
     * Handles different date formats that might be stored.
     * 
     * @param dateString The date string from the database
     * @return A Date object, or current date if parsing fails
     */
    private Date parseDbDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return new Date();
        }
        
        try {
            // Try parsing as full datetime (yyyy-MM-dd HH:mm:ss)
            if (dateString.length() >= 19) {
                String trimmed = dateString.substring(0, 19).replace('T', ' ');
                return DB_DATETIME.parse(trimmed);
            }
            return DB_DATETIME.parse(dateString);
        } catch (ParseException e) {
            // Fallback: try parsing as date only (yyyy-MM-dd)
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd")
                        .parse(dateString.substring(0, Math.min(10, dateString.length())));
            } catch (Exception ignored) {
                return new Date();
            }
        }
    }

    /**
     * Gets all sales (read-only).
     * 
     * @return An unmodifiable list of all sales
     */
    public List<Sale> getSales() {
        return Collections.unmodifiableList(sales);
    }

    /**
     * Gets cumulative sales totals by product ID.
     * 
     * @return An unmodifiable map of product ID to total quantity sold
     */
    public Map<Integer, Integer> getCumulativeProductSales() {
        return Collections.unmodifiableMap(cumulativeProductSales);
    }
}
